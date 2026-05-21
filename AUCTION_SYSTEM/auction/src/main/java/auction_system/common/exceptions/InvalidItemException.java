package auction_system.common.exceptions;

/**
 * Ngoại lệ ném ra khi một sản phẩm không hợp lệ (thông tin thiếu hoặc lỗi).
 */
public class InvalidItemException extends RuntimeException {
    public InvalidItemException(String message) {
        super(message);
    }
}