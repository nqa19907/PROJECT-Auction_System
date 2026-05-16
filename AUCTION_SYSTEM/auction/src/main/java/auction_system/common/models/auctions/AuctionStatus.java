package auction_system.common.models.auctions;

/**
 * Enum liệt kê các trạng thái có thể có của một phiên đấu giá trong hệ thống.
 */
public enum AuctionStatus {
    OPEN,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED;
}
