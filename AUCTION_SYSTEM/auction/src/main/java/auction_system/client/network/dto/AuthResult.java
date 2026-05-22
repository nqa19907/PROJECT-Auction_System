package auction_system.client.network.dto;

/**
 * Đối tượng vận chuyển kết quả xác thực từ tầng mạng lên tầng giao diện.
 */
public class AuthResult {

    private final boolean success;
    private final String message;

    /**
     * Khởi tạo kết quả xác thực.
     *
     * @param success trạng thái xử lý thành công hay thất bại
     * @param message thông báo lỗi hoặc thông báo nghiệp vụ
     */
    public AuthResult(final boolean success, final String message) {
        this.success = success;
        this.message = message;
    }

    /**
     * Kiểm tra thao tác xác thực có thành công hay không.
     *
     * @return true nếu thành công, false nếu thất bại
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Lấy thông báo phản hồi.
     *
     * @return thông báo phản hồi
     */
    public String getMessage() {
        return message;
    }

    /**
     * Lấy thông báo lỗi để tương thích với màn hình đăng nhập hiện tại.
     *
     * @return thông báo lỗi
     */
    public String getErrorMessage() {
        return message;
    }
}