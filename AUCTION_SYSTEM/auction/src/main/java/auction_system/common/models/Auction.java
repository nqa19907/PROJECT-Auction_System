package auction_system.common.models;

import auction_system.common.enums.AuctionStatus;
import auction_system.common.patterns.observer.AuctionObserver;
import auction_system.common.exceptions.AuctionClosedException;
import auction_system.common.exceptions.InvalidBidException;
import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lớp đại diện cho một phiên đấu giá trong hệ thống.
 */
public class Auction extends Entity {
    private Item item;
    private Seller seller;
    private final List<BidTransaction> bids;
    private BidTransaction currentHighestBid;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private final List<AuctionObserver> observers;

    /**
     * Khởi tạo một phiên đấu giá mới.
     *
     * @param item      Sản phẩm đấu giá.
     * @param seller    Người bán tổ chức phiên đấu giá.
     * @param startTime Thời gian bắt đầu.
     * @param endTime   Thời gian kết thúc.
     */
    public Auction(Item item, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        super();
        this.item = item;
        this.seller = seller;
        this.startTime = startTime;
        this.endTime = endTime;

        this.bids = new ArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
        this.status = AuctionStatus.OPEN;
    }

    /**
     * Ghi nhận một lượt đặt giá mới cho phiên đấu giá.
     *
     * @param bid Giao dịch đặt giá của người dùng.
     */
    public synchronized void placeBid(BidTransaction bid) {
        // 1. Kiểm tra trạng thái phiên đấu giá
        if (this.status != AuctionStatus.RUNNING) {
            throw new AuctionClosedException(
                    "Phiên đấu giá này không ở trạng thái mở hoặc đã đóng!");
        }

        double newAmount = bid.getAmount();
        double currentHighest = (currentHighestBid != null) 
                ? currentHighestBid.getAmount() : item.getStartPrice();

        // 2. Giá đặt phải cao hơn giá cao nhất hiện tại (hoặc giá khởi điểm)
        if (newAmount <= currentHighest) {
            throw new InvalidBidException(
                    "Giá đặt phải lớn hơn giá cao nhất hiện tại (" + currentHighest + ")");
        }

        // 3. Cập nhật thông tin giá mới nhất
        this.currentHighestBid = bid;
        this.bids.add(bid);
        this.item.setCurrentPrice(newAmount);

        // 4. Thông báo cho tất cả mọi người đang xem biết có giá mới
        notifyObservers();
    }

    /**
     * Tính toán và xác định người chiến thắng của phiên đấu giá.
     *
     * @return Người thắng cuộc, hoặc null nếu không ai đặt giá.
     */
    public Bidder calculateWinner() {
        // Chỉ được tiính người thắng khi phiên đấu giá đã kết thúc
        if (this.status != AuctionStatus.FINISHED) {
            throw new IllegalStateException(
                    "Phiên đấu giá chưa kết thúc, chưa thể tìm được người thắng!");
        }

        if (this.currentHighestBid != null) {
            return currentHighestBid.getBidder();
        }

        // Trường hợp không ai đặt giá
        return null;
    }

    /**
     * Đăng ký một người theo dõi (observer) vào phiên đấu giá.
     *
     * @param observer Người theo dõi mới.
     */
    public void attach(AuctionObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Gỡ bỏ đăng ký một người theo dõi khỏi phiên đấu giá.
     *
     * @param observer Người theo dõi cần gỡ.
     */
    public void detach(AuctionObserver observer) {
        observers.remove(observer);
    }

    /**
     * Gửi thông báo tự động (cập nhật giá mới) đến toàn bộ người theo dõi.
     */
    public void notifyObservers() {
        // Đóng gói thông báo thành một chuỗi chuẩn để đẩy qua Socket
        // (UPDATE_PRICE|giá_mới)
        double currentPrice = (currentHighestBid != null) 
                ? currentHighestBid.getAmount() : item.getStartPrice();
        String message = "UPDATE_PRICE|" + this.getId() + "|" + currentPrice;

        for (AuctionObserver observer :  observers) {
            observer.update(message);
        }
    }

    /**
     * Gửi một thông điệp tuỳ chỉnh đến toàn bộ người theo dõi.
     *
     * @param message Nội dung thông báo.
     */
    public void notifyObservers(String message) {
        for (AuctionObserver observer :  observers) {
            observer.update(message);
        }
    }

    /**
     * Kích hoạt bắt đầu phiên đấu giá (chuyển trạng thái sang RUNNING).
     */
    public void startAuction() {
        // Chỉ bắt đầu khi đang OPEN và đã tới giờ bắt đầu
        if (this.status == AuctionStatus.OPEN && LocalDateTime.now().isAfter(startTime)) {
            setStatus(AuctionStatus.RUNNING);
            String message = "AUCTION_STARTED|" + this.getId();
            notifyObservers(message);
        }
    }

    /**
     * Kết thúc phiên đấu giá (chuyển trạng thái sang FINISHED).
     */
    public void endAuction() {
        // Chỉ kết thúc khi đang RUNNING và đã qua giờ kết thúc
        if (this.status == AuctionStatus.RUNNING && LocalDateTime.now().isAfter(this.endTime)) {
            setStatus(AuctionStatus.FINISHED);
            Bidder winner = calculateWinner();
            String winnerUsername = (winner != null) ? winner.getUsername() : "Không có ai";
            String message = "AUCTION_ENDED|" + this.getId() + "|" + winnerUsername;
            notifyObservers(message);
        }
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public BidTransaction getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(BidTransaction currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Auction{"
                + "id='" + id + '\'' // Sử dụng id từ lớp Entity
                + ", item=" + item
                + ", seller=" + seller
                + ", bids=" + bids
                + ", currentHighestBid=" + currentHighestBid
                + ", startTime=" + startTime
                + ", endTime=" + endTime
                + ", status=" + status
                + ", observers=" + observers
                + '}';
    }
}