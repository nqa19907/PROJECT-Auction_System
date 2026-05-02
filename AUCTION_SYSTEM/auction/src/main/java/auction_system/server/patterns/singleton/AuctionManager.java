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
 * Lớp quản lý trung tâm cho các phiên đấu giá và người dùng đang hoạt động.
 * Sử dụng mẫu thiết kế Singleton để đảm bảo chỉ có một instance duy nhất trong toàn hệ thống.
 */
public class AuctionManager {
    private static AuctionManager instance;
    private final List<Auction> auctionList;
    private final Map<String, User> activeUsers;

    private AuctionManager() {
        // Sử dụng list thread-safe để tránh lỗi khi nhiều client cùng tương tác
        this.auctionList = new CopyOnWriteArrayList<>();
        // Dùng ConcurrentHashMap để an toàn trong môi trường đa luồng
        this.activeUsers = new ConcurrentHashMap<>();
    }

    /**
     * Lấy instance duy nhất của hệ thống quản lý đấu giá.
     *
     * @return Instance của AuctionManager.
     */
    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }

        return instance;
    }

    // --- Quản lý phiên đấu giá ---
    /**
     * Tạo một phiên đấu giá mới và thêm vào hệ thống quản lý.
     *
     * @param item      Sản phẩm được đưa ra đấu giá.
     * @param seller    Người bán sản phẩm.
     * @param startTime Thời gian bắt đầu.
     * @param endTime   Thời gian kết thúc.
     */
    public void createAuction(Item item, Seller seller, 
                              LocalDateTime startTime, LocalDateTime endTime) {
        Auction newAuction = new Auction(item, seller, startTime, endTime);
        auctionList.add(newAuction);
        // Có thể thêm logic thông báo cho mọi người về phiên đấu giá
        // to be coded
    }

    /**
     * Tìm kiếm một phiên đấu giá đang quản lý dựa vào ID.
     *
     * @param auctionId ID của phiên đấu giá cần tìm.
     * @return Phiên đấu giá tương ứng, hoặc null nếu không tìm thấy.
     */
    public Auction getAuctionById(String auctionId) {
        return auctionList.stream()
                .filter(auction -> auction.getId().equals(auctionId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Lấy danh sách toàn bộ các phiên đấu giá trong hệ thống.
     *
     * @return Danh sách các phiên đấu giá (chỉ đọc, không thể sửa đổi trực tiếp).
     */
    public List<Auction> getAllAuctions() {
        // Trả về một view không thể sửa đổi của danh sách để bên
        // ngoài không thể thay đổi trực tiếp
        return Collections.unmodifiableList(auctionList);
    }

    // -- Quản lý người dùng --
    /**
     * Đánh dấu và lưu trữ trạng thái một người dùng vừa đăng nhập.
     *
     * @param user Người dùng đã đăng nhập.
     */
    public void userLoggedIn(User user) {
        activeUsers.put(user.getId(), user);
    }
    
    /**
     * Loại bỏ trạng thái đăng nhập của một người dùng.
     *
     * @param user Người dùng đã đăng xuất.
     */
    public void userLoggedOut(User user) {
        activeUsers.remove(user.getId());
    }
}
