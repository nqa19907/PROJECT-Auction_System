package auction_system.common.models.auctions;

import auction_system.common.models.users.Participant;
import java.time.LocalDateTime;

/**
 * Lớp đại diện cho một giao dịch đặt giá trong phiên đấu giá.
 */
public class BidTransaction extends Entity {
    private final Participant participant;
    private final double amount;
    private final LocalDateTime timestamp;
    private final String auctionId;

    /**
     * Khởi tạo một giao dịch đặt giá mới.
     *
     * @param participant Người đặt giá.
     * @param amount Số tiền đặt giá.
     * @param auction Phiên mà nó thuộc về.
     */
    public BidTransaction(Participant participant, double amount, Auction auction) {
        super(); // Tự động tạo ID
        this.participant = participant;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.auctionId = auction.getId();
    }

    public Participant getParticipant() {
        return participant;
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
