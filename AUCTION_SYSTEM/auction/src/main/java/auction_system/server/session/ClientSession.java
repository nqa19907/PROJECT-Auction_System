package auction_system.server.session;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.users.User;
import auction_system.common.patterns.observer.AuctionObserver;
import auction_system.server.patterns.singleton.AuctionManager;
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
     * ID các phiên đấu giá mà client này đang theo dõi (đã JOIN).
     */
    private final Set<String> joinedAuctionIds = new HashSet<>();
    /**
     * Observer (chính là ClientHandler) để đăng ký/hủy đăng ký nhận thông báo.
     */
    private final AuctionObserver observer;

    public ClientSession(AuctionObserver observer) {
        this.observer = observer;
    }

    public User getCurrentUser() {
        return currentUser;
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
    public void joinAuction(String auctionId) {
        Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);
        if (auction != null) {
            auction.attach(observer);
            joinedAuctionIds.add(auctionId);
        }
    }

    /**
     * Gỡ bỏ một phiên đấu giá khỏi danh sách theo dõi của client.
     *
     * @param auctionId ID của phiên đấu giá cần gỡ bỏ.
     */
    public void leaveAuction(String auctionId) {
        Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);
        if (auction != null) {
            auction.detach(observer);
        }
        joinedAuctionIds.remove(auctionId);
    }

    /**
     * Hủy theo dõi tất cả các phiên đấu giá mà client đang tham gia.
     */
    public void leaveAllAuctions() {
        // Tạo bản sao để tránh ConcurrentModificationException khi duyệt và xóa
        new HashSet<>(joinedAuctionIds).forEach(this::leaveAuction);
        joinedAuctionIds.clear();
    }
}