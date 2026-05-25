package auction_system.server.session;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.users.User;
import auction_system.server.core.AuctionManager;
import java.util.HashSet;
import java.util.Set;

/**
 * Lưu trữ trạng thái và quản lý logic liên quan đến một phiên làm việc của client.
 * Bao gồm thông tin người dùng đã đăng nhập và các phiên đấu giá đang theo dõi.
 */
public class ClientSession {
    /**
     * Người dùng đang đăng nhập trên kết nối này.
     * Null nếu chưa đăng nhập.
     */
    private User currentUser;
    /**
     * ID các phiên đấu giá mà client này đang theo dõi realtime.
     */
    private final Set<String> watchedAuctionIds = new HashSet<>();
    /**
     * Observer (chính là ClientHandler) để đăng ký/hủy đăng ký nhận thông báo.
     */
    private final AuctionObserver observer;
    private final AuctionManager auctionManager;

    public ClientSession(AuctionObserver observer, AuctionManager auctionManager) {
        this.observer = observer;
        this.auctionManager = auctionManager;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Lấy observer gắn với session hiện tại.
     *
     * <p>Trong runtime hiện tại observer chính là ClientHandler đang giữ socket,
     * dùng để đăng ký realtime message theo user sau khi login.
     *
     * @return observer của session
     */
    public AuctionObserver getObserver() {
        return observer;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isLoggedIn() {
        return this.currentUser != null;
    }

    /**
     * Thêm một phiên đấu giá vào danh sách theo dõi của client.
     *
     * @param auctionId ID của phiên đấu giá cần theo dõi.
     */
    public void watchAuction(String auctionId) {
        Auction auction = auctionManager.getAuctionById(auctionId);
        if (auction != null) {
            auction.attach(observer);
            watchedAuctionIds.add(auctionId);
        }
    }

    /**
     * Gỡ bỏ một phiên đấu giá khỏi danh sách theo dõi của client.
     *
     * @param auctionId ID của phiên đấu giá cần gỡ bỏ.
     */
    public void unwatchAuction(String auctionId) {
        Auction auction = auctionManager.getAuctionById(auctionId);
        if (auction != null) {
            auction.detach(observer);
        }
        watchedAuctionIds.remove(auctionId);
    }

    /**
     * Hủy theo dõi tất cả các phiên đấu giá mà client đang tham gia.
     */
    public void unwatchAllAuctions() {
        // Tạo bản sao để tránh ConcurrentModificationException khi duyệt và xóa
        new HashSet<>(watchedAuctionIds).forEach(this::unwatchAuction);
        watchedAuctionIds.clear();
    }
}
