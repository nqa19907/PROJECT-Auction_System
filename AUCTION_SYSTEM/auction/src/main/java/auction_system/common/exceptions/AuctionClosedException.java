package auction_system.common.exceptions;

public class AuctionClosedException extends RuntimeException {
    public AuctionClosedException(String message) {
        super(message);
    }
}
