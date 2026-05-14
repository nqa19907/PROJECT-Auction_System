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

    /**
     * Khởi tạo một giao dịch đặt giá mới.
     *
     * @param bidder Người đặt giá.
     * @param amount Số tiền đặt giá.
     */
    public BidTransaction(Bidder bidder, double amount) {
        super(); // Tự động tạo ID
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
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
}
