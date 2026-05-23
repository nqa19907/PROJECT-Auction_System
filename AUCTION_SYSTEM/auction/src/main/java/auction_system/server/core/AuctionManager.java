package auction_system.server.core;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.items.Item;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quản lý trung tâm cho các phiên đấu giá và người dùng — Singleton.
 *
 * <p>Trách nhiệm:
 * <ul>
 * <li>Tạo và tra cứu phiên đấu giá ({@link Auction}).</li>
 * <li>Theo dõi trạng thái online của người dùng.</li>
 * <li>Lưu trữ registry người dùng đã đăng ký (in-memory).</li>
 * <li>Scheduler tự động chuyển trạng thái phiên theo thời gian.</li>
 * </ul>
 */
public class AuctionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionManager.class);
    private static final int SCHEDULER_INTERVAL_SECONDS = 10;

    /** Database serialization của server. */
    private final SerializedDatabase database;

    // =========================================================================
    // Singleton
    // =========================================================================

    private static AuctionManager instance;

    /**
     * Lấy instance duy nhất của AuctionManager.
     *
     * @param database database dùng để đọc ghi dữ liệu
     * @return Instance duy nhất.
     */
    public static synchronized AuctionManager getInstance(final SerializedDatabase database) {
        if (instance == null) {
            instance = new AuctionManager(database);
        }
        return instance;
    }

    // =========================================================================
    // State
    // =========================================================================

    /** Danh sách phiên đấu giá (thread-safe). */
    private final List<Auction> auctionList;

    /** Map người dùng đang ONLINE: userId → User. */
    private final Map<String, User> activeUsers;

    /**
     * Registry toàn bộ người dùng đã đăng ký: username → User.
     * Trong thực tế nên lưu xuống database.
     */
    private final Map<String, User> userRegistry;

    /** Scheduler kiểm tra thời gian phiên đấu giá định kỳ. */
    private final ScheduledExecutorService scheduler;

    /**
     * Khởi tạo manager với database.
     *
     * @param database database dùng để đọc ghi dữ liệu
     */
    private AuctionManager(final SerializedDatabase database) {
        this.auctionList = new CopyOnWriteArrayList<>();
        this.activeUsers = new ConcurrentHashMap<>();
        this.userRegistry = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.database = Objects.requireNonNull(database, "database");
        // Nạp trạng thái từ lần chạy trước (nếu có)
        loadPersistentState();
        startAuctionScheduler();

        try {
            // Nếu không có dữ liệu cũ, tạo dữ liệu mẫu
            if (userRegistry.isEmpty() || auctionList.isEmpty()) {
                TestDataGenerator.generate(this);
            }
        } catch (Exception exception) {
            LOGGER.warn("Không thể khởi tạo user mẫu: " + exception.getMessage());
        }
    }

    /**
     * Nạp dữ liệu đã lưu từ database serialization vào trạng thái runtime.
     * Việc này giúp server "nhớ" lại các user và auction từ lần chạy trước.
     */
    private void loadPersistentState() {
        database.users().findAll().forEach(user -> userRegistry.put(user.getUsername(), user));
        auctionList.addAll(database.auctions().findAll());

        LOGGER.info("Đã nạp " + userRegistry.size()
                + " user và " + auctionList.size()
                + " phiên đấu giá từ database.");
    }

    // =========================================================================
    // Scheduler
    // =========================================================================

    /**
     * Khởi động scheduler kiểm tra trạng thái phiên mỗi
     * {@value #SCHEDULER_INTERVAL_SECONDS} giây.
     * Tự động gọi {@code startAuction()} và {@code endAuction()} theo thời gian.
     */
    private void startAuctionScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            for (final Auction auction : auctionList) {
                try {
                    final AuctionStatus status = auction.getStatus();
                    if (status == AuctionStatus.OPEN) {
                        auction.startAuction();
                    } else if (status == AuctionStatus.RUNNING) {
                        auction.endAuction();
                    }
                } catch (Exception e) {
                    LOGGER.warn("Lỗi scheduler phiên " + auction.getId()
                            + ": " + e.getMessage());
                }
            }
        }, 0, SCHEDULER_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Dừng scheduler khi server tắt.
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    // =========================================================================
    // Quản lý phiên đấu giá
    // =========================================================================

    /**
     * Tạo một phiên đấu giá mới và thêm vào hệ thống.
     *
     * @param item      Sản phẩm đưa ra đấu giá.
     * @param seller    Người bán.
     * @param startTime Thời gian bắt đầu.
     * @param endTime   Thời gian kết thúc.
     * @return Phiên đấu giá vừa tạo.
     */
    public Auction createAuction(
            final Item item,
            final Participant seller,
            final LocalDateTime startTime,
            final LocalDateTime endTime) {
        final Auction newAuction = new Auction(item, seller, startTime, endTime);
        auctionList.add(newAuction);
        database.items().save(item);
        database.auctions().save(newAuction);

        LOGGER.info("Phiên đấu giá mới: " + newAuction.getId()
                + " | Item: " + item.getItemName());

        return newAuction;
    }

    /**
     * Tìm phiên đấu giá theo ID.
     *
     * @param auctionId ID cần tìm.
     * @return Phiên đấu giá, hoặc null nếu không tìm thấy.
     */
    public Auction getAuctionById(final String auctionId) {
        return auctionList.stream()
                .filter(a -> a.getId().equals(auctionId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Lấy danh sách tất cả phiên đấu giá (chỉ đọc).
     *
     * @return Unmodifiable list các phiên đấu giá.
     */
    public List<Auction> getAllAuctions() {
        return Collections.unmodifiableList(auctionList);
    }

    /**
     * Huỷ một phiên đấu giá theo ID.
     *
     * @param auctionId ID phiên cần huỷ.
     * @return true nếu huỷ thành công, false nếu không tìm thấy.
     */
    public boolean cancelAuction(final String auctionId) {
        final Auction auction = getAuctionById(auctionId);
        if (auction == null) {
            return false;
        }
        auction.setStatus(AuctionStatus.CANCELED);
        auction.notifyObservers("AUCTION_ENDED|" + auctionId + "|NONE");
        LOGGER.info("Huỷ phiên đấu giá: " + auctionId);
        return true;
    }

    /**
     * Xóa hẳn một phiên đấu giá theo ID.
     *
     * @param auctionId ID phiên cần xóa.
     * @return true nếu xóa thành công, false nếu không tìm thấy.
     */
    public boolean deleteAuction(final String auctionId) {
        final Auction auction = getAuctionById(auctionId);
        if (auction == null) {
            return false;
        }

        auctionList.remove(auction);
        database.auctions().deleteById(auctionId);
        LOGGER.info("Xóa phiên đấu giá: " + auctionId);
        return true;
    }

    // =========================================================================
    // Quản lý trạng thái online
    // =========================================================================

    /**
     * Đánh dấu người dùng đang online.
     *
     * @param user Người dùng vừa đăng nhập.
     */
    public void userLoggedIn(final User user) {
        activeUsers.put(user.getId(), user);
        LOGGER.debug("Online: " + user.getUsername() + " (total: " + activeUsers.size() + ")");
    }

    /**
     * Xoá trạng thái online của người dùng.
     *
     * @param user Người dùng vừa đăng xuất hoặc mất kết nối.
     */
    public void userLoggedOut(final User user) {
        activeUsers.remove(user.getId());
        LOGGER.debug("Offline: " + user.getUsername() + " (total: " + activeUsers.size() + ")");
    }

    /**
     * Kiểm tra người dùng có đang online không.
     *
     * @param userId ID cần kiểm tra.
     * @return true nếu đang online.
     */
    public boolean isAlreadyOnline(final String userId) {
        return activeUsers.containsKey(userId);
    }

    /**
     * Lấy số lượng người dùng đang online.
     *
     * @return Số người online.
     */
    public int getOnlineCount() {
        return activeUsers.size();
    }

    /**
     * Lấy danh sách người dùng đang online (chỉ đọc).
     *
     * @return Unmodifiable map userId → User.
     */
    public Map<String, User> getActiveUsers() {
        return Collections.unmodifiableMap(activeUsers);
    }

    // =========================================================================
    // Quản lý registry người dùng
    // =========================================================================

    /**
     * Đăng ký người dùng mới vào hệ thống.
     * Nếu user đã tồn tại trong DB (dựa trên email/username), trả về user đó.
     * Nếu chưa, lưu user mới vào DB và trả về.
     *
     * @param user Người dùng mới (username phải chưa tồn tại).
     * @return người dùng đã được lưu hoặc người dùng đã tồn tại trong database
     */
    public User registerUser(final User user) {
        Objects.requireNonNull(user, "user");

        // Thử tìm user bằng email, nếu không thấy thì thử bằng username.
        // Nếu cả hai đều không tồn tại, lưu user mới vào DB.
        User persistedUser = database.users()
                .findByEmail(user.getEmail())
                .or(() -> database.users().findByUsername(user.getUsername()))
                .orElseGet(() -> database.users().save(user));

        // Cập nhật registry in-memory
        userRegistry.put(persistedUser.getUsername(), persistedUser);
        LOGGER.info("Đăng ký/nạp user: " + persistedUser.getUsername());

        return persistedUser;
    }

    /**
     * Kiểm tra username đã tồn tại chưa.
     *
     * @param username Tên cần kiểm tra.
     * @return true nếu đã tồn tại.
     */
    public boolean isUsernameTaken(final String username) {
        return userRegistry.containsKey(username);
    }

    /**
     * Xác thực đăng nhập theo username và password.
     *
     * <p><b>Lưu ý:</b> trong thực tế password phải được hash
     * (bcrypt/SHA-256) trước khi so sánh.
     *
     * @param email    Địa chỉ email đăng nhập.
     * @param password Mật khẩu plaintext.
     * @return User nếu khớp, null nếu sai thông tin.
     */
    public User findUserByCredentials(final String email, final String password) {
        if (email == null || password == null) {
            return null;
        }

        return userRegistry.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email) 
                        && u.getPassword().equals(password))
                .findFirst()
                .orElse(null);
    }

    /**
     * Tìm người dùng theo ID.
     *
     * @param userId ID cần tìm.
     * @return User tương ứng, hoặc null.
     */
    public User findUserById(final String userId) {
        return userRegistry.values().stream()
                .filter(u -> u.getId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Xóa người dùng theo ID.
     *
     * <p>Chỉ xóa ở tầng dữ liệu người dùng:
     * - registry trong bộ nhớ
     * - trạng thái online
     * - repository users trong database
     *
     * @param userId ID người dùng cần xóa
     * @return true nếu xóa thành công, false nếu không tìm thấy
     */
    public boolean deleteUser(final String userId) {
        User target = findUserById(userId);
        if (target == null) {
            return false;
        }

        activeUsers.remove(target.getId());
        userRegistry.remove(target.getUsername());

        return database.users().deleteById(userId);
    }

    /**
     * Lấy danh sách tất cả Bidder đã đăng ký.
     *
     * @return List Bidder.
     */
    public List<User> getAllBidders() {
        return userRegistry.values().stream()
                .filter(u -> u instanceof Participant)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách tất cả Seller đã đăng ký.
     *
     * @return List Seller.
     */
    public List<User> getAllSellers() {
        return userRegistry.values().stream()
                .filter(u -> u instanceof Participant)
                .collect(Collectors.toList());
    }
}
