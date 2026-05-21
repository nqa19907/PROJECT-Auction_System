package auction_system.common.exceptions;

/**
 * Ngoại lệ ném ra khi người dùng cung cấp sai thông tin xác thực.
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}