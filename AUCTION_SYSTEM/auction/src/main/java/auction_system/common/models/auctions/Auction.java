package auction_system.common.models.auctions;

import auction_system.common.exceptions.AuctionClosedException;
import auction_system.common.exceptions.InvalidBidException;
import auction_system.common.models.items.Item;
import auction_system.common.models.users.Participant;
import auction_system.common.network.Protocol;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lớp đại diện cho một phiên đấu giá trong hệ thống.
 */
public class Auction extends Entity {
    private static final long ANTI_SNIPING_WINDOW_SECONDS = 30L;
    private static final long ANTI_SNIPING_EXTENSION_SECONDS = 30L;

    private Item item;
    private Participant participant;
    private final List<BidTransaction> bids;
    private BidTransaction currentHighestBid;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private String sellerId;
    private boolean sellerPaid;
    private boolean antiSnipingEnabled;
    // Dùng transient để bỏ qua thuộc tính, không
    // lưu xuống file và không gửi qua mạng.
    private transient List<AuctionObserver> observers;

    /**
     * Khởi tạo một phiên đấu giá mới.
     *
     * @param item      Sản phẩm đấu giá.
     * @param seller    Người bán tổ chức phiên đấu giá.
     * @param startTime Thời gian bắt đầu.
     * @param endTime   Thời gian kết thúc.
     */
    public Auction(Item item,
                   Participant seller,
                   LocalDateTime startTime,
                   LocalDateTime endTime) {
        super();
        this.item = item;
        this.participant = seller;
        this.startTime = startTime;
        this.endTime = endTime;

        this.bids = new ArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
        this.status = AuctionStatus.OPEN;
        this.sellerPaid = false;
        this.antiSnipingEnabled = false;
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

        double newBidAmount = bid.getAmount();

        // 2. Bid đầu tiên được bằng giá khởi điểm; các bid sau phải vượt giá đang dẫn đầu.
        if (currentHighestBid == null) {
            if (newBidAmount < item.getStartPrice()) {
                throw new InvalidBidException(
                        "Giá đặt đầu tiên phải lớn hơn hoặc bằng giá khởi điểm ("
                                + item.getStartPrice() + ")");
            }
        } else if (newBidAmount <= currentHighestBid.getAmount()) {
            throw new InvalidBidException(
                    "Giá đặt phải lớn hơn giá cao nhất hiện tại ("
                            + currentHighestBid.getAmount() + ")");
        }

        // 3. Cập nhật thông tin giá mới nhất
        this.currentHighestBid = bid;
        this.bids.add(bid);
        this.item.setCurrentPrice(newBidAmount);
        extendEndTimeIfNeeded(LocalDateTime.now());

        // 4. Thông báo cho tất cả mọi người đang xem biết có giá mới
        notifyObservers();
    }

    /**
     * Gia hạn phiên nếu bid hợp lệ xuất hiện trong cửa sổ chống đặt giá phút chót.
     *
     * @param now thời điểm server ghi nhận bid hợp lệ
     */
    private void extendEndTimeIfNeeded(final LocalDateTime now) {
        if (!antiSnipingEnabled || now == null || now.isAfter(endTime)) {
            return;
        }

        final LocalDateTime antiSnipingStart =
                endTime.minusSeconds(ANTI_SNIPING_WINDOW_SECONDS);
        if (now.isBefore(antiSnipingStart)) {
            return;
        }

        endTime = endTime.plusSeconds(ANTI_SNIPING_EXTENSION_SECONDS);
        notifyObservers(buildAuctionExtendedMessage());
    }

    /**
     * Tạo thông báo realtime khi phiên được gia hạn.
     *
     * @return thông báo theo protocol socket
     */
    private String buildAuctionExtendedMessage() {
        return Protocol.Response.AUCTION_EXTENDED.name()
                + Protocol.SEPARATOR + getId()
                + Protocol.SEPARATOR + endTime;
    }

