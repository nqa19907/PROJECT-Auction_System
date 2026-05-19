package auction_system.common.models.auctions;

import auction_system.common.models.users.Bidder;
import java.time.LocalDateTime;

/**
 * Lớp đại diện cho một giao dịch đặt giá trong phiên đấu giá.
 */
public class BidTransaction extends Entity {
    private final Bidder bidder;
    private final double amount;
    private final LocalDateTime timestamp;
    private final String auctionId;

    /**
     * Khởi tạo một giao dịch đặt giá mới.
     *
     * @param bidder Người đặt giá.
     * @param amount Số tiền đặt giá.
     * @param auctionId Id của phiên nó đang ở.
     */
    public BidTransaction(Bidder bidder, double amount, Auction auction) {
        super(); // Tự động tạo ID
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.auctionId = auction.getId();
    }

    public Bidder getBidder() {
        return bidder;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public String getAuctionId() {
        return auctionId;
    }
}
