package auction_system.common.exceptions;

public class InvalidBidException extends RuntimeException {
    public InvalidBidException(String message) {
        super(message);
    }
}