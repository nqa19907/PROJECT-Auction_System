package auction_system.client.network.dto;

/**
 * Đối tượng vận chuyển kết quả đăng nhập từ tầng mạng lên tầng giao diện.
 */
public class LoginResult {
    private final boolean success;
    private final String errorMessage;

    public LoginResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
