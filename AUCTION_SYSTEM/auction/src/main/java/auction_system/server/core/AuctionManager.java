package auction_system.server.core;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.items.Item;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.server.network.ClientHandler;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade runtime trung tâm của server.
 *
 * <p>Class này giữ API cũ cho command/session đang dùng. Các trách nhiệm cụ
 * thể được ủy quyền sang registry/scheduler riêng để manager không còn ôm cả
 * persistence, online state, socket routing và lifecycle scheduling.
 */
public class AuctionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionManager.class);

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
    // Collaborators
    // =========================================================================

    private final AuctionRegistry auctionRegistry;
    private final UserRegistry userRegistry;
    private final OnlineUserRegistry onlineUserRegistry;
    private final ClientConnectionRegistry clientConnectionRegistry;
    private final AuctionLifecycleScheduler lifecycleScheduler;

    /**
     * Khởi tạo manager với database.
     *
     * @param database database dùng để đọc ghi dữ liệu
     */
    private AuctionManager(final SerializedDatabase database) {
        final SerializedDatabase checkedDatabase = Objects.requireNonNull(database, "database");

        this.auctionRegistry = new AuctionRegistry(checkedDatabase);
        this.userRegistry = new UserRegistry(checkedDatabase);
        this.onlineUserRegistry = new OnlineUserRegistry();
        this.clientConnectionRegistry = new ClientConnectionRegistry();
        this.lifecycleScheduler = new AuctionLifecycleScheduler(auctionRegistry);

        loadPersistentState();
        lifecycleScheduler.start();

        try {
            // Nếu không có dữ liệu cũ, tạo dữ liệu mẫu
            if (userRegistry.isEmpty() || auctionRegistry.isEmpty()) {
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
        userRegistry.loadFromPersistence();
        auctionRegistry.loadFromPersistence();

        LOGGER.info(
                "Đã nạp {} user và {} phiên đấu giá từ database.",
                userRegistry.count(),
                auctionRegistry.findAll().size());
    }

    /**
     * Dừng scheduler khi server tắt.
     */
    public void shutdown() {
        lifecycleScheduler.shutdown();
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
        return auctionRegistry.createAuction(item, seller, startTime, endTime);
    }

    /**
     * Tìm phiên đấu giá theo ID.
     *
     * @param auctionId ID cần tìm.
     * @return Phiên đấu giá, hoặc null nếu không tìm thấy.
     */
    public Auction getAuctionById(final String auctionId) {
        return auctionRegistry.findById(auctionId);
    }

    /**
     * Lấy danh sách tất cả phiên đấu giá (chỉ đọc).
     *
     * @return Unmodifiable list các phiên đấu giá.
     */
    public List<Auction> getAllAuctions() {
        return auctionRegistry.findAll();
    }

    /**
     * Cập nhật trạng thái tất cả phiên theo thời gian hiện tại và lưu xuống database.
     */
    public void refreshAllAuctionLifecycles() {
        auctionRegistry.refreshAllAuctionLifecycles();
    }

    /**
     * Cập nhật trạng thái một phiên theo thời gian hiện tại và lưu nếu có thay đổi.
     *
     * @param auction phiên đấu giá cần cập nhật
     */
    public void refreshAuctionLifecycle(final Auction auction) {
        auctionRegistry.refreshAuctionLifecycle(auction);
    }

    /**
     * Huỷ một phiên đấu giá theo ID.
     *
     * @param auctionId ID phiên cần huỷ.
     * @return true nếu huỷ thành công, false nếu không tìm thấy.
     */
    public boolean cancelAuction(final String auctionId) {
        return auctionRegistry.cancelById(auctionId);
    }

    /**
     * Xóa hẳn một phiên đấu giá theo ID.
     *
     * @param auctionId ID phiên cần xóa.
     * @return true nếu xóa thành công, false nếu không tìm thấy.
     */
    public boolean deleteAuction(final String auctionId) {
        return auctionRegistry.deleteById(auctionId);
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
        onlineUserRegistry.markOnline(user);
    }

    /**
     * Xoá trạng thái online của người dùng.
     *
     * @param user Người dùng vừa đăng xuất hoặc mất kết nối.
     */
    public void userLoggedOut(final User user) {
        onlineUserRegistry.markOffline(user);
    }

    /**
     * Gắn socket handler hiện tại với user vừa đăng nhập.
     *
     * <p>Registry này dùng cho các realtime message theo user, ví dụ cập nhật ví,
     * không phụ thuộc vào user đang xem màn hình nào.
     *
     * @param userId id của user đã đăng nhập
     * @param handler handler đang giữ socket của user
     */
    public void registerClientHandler(
            final String userId,
            final ClientHandler handler) {
        clientConnectionRegistry.register(userId, handler);
    }

    /**
     * Gỡ socket handler của user khi logout hoặc mất kết nối.
     *
     * <p>So sánh cả handler để tránh gỡ nhầm nếu sau này có reconnect hoặc thay
     * đổi chính sách đăng nhập nhiều thiết bị.
     *
     * @param userId id của user cần gỡ handler
     * @param handler handler đang bị đóng
     */
    public void unregisterClientHandler(
            final String userId,
            final ClientHandler handler) {
        clientConnectionRegistry.unregister(userId, handler);
    }

    /**
     * Gửi message realtime trực tiếp tới user đang online.
     *
     * <p>Nếu user offline thì không gửi realtime. Số dư vẫn đã được lưu trong
     * database, lần đăng nhập sau user sẽ nhận số dư mới từ LOGIN_OK.
     *
     * @param userId id user cần nhận message
     * @param message message cần gửi xuống client
     */
    public void notifyUser(final String userId, final String message) {
        clientConnectionRegistry.notifyUser(userId, message);
    }

    /**
     * Kiểm tra người dùng có đang online không.
     *
     * @param userId ID cần kiểm tra.
     * @return true nếu đang online.
     */
    public boolean isAlreadyOnline(final String userId) {
        return onlineUserRegistry.isOnline(userId);
    }

    /**
     * Lấy số lượng người dùng đang online.
     *
     * @return Số người online.
     */
    public int getOnlineCount() {
        return onlineUserRegistry.count();
    }

    /**
     * Lấy danh sách người dùng đang online (chỉ đọc).
     *
     * @return Unmodifiable map userId → User.
     */
    public Map<String, User> getActiveUsers() {
        return onlineUserRegistry.findAll();
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
        return userRegistry.register(user);
    }

    /**
     * Kiểm tra username đã tồn tại chưa.
     *
     * @param username Tên cần kiểm tra.
     * @return true nếu đã tồn tại.
     */
    public boolean isUsernameTaken(final String username) {
        return userRegistry.containsUsername(username);
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
        return userRegistry.findByCredentials(email, password);
    }

    /**
     * Tìm người dùng theo ID.
     *
     * @param userId ID cần tìm.
     * @return User tương ứng, hoặc null.
     */
    public User findUserById(final String userId) {
        return userRegistry.findById(userId);
    }

    /**
     * Lấy toàn bộ user đã nạp trong runtime registry.
     *
     * <p>API này phục vụ các command quản trị read-only. Trạng thái online vẫn
     * nên đọc qua {@link #isAlreadyOnline(String)} vì đó là runtime state của
     * socket, không phải dữ liệu persistence.
     *
     * @return danh sách user chỉ đọc
     */
    public List<User> getAllUsers() {
        return userRegistry.findAll();
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
        User target = userRegistry.findById(userId);
        if (target == null) {
            return false;
        }

        onlineUserRegistry.remove(target.getId());
        userRegistry.remove(target);

        return userRegistry.deleteById(userId);
    }

    /**
     * Lấy danh sách tất cả Bidder đã đăng ký.
     *
     * @return List Bidder.
     */
    public List<User> getAllBidders() {
        return userRegistry.findParticipants();
    }

    /**
     * Lấy danh sách tất cả Seller đã đăng ký.
     *
     * @return List Seller.
     */
    public List<User> getAllSellers() {
        return userRegistry.findParticipants();
    }
}
