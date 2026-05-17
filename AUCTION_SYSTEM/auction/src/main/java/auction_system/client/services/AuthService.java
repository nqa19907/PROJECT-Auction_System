package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.client.network.dto.LoginResult;
import auction_system.common.network.Protocol;
import java.util.logging.Logger;

/**
 * Service xử lý các tác vụ liên quan đến xác thực người dùng.
 *
 * <p>Singleton: Eager Initialization
 */
public final class AuthService {
    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());
    private static final AuthService INSTANCE = new AuthService();

    private AuthService() {}

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
        String request = Protocol.Command.LOGIN.name()
                        + Protocol.SEPARATOR
                        + email
                        + Protocol.SEPARATOR
                        + password;

        // Đăng ký nhận tin nhắn từ NetworkClient và ép kiểu dữ liệu
        NetworkClient.getInstance().setMessageHandler(response -> {
            LOGGER.info("AuthService nhận chuỗi thô: " + response);
            String[] parts = response.split(Protocol.SEPARATOR_REGEX);
            String cmd = parts[0];

            if (Protocol.Response.LOGIN_OK.name().equals(cmd)) {
                callback.onResult(new LoginResult(true, null));
            } else if (Protocol.Response.LOGIN_FAIL.name().equals(cmd)) {
                String reason = (parts.length > 1) ? parts[1] : "Sai tài khoảng hoặc mật khẩu!";
                callback.onResult(new LoginResult(false, reason));
            }
        });

        boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent) {
            callback.onResult(new LoginResult(false, "Mất kết nối tới máy chủ!"));
        }
    }
}