    /**
     * Tính toán và xác định người chiến thắng của phiên đấu giá.
     *
     * @return Người thắng cuộc, hoặc null nếu không ai đặt giá.
     */
    public Participant calculateWinner() {
        // Chỉ được tiính người thắng khi phiên đấu giá đã kết thúc
        if (this.status != AuctionStatus.FINISHED) {
            throw new IllegalStateException(
                    "Phiên đấu giá chưa kết thúc, chưa thể tìm được người thắng!");
        }

        if (this.currentHighestBid != null) {
            return currentHighestBid.getParticipant();
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
                ? currentHighestBid.getAmount()
                : item.getStartPrice();
        String bidderName = "";
        String bidTime = "";
        if (currentHighestBid != null && currentHighestBid.getParticipant() != null) {
            bidderName = currentHighestBid.getParticipant().getUsername();
            bidTime = currentHighestBid.getTimestamp().toString();
        }

        String message = Protocol.Response.UPDATE_PRICE.name()
                + Protocol.SEPARATOR + this.getId()
                + Protocol.SEPARATOR + currentPrice
                + Protocol.SEPARATOR + bidderName
                + Protocol.SEPARATOR + bidTime
                + Protocol.SEPARATOR + this.endTime;

        for (AuctionObserver observer : observers) {
            observer.update(message);
        }
    }

    /**
     * Gửi một thông điệp tuỳ chỉnh đến toàn bộ người theo dõi.
     *
     * @param message Nội dung thông báo.
     */
    public void notifyObservers(String message) {
        for (AuctionObserver observer : observers) {
            observer.update(message);
        }
    }

    /**
     * Kích hoạt bắt đầu phiên đấu giá (chuyển trạng thái sang RUNNING).
     */
    public void startAuction() {
        // Chỉ bắt đầu khi đang OPEN và đã tới giờ bắt đầu
        if (this.status == AuctionStatus.OPEN && !LocalDateTime.now().isBefore(startTime)) {
            setStatus(AuctionStatus.RUNNING);
            String message = Protocol.Response.AUCTION_STARTED.name() 
                    + Protocol.SEPARATOR + this.getId();
            notifyObservers(message);
        }
    }

    /**
     * Kết thúc phiên đấu giá (chuyển trạng thái sang FINISHED).
     */
    public void endAuction() {
        // Chỉ kết thúc khi đang RUNNING và đã qua giờ kết thúc
        if (this.status == AuctionStatus.RUNNING && !LocalDateTime.now().isBefore(this.endTime)) {
            setStatus(AuctionStatus.FINISHED);
            Participant winner = calculateWinner();
            String winnerUsername = (winner != null) ? winner.getUsername() : "Không có ai";
            String itemName = (item != null) ? item.getItemName() : "";
            String message = Protocol.Response.AUCTION_ENDED.name()
                    + Protocol.SEPARATOR + this.getId()
                    + Protocol.SEPARATOR + winnerUsername
                    + Protocol.SEPARATOR + itemName;
            notifyObservers(message);
        }
    }

    /**
     * Khôi phục lại danh sách observers sau khi được nhận từ mạng (deserialize)
     * vì thuộc tính này là transient (không được truyền đi) nên ban đầu sẽ bị null.
     *
     * @param in Luồng đối tượng đầu vào
     * @throws IOException Lỗi nhập/xuất trong quá trình đọc luồng
     * @throws ClassNotFoundException Lỗi không tìm thấy lớp của đối tượng
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject(); // Đọc các thuộc tính bình thường khác
        // Khởi tạo lại danh sách rỗng để không bị lỗi NullPointerException
        this.observers = new CopyOnWriteArrayList<>();
    }

    public Participant getParticipant() {
        return participant;
    }

    public void setParticipant(Participant participant) {
        this.participant = participant;
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

    public String getSellerId() {
        return sellerId;
    }

    public boolean isSellerPaid() {
        return sellerPaid;
    }

    public void setSellerPaid(final boolean sellerPaid) {
        this.sellerPaid = sellerPaid;
    }

    public boolean isAntiSnipingEnabled() {
        return antiSnipingEnabled;
    }

    public void setAntiSnipingEnabled(final boolean antiSnipingEnabled) {
        this.antiSnipingEnabled = antiSnipingEnabled;
    }

    @Override
    public String toString() {
        return "Auction{"
                + "id='" + id + '\'' // Sử dụng id từ lớp Entity
                + ", item=" + item
                + ", seller=" + participant
                + ", bids=" + bids
                + ", currentHighestBid=" + currentHighestBid
                + ", startTime=" + startTime
                + ", endTime=" + endTime
                + ", status=" + status
                + ", antiSnipingEnabled=" + antiSnipingEnabled
                + ", observers=" + observers
                + '}';
    }
}
