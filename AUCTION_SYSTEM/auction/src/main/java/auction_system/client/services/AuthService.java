package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.client.network.dto.LoginResult;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service xử lý các tác vụ liên quan đến xác thực người dùng.
 *
 * <p>Singleton: Eager Initialization
 */
public final class AuthService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private AuthCallback currentCallback;

    // Định nghĩa hằng số cho cấu trúc gói tin LOGIN_OK: LOGIN_OK|userId|username|email|role
    private static final int IDX_USER_ID = 1;
    private static final int IDX_USERNAME = 2;
    private static final int IDX_EMAIL = 3;
    private static final int IDX_ROLE = 4;
    private static final int IDX_BALANCE = 5;
    private User currentUser; // Đảm bảo dùng lớp cha User

    private static final AuthService INSTANCE = new AuthService();

    private AuthService() {
        NetworkClient.getInstance().registerHandler(
            Protocol.Response.LOGIN_OK.name(), this::handleLoginSuccess);
        NetworkClient.getInstance().registerHandler(
            Protocol.Response.LOGIN_FAIL.name(), this::handleLoginFailure);
        NetworkClient.getInstance().registerHandler(
            Protocol.Response.LOGOUT_OK.name(), this::handleLogoutResponse);
    }


    /**
     * Lấy instance duy nhất của AuthService.
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

    private void handleLoginSuccess(String response) {
        if (currentCallback == null) {
            return;
        }

        LOGGER.info("Đăng nhập thành công, đang khởi tạo dữ liệu người dùng.");
        String[] parts = response.split(Protocol.SEPARATOR_REGEX);
        
        // Trích xuất thông tin an toàn
        String username = getPart(parts, IDX_USERNAME, "Unknown");
        String email = getPart(parts, IDX_EMAIL, "unknown@example.com");
        String role = getPart(parts, IDX_ROLE, "BIDDER");
        double balance = parseDouble(getPart(parts, IDX_BALANCE, "0.0"));

        // SRP: Ủy thác việc tạo User cho Factory.
        this.currentUser = UserFactory.create(role, username, email, balance);
        UserSessionService.getInstance().setCurrentUser(this.currentUser);
        
        currentCallback.onResult(new LoginResult(true, null));
        currentCallback = null;
    }

    private void handleLoginFailure(String response) {
        if (currentCallback == null) {
            return;
        }

        String[] parts = response.split(Protocol.SEPARATOR_REGEX);
        String reason = (parts.length > 1) ? parts[1] : "Sai tài khoản hoặc mật khẩu!";
        
        currentCallback.onResult(new LoginResult(false, reason));
        currentCallback = null;
    }

    private String getPart(String[] parts, int index, String defaultValue) {
        return (parts.length > index) ? parts[index] : defaultValue;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            LOGGER.error("Không thể phân tích giá trị số: {}", value);
            return 0.0;
        }
    }

    /**
     * Xử lý gửi gói tin đăng xuất từ mạng.
     *
     * @param callback Hàm phản hồi sau khi nhận kết quả từ server.
     */
    public void logout(AuthCallback callback) {
        this.currentCallback = callback;
        String request = Protocol.Command.LOGOUT.name();

        boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent) {
            if (this.currentCallback != null) {
                this.currentCallback.onResult(new LoginResult(false, "Mất kết nối tới máy chủ!"));
                this.currentCallback = null;
            }
        }
    }

    private void handleLogoutResponse(String response) {
        if (currentCallback == null) {
            return;
        }

        LOGGER.info("AuthService xử lý phản hồi đăng xuất: " + response);
        String[] parts = response.split(Protocol.SEPARATOR_REGEX);
        String cmd = parts[0];

        if (Protocol.Response.LOGOUT_OK.name().equals(cmd)) {
            this.currentUser = null;
            UserSessionService.getInstance().clearSession();
            currentCallback.onResult(new LoginResult(true, null));
        }

        // Giải phóng callback
        currentCallback = null;
    }

    public User getCurrentUser() {
        return UserSessionService.getInstance().getCurrentUser();
    }
}
