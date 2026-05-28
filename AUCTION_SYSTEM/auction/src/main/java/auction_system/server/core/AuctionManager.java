package auction_system.server.core;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.items.Art;
import auction_system.common.models.items.Electronic;
import auction_system.common.models.items.Item;
import auction_system.common.models.items.Vehicle;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade tương thích ngược cho các command/caller cũ.
 *
 * <p>Logic chính đã được tách xuống các registry/service nhỏ trong package này.
 * Khi caller mới được viết hoặc refactor, ưu tiên inject service cụ thể thay vì
 * thêm trách nhiệm mới vào manager.
 */
public class AuctionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionManager.class);
    private static final int SCHEDULER_INTERVAL_SECONDS = 10;

    private static AuctionManager instance;

    private final SerializedDatabase database;
    private final AuctionRegistry auctionRegistry;
    private final UserRegistry userRegistry;
    private final OnlineUserRegistry onlineUsers;
    private final AuctionRealtimeNotifier notifier;
    private final AuctionSettlementService settlementService;
    private final AuctionLifecycleService lifecycleService;
    private final AuctionAdministrationService administrationService;
    private final AntiSnipingService antiSnipingService;
    private final AuctionLifecycleScheduler lifecycleScheduler;

    /**
     * Lấy singleton dùng chung cho runtime server.
     *
     * @param database database serialization của server
     * @return instance dùng chung của manager
     */
    public static synchronized AuctionManager getInstance(final SerializedDatabase database) {
        if (instance == null) {
            instance = new AuctionManager(database);
        }
        return instance;
    }

    private AuctionManager(final SerializedDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
        this.auctionRegistry = new AuctionRegistry(database);
        this.userRegistry = new UserRegistry(database);
        this.onlineUsers = new OnlineUserRegistry();
        this.notifier = new AuctionRealtimeNotifier(database, onlineUsers);
        this.settlementService = new AuctionSettlementService(database, notifier);
        this.lifecycleService = new AuctionLifecycleService(
                database,
                auctionRegistry,
                settlementService,
                notifier);
        this.administrationService = new AuctionAdministrationService(
                auctionRegistry,
                lifecycleService,
                LOGGER);
        this.antiSnipingService = new AntiSnipingService(
                database,
                auctionRegistry,
                lifecycleService,
                notifier);
        this.lifecycleScheduler = new AuctionLifecycleScheduler(
                auctionRegistry,
                lifecycleService,
                LOGGER,
                SCHEDULER_INTERVAL_SECONDS);

        loadPersistentState();
        lifecycleScheduler.start();
        generateTestDataIfNeeded();
    }

    private void loadPersistentState() {
        userRegistry.loadFromDatabase();
        auctionRegistry.loadFromDatabase();

        LOGGER.info("Đã nạp " + userRegistry.size()
                + " user và " + auctionRegistry.getAll().size()
                + " phiên đấu giá từ database.");
    }

    private void generateTestDataIfNeeded() {
        try {
            // Nếu không có dữ liệu cũ, tạo dữ liệu mẫu.
            if (userRegistry.isEmpty() || auctionRegistry.isEmpty()) {
                TestDataGenerator.generate(this);
            }
        } catch (Exception exception) {
            LOGGER.warn("Không thể khởi tạo user mẫu: " + exception.getMessage());
        }
    }

    public void shutdown() {
        lifecycleScheduler.shutdown();
    }

    /**
     * Tạo phiên đấu giá mới.
     *
     * @param item sản phẩm đưa ra đấu giá
     * @param seller người bán
     * @param startTime thời gian bắt đầu
     * @param endTime thời gian kết thúc
     * @return phiên đấu giá vừa tạo
     */
    public Auction createAuction(
            final Item item,
            final Participant seller,
            final LocalDateTime startTime,
            final LocalDateTime endTime) {
        final Auction newAuction = auctionRegistry.createAuction(item, seller, startTime, endTime);

        // Báo cho các client online cập nhật danh sách phiên đấu giá.
        notifier.notifyAuctionCreated(newAuction);

        LOGGER.info("Phiên đấu giá mới: " + newAuction.getId()
                + " | Item: " + item.getItemName());

        return newAuction;
    }

    /**
     * Tìm phiên đấu giá theo mã và refresh lifecycle trước khi trả về.
     *
     * @param auctionId mã phiên đấu giá
     * @return phiên đấu giá nếu tồn tại
     */
    public Auction getAuctionById(final String auctionId) {
        final Auction auction = auctionRegistry.findById(auctionId);

        // Đồng bộ trạng thái thời gian trước khi trả phiên cho caller.
        if (auction != null) {
            lifecycleService.refreshAuctionLifecycle(auction);
        }
        return auction;
    }

    public List<Auction> getAllAuctions() {
        lifecycleService.refreshAllAuctionLifecycles();
        return auctionRegistry.getAll();
    }

    public List<User> getAllUsers() {
        return List.copyOf(database.users().findAll());
    }

    public void refreshAllAuctionLifecycles() {
        lifecycleService.refreshAllAuctionLifecycles();
    }

    public void refreshAuctionLifecycle(final Auction auction) {
        lifecycleService.refreshAuctionLifecycle(auction);
    }

    public void settleFinishedAuction(final Auction auction) {
        settlementService.settleFinishedAuction(auction);
    }

    /**
     * Thông báo realtime rằng danh sách người dùng đã thay đổi.
     *
     * <p>Client nhận được sự kiện này có thể chủ động gọi lại API danh sách user
     * để lấy snapshot mới nhất từ server.
     */
    public void notifyUserListChanged() {
        final String message = Protocol.Response.USER_LIST_CHANGED.name();
        onlineUsers.getObservers().forEach(observer -> observer.update(message));
    }

    /**
     * Huỷ một phiên đấu giá theo ID.
     *
     * @param auctionId ID phiên cần huỷ.
     * @return true nếu huỷ thành công, false nếu không tìm thấy.
     */
    public boolean cancelAuction(final String auctionId) {
        return administrationService.cancelAuction(auctionId);
    }

    public boolean deleteAuction(final String auctionId) {
        return administrationService.deleteAuction(auctionId);
    }

    /**
     * Cap nhat thong tin phien do seller so huu.
     *
     * @param auctionId ma phien can chinh sua
     * @param userId ma user dang thao tac
     * @param category danh muc moi
     * @param itemName ten tai san moi
     * @param description mo ta moi
     * @param condition tinh trang moi
     * @param endTime thoi gian ket thuc moi
     * @return true neu cap nhat thanh cong
     */
    public boolean updateMyAuctionInfo(
            final String auctionId,
            final String userId,
            final String category,
            final String itemName,
            final String description,
            final String condition,
            final LocalDateTime endTime) {
        final Auction auction = auctionRegistry.findById(auctionId);
        if (auction == null) {
            return false;
        }
        if (auction.getParticipant() == null
                || !userId.equals(auction.getParticipant().getId())) {
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa phiên này.");
        }
        if (auction.getCurrentHighestBid() != null) {
            throw new IllegalArgumentException("Phiên đã có người đặt giá, không thể chỉnh sửa.");
        }
        if (auction.getStatus() != AuctionStatus.OPEN) {
            throw new IllegalArgumentException("Chỉ được chỉnh sửa phiên chưa bắt đầu.");
        }

        // Cập nhật các trường cho phép sửa theo yêu cầu.
        // endTime má»›i pháº£i sau startTime Ä‘á»ƒ khÃ´ng phÃ¡ vá»¡ lifecycle.
        if (endTime == null || !endTime.isAfter(auction.getStartTime())) {
            throw new IllegalArgumentException("Thoi gian ket thuc phai sau thoi gian bat dau.");
        }

        final String normalizedCategory = category.trim().toUpperCase();
        final Item currentItem = auction.getItem();
        final String fullDescription = description + "\nTình trạng: " + condition;

        if (normalizedCategory.equals(currentItem.getCategory())) {
            // Cùng danh mục: giữ nguyên class item cũ, chỉ cập nhật nội dung.
            currentItem.setItemName(itemName);
            currentItem.setDescription(fullDescription);
            currentItem.setCategory(normalizedCategory);
        } else {
            // Khác danh mục: phải tạo item mới đúng class để category hoạt động đúng theo model.
            final Item replacementItem = switch (normalizedCategory) {
                case "ART" -> new Art(
                        itemName,
                        fullDescription,
                        currentItem.getStartPrice(),
                        currentItem.getSellerId());
                case "ELECTRONIC" -> new Electronic(
                        itemName,
                        fullDescription,
                        currentItem.getStartPrice(),
                        currentItem.getSellerId());
                case "VEHICLE" -> new Vehicle(
                        itemName,
                        fullDescription,
                        currentItem.getStartPrice(),
                        currentItem.getSellerId());
                default -> throw new IllegalArgumentException("Danh mục không hợp lệ.");
            };
            replacementItem.setCurrentPrice(currentItem.getCurrentPrice());
            auction.setItem(replacementItem);
        }

        // Cáº­p nháº­t má»‘c káº¿t thÃºc má»›i trÆ°á»›c khi lÆ°u.
        auction.setEndTime(endTime);
        database.items().save(auction.getItem());
        database.auctions().save(auction);
        database.flushAll();
        return true;
    }

    public Auction updateAntiSniping(
            final String auctionId,
            final User currentUser,
            final boolean enabled) {
        return antiSnipingService.updateAntiSniping(auctionId, currentUser, enabled);
    }

    /**
     * Đánh dấu user online.
     *
     * @param user user vừa đăng nhập
     */
    public void userLoggedIn(final User user) {
        onlineUsers.userLoggedIn(user);
        LOGGER.debug("Online: " + user.getUsername()
                + " (total: " + onlineUsers.getOnlineCount() + ")");
    }

    /**
     * Đánh dấu user online kèm kênh realtime.
     *
     * @param user user vừa đăng nhập
     * @param observer kênh realtime của kết nối socket
     */
    public void userLoggedIn(final User user, final AuctionObserver observer) {
        onlineUsers.userLoggedIn(user, observer);
        LOGGER.debug("Online: " + user.getUsername()
                + " (total: " + onlineUsers.getOnlineCount() + ")");
        notifyUserListChanged();
    }

    /**
     * Xóa trạng thái online của user.
     *
     * @param user user vừa logout hoặc mất kết nối
     */
    public void userLoggedOut(final User user) {
        onlineUsers.userLoggedOut(user);
        LOGGER.debug("Offline: " + user.getUsername()
                + " (total: " + onlineUsers.getOnlineCount() + ")");
        notifyUserListChanged();
    }

    public void notifyBalanceUpdated(final Participant participant) {
        notifier.notifyBalanceUpdated(participant);
    }

    public void notifyAuctionResult(final Auction auction) {
        notifier.notifyAuctionResult(auction);
    }

    public boolean isAlreadyOnline(final String userId) {
        return onlineUsers.isAlreadyOnline(userId);
    }

    public int getOnlineCount() {
        return onlineUsers.getOnlineCount();
    }

    public Map<String, User> getActiveUsers() {
        return onlineUsers.getActiveUsers();
    }

    /**
     * Đăng ký hoặc nạp user vào registry runtime.
     *
     * @param user user cần đăng ký
     * @return user đã persist hoặc user trùng đã tồn tại
     */
    public User registerUser(final User user) {
        final User persistedUser = userRegistry.registerUser(user);
        LOGGER.info("Đăng ký/nạp user: " + persistedUser.getUsername());

        return persistedUser;
    }

    public boolean isUsernameTaken(final String username) {
        return userRegistry.isUsernameTaken(username);
    }

    public User findUserByCredentials(final String email, final String password) {
        return userRegistry.findUserByCredentials(email, password);
    }

    public User findUserById(final String userId) {
        return userRegistry.findUserById(userId);
    }

    /**
     * Xóa user khỏi runtime registry và database.
     *
     * @param userId mã user cần xóa
     * @return true nếu xóa thành công
     */
    public boolean deleteUser(final String userId) {
        User target = findUserById(userId);
        if (target == null) {
            return false;
        }

        // Dọn trạng thái runtime trước khi xóa bản ghi user khỏi database.
        onlineUsers.remove(target);
        userRegistry.remove(target);

        final boolean deleted = database.users().deleteById(userId);
        if (deleted) {
            notifyUserListChanged();
        }
        return deleted;
    }

    public List<User> getAllBidders() {
        return userRegistry.getAllParticipants();
    }

    public List<User> getAllSellers() {
        return userRegistry.getAllParticipants();
    }
}
