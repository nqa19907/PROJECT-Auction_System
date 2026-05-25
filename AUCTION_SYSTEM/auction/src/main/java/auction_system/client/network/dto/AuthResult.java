package auction_system.client.network.dto;

/**
 * Đối tượng vận chuyển kết quả xác thực từ tầng mạng lên tầng giao diện.
 */
public class AuthResult {

    private final boolean success;
    private final String message;
    private final String userId;
    private final String username;
    private final String email;
    private final String roleName;
    private final double balance;

    /**
     * Khởi tạo kết quả xác thực.
     *
     * @param success trạng thái xử lý thành công hay thất bại
     * @param message thông báo lỗi hoặc thông báo nghiệp vụ
     */
    public AuthResult(final boolean success, final String message) {
        this(success, message, null, null, null, null, 0);
    }

    /**
     * Khởi tạo kết quả xác thực kèm thông tin người dùng.
     *
     * @param success trạng thái xử lý thành công hay thất bại
     * @param message thông báo lỗi hoặc thông báo nghiệp vụ
     * @param userId mã người dùng được server xác thực
     * @param username tên đăng nhập của người dùng
     * @param roleName vai trò của người dùng
     */
    public AuthResult(
            final boolean success,
            final String message,
            final String userId,
            final String username,
            final String roleName) {
        this(success, message, userId, username, null, roleName, 0);
    }

    /**
     * Khởi tạo kết quả xác thực kèm đầy đủ thông tin người dùng.
     *
     * @param success trạng thái xử lý thành công hay thất bại
     * @param message thông báo lỗi hoặc thông báo nghiệp vụ
     * @param userId mã người dùng được server xác thực
     * @param username tên đăng nhập của người dùng
     * @param email email của người dùng
     * @param roleName vai trò của người dùng
     * @param balance số dư hiện tại của người dùng
     */
    public AuthResult(
            final boolean success,
            final String message,
            final String userId,
            final String username,
            final String email,
            final String roleName,
            final double balance) {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.roleName = roleName;
        this.balance = balance;
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

    /**
     * Lấy mã người dùng sau khi đăng nhập thành công.
     *
     * @return mã người dùng hoặc null nếu xác thực thất bại
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Lấy tên đăng nhập sau khi đăng nhập thành công.
     *
     * @return tên đăng nhập hoặc null nếu xác thực thất bại
     */
    public String getUsername() {
        return username;
    }

    /**
     * Lấy email sau khi đăng nhập thành công.
     *
     * @return email hoặc null nếu xác thực thất bại
     */
    public String getEmail() {
        return email;
    }

    /**
     * Lấy vai trò người dùng sau khi đăng nhập thành công.
     *
     * @return vai trò người dùng hoặc null nếu xác thực thất bại
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * Lấy số dư sau khi đăng nhập thành công.
     *
     * @return số dư tài khoản, hoặc 0 nếu không có ví
     */
    public double getBalance() {
        return balance;
    }
}
