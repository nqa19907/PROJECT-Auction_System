package auction_system.common.exceptions;

/**
 * Ngoại lệ ném ra khi cố gắng thao tác với một phiên đấu giá đã đóng hoặc chưa mở.
 */
public class AuctionClosedException extends RuntimeException {
    public AuctionClosedException(String message) {
        super(message);
    }
}