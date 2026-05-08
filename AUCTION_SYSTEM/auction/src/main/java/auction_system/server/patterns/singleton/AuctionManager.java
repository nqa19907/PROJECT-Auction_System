package auction_system.server.patterns.singleton;

import auction_system.common.models.Auction;
import auction_system.common.models.Item;
import auction_system.common.models.Seller;
import auction_system.common.models.User;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lớp quản lý trung tâm cho các phiên đấu giá và người dùng.
 * Sử dụng mẫu thiết kế Singleton để đảm bảo chỉ có một instance duy nhất.
 * <p>
 * Được mở rộng thêm:
 * - Quản lý registry toàn bộ người dùng đã đăng ký (allUsers)
 * - Tra cứu người dùng theo credentials (cho login)
 * - Kiểm tra username đã tồn tại (cho register)
 * - Kiểm tra trạng thái online để tránh đăng nhập trùng
 */
public class AuctionManager {

    private static AuctionManager instance;

    // Danh sách phiên đấu giá (thread-safe)
    private final List<Auction> auctionList;

    // Map người dùng đang ONLINE: userId -> User
    private final Map<String, User> activeUsers;

    // Registry toàn bộ người dùng đã đăng ký: username -> User
    // (trong thực tế nên lưu vào DB, hiện tại dùng in-memory)
    private final Map<String, User> userRegistry;

    private AuctionManager() {
        this.auctionList = new CopyOnWriteArrayList<>();
        this.activeUsers = new ConcurrentHashMap<>();
        this.userRegistry = new ConcurrentHashMap<>();
    }

    /**
     * Lấy instance duy nhất.
     */
    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    // =========================================================================
    // Quản lý phiên đấu giá
    // =========================================================================

    /**
     * Tạo một phiên đấu giá mới.
     *
     * @param item      Sản phẩm được đưa ra đấu giá.
     * @param seller    Người bán.
     * @param startTime Thời gian bắt đầu.
     * @param endTime   Thời gian kết thúc.
     * @return Phiên đấu giá vừa tạo.
     */
    public Auction createAuction(Item item, Seller seller,
                                 LocalDateTime startTime, LocalDateTime endTime) {
        Auction newAuction = new Auction(item, seller, startTime, endTime);
        auctionList.add(newAuction);
        return newAuction;
    }

    /**
     * Tìm phiên đấu giá theo ID.
     *
     * @param auctionId ID cần tìm.
     * @return Phiên đấu giá, hoặc null nếu không tìm thấy.
     */
    public Auction getAuctionById(String auctionId) {
        return auctionList.stream()
                .filter(auction -> auction.getId().equals(auctionId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Lấy danh sách tất cả phiên đấu giá (chỉ đọc).
     */
    public List<Auction> getAllAuctions() {
        return Collections.unmodifiableList(auctionList);
    }

    // =========================================================================
    // Quản lý trạng thái online
    // =========================================================================

    /**
     * Đánh dấu người dùng đang online.
     *
     * @param user Người dùng vừa đăng nhập.
     */
    public void userLoggedIn(User user) {
        activeUsers.put(user.getId(), user);
    }

    /**
     * Xoá trạng thái online của người dùng.
     *
     * @param user Người dùng vừa đăng xuất hoặc mất kết nối.
     */
    public void userLoggedOut(User user) {
        activeUsers.remove(user.getId());
    }

    /**
     * Kiểm tra người dùng có đang online không.
     *
     * @param userId ID cần kiểm tra.
     * @return true nếu đang online.
     */
    public boolean isAlreadyOnline(String userId) {
        return activeUsers.containsKey(userId);
    }

    /**
     * Lấy danh sách người dùng đang online.
     */
    public Map<String, User> getActiveUsers() {
        return Collections.unmodifiableMap(activeUsers);
    }

    // =========================================================================
    // Quản lý registry người dùng (đăng ký / đăng nhập)
    // =========================================================================

    /**
     * Đăng ký người dùng mới vào hệ thống.
     * Trong thực tế, method này nên lưu xuống database thay vì in-memory.
     *
     * @param user Người dùng mới.
     */
    public void registerUser(User user) {
        userRegistry.put(user.getUsername(), user);
    }

    /**
     * Kiểm tra username đã có trong hệ thống chưa.
     *
     * @param username Tên đăng nhập cần kiểm tra.
     * @return true nếu đã tồn tại.
     */
    public boolean isUsernameTaken(String username) {
        return userRegistry.containsKey(username);
    }

    /**
     * Tìm người dùng theo username và password (dùng cho xác thực đăng nhập).
     * Lưu ý: trong thực tế, password phải được hash (bcrypt/SHA-256) trước khi so sánh.
     *
     * @param username Tên đăng nhập.
     * @param password Mật khẩu (plaintext — cần hash trong production).
     * @return Đối tượng User nếu khớp, null nếu không tìm thấy.
     */
    public User findUserByCredentials(String username, String password) {
        User user = userRegistry.get(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    /**
     * Tìm người dùng theo ID.
     *
     * @param userId ID cần tìm.
     * @return Đối tượng User, hoặc null nếu không tìm thấy.
     */
    public User findUserById(String userId) {
        return userRegistry.values().stream()
                .filter(u -> u.getId().equals(userId))
                .findFirst()
                .orElse(null);
    }
}
