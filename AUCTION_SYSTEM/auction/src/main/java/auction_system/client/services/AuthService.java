package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.client.network.dto.LoginResult;
import auction_system.common.network.Protocol;
import auction_system.common.utils.SecurityUtils;
import java.util.logging.Logger;

/**
 * Service xử lý các tác vụ liên quan đến xác thực người dùng.
 *
 * <p>Singleton: Eager Initialization
 */
public final class AuthService {
    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());

    private AuthCallback currentCallback;
    /** Database dùng để truy xuất dữ liệu người dùng. */

    private static final AuthService INSTANCE = new AuthService();

    private AuthService() {
        NetworkClient.getInstance().registerHandler(
            Protocol.Response.LOGIN_OK.name(), this::handleLoginResponse);
        NetworkClient.getInstance().registerHandler(
            Protocol.Response.LOGIN_FAIL.name(), this::handleLoginResponse);
    }


    /**
     * Lấy instance duy nhất của AuthService.
     *
     * <p>Lần gọi đầu tiên bắt buộc phải truyền database để khởi tạo service.
     *
     * @return instance duy nhất của AuthService
     */
    public static AuthService getInstance() {
        return INSTANCE;
    }

    /**
     * Giao diện chức năng phục vụ phản hồi bất đồng bộ từ Server.
     */
    @FunctionalInterface
    public interface AuthCallback {
        void onResult(LoginResult result);
    }

    /**
     * Xử lý gửi gói tin đăng nhập và phân tích phản hồi thô từ mạng.
     *
     * @param email    Địa chỉ email người dùng.
     * @param password Mật khẩu người dùng.
     * @param callback Hàm phản hồi sau khi nhận kết quả từ server.
     */
    public void login(String email, String password, AuthCallback callback) {
        this.currentCallback = callback;
        
        // Băm mật khẩu ra chuỗi SHA-256 trước khi gửi
        String hashedPassword = SecurityUtils.hashPassword(password);
        String request = Protocol.Command.LOGIN.name()
                        + Protocol.SEPARATOR
                        + email
                        + Protocol.SEPARATOR
                        + hashedPassword;

        boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent) {
            if (this.currentCallback != null) {
                this.currentCallback.onResult(new LoginResult(false, "Mất kết nối tới máy chủ!"));
                this.currentCallback = null;
            }
        }
    }

    private void handleLoginResponse(String response) {
        if (currentCallback == null) {
            return;
        }

        LOGGER.info("AuthService xử lý phản hồi: " + response);
        String[] parts = response.split(Protocol.SEPARATOR_REGEX);
        String cmd = parts[0];

        if (Protocol.Response.LOGIN_OK.name().equals(cmd)) {
            currentCallback.onResult(new LoginResult(true, null));
        } else if (Protocol.Response.LOGIN_FAIL.name().equals(cmd)) {
            String reason = (parts.length > 1) ? parts[1] : "Sai tài khoản hoặc mật khẩu!";
            currentCallback.onResult(new LoginResult(false, reason));
        }

        // Giải phóng callback sau khi dùng xong tránh kẹt bộ nhớ
        currentCallback = null;
    }

    /**
     * Gửi yêu cầu đăng xuất tới server.
     *
     * <p>Server sẽ xử lý việc xóa người dùng khỏi danh sách online thông qua
     * LogoutCommand. Client không được gọi trực tiếp AuctionManager.
     */
    public void logout() {
        NetworkClient.getInstance().sendCommand(Protocol.Command.LOGOUT.name());
    }
}
