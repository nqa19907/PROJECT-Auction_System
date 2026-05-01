package auction_system.common.models;
import java.time.LocalDateTime;
public class BidTransaction extends Entity {
    private final Bidder bidder;
    private final double amount;
    private final LocalDateTime timestamp;

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
