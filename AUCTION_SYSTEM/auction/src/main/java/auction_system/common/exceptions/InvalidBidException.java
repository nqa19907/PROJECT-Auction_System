package auction_system.common.exceptions;

/**
 * Ngoại lệ ném ra khi mức giá đặt cược không hợp lệ (ví dụ: thấp hơn giá hiện tại).
 */
public class InvalidBidException extends RuntimeException {
    public InvalidBidException(String message) {
        super(message);
    }
}