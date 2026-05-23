package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.client.network.dto.AuthResult;
import auction_system.common.network.Protocol;
import java.util.logging.Logger;

/**
 * Service xử lý xác thực ở phía client.
 *
 * <p>Service này chỉ gửi request qua socket và nhận response bất đồng bộ từ server.
 * Logic kiểm tra mật khẩu, hash mật khẩu và ghi database thuộc trách nhiệm của server.
 */
public final class AuthService {

    private static final Logger logger = Logger.getLogger(AuthService.class.getName());
    private static final AuthService instance = new AuthService();

    private AuthCallback loginCallback;
    private AuthCallback registerCallback;

    private AuthService() {
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.LOGIN_OK.name(),
                this::handleLoginResponse);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.LOGIN_FAIL.name(),
                this::handleLoginResponse);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.REGISTER_OK.name(),
                this::handleRegisterResponse);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.REGISTER_FAIL.name(),
                this::handleRegisterResponse);
    }

    /**
     * Lấy service xác thực duy nhất ở phía client.
     *
     * @return instance duy nhất của AuthService
     */
    public static AuthService getInstance() {
        return instance;
    }

    /**
     * Callback nhận kết quả xác thực bất đồng bộ từ server.
     */
    @FunctionalInterface
    public interface AuthCallback {

        /**
         * Nhận kết quả xác thực.
         *
         * @param result kết quả xác thực đã được parse
         */
        void onResult(AuthResult result);
    }

    /**
     * Gửi yêu cầu đăng nhập tới server.
     *
     * @param email email người dùng nhập
     * @param password mật khẩu gốc từ form đăng nhập
     * @param callback callback nhận kết quả đăng nhập
     */
    public void login(
            final String email,
            final String password,
            final AuthCallback callback) {
        loginCallback = callback;

        if (containsSeparator(email) || containsSeparator(password)) {
            notifyLoginAndClear(new AuthResult(false, "Thông tin đăng nhập không hợp lệ."));
            return;
        }

        final String request = Protocol.Command.LOGIN.name()
                + Protocol.SEPARATOR
                + email
                + Protocol.SEPARATOR
                + password;

        final boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent) {
            notifyLoginAndClear(new AuthResult(false, "Mất kết nối tới máy chủ."));
        }
    }

    /**
     * Gửi yêu cầu đăng ký tài khoản tới server.
     *
     * @param username tên đăng nhập
     * @param email email đăng ký
     * @param password mật khẩu gốc từ form đăng ký
     * @param roleName vai trò người dùng
     * @param callback callback nhận kết quả đăng ký
     */
    public void register(
            final String username,
            final String email,
            final String password,
            final String roleName,
            final AuthCallback callback) {
        registerCallback = callback;

        if (containsSeparator(username)
                || containsSeparator(email)
                || containsSeparator(password)
                || containsSeparator(roleName)) {
            notifyRegisterAndClear(new AuthResult(false, "Thông tin đăng ký không hợp lệ."));
            return;
        }

        final String request = Protocol.Command.REGISTER.name()
                + Protocol.SEPARATOR
                + username
                + Protocol.SEPARATOR
                + email
                + Protocol.SEPARATOR
                + password
                + Protocol.SEPARATOR
                + roleName;

        final boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent) {
            notifyRegisterAndClear(new AuthResult(false, "Mất kết nối tới máy chủ."));
        }
    }

    /**
     * Gửi yêu cầu đăng xuất tới server.
     */
    public void logout() {
        NetworkClient.getInstance().sendCommand(Protocol.Command.LOGOUT.name());
    }

    /**
     * Xử lý phản hồi đăng nhập từ server.
     *
     * @param response phản hồi dạng chuỗi từ server
     */
    private void handleLoginResponse(final String response) {
        logger.info("Xử lý phản hồi đăng nhập: " + response);

        final String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
        final String command = parts[0];

        if (Protocol.Response.LOGIN_OK.name().equals(command)) {
            notifyLoginAndClear(new AuthResult(
                    true,
                    null,
                    extractPart(parts, 1),
                    extractPart(parts, 2),
                    extractPart(parts, 3)));
            return;
        }

        final String message = extractMessage(parts, "Email hoặc mật khẩu không đúng.");
        notifyLoginAndClear(new AuthResult(false, message));
    }

    /**
     * Xử lý phản hồi đăng ký từ server.
     *
     * @param response phản hồi dạng chuỗi từ server
     */
    private void handleRegisterResponse(final String response) {
        logger.info("Xử lý phản hồi đăng ký: " + response);

        final String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
        final String command = parts[0];

        if (Protocol.Response.REGISTER_OK.name().equals(command)) {
            notifyRegisterAndClear(new AuthResult(true, "Đăng ký tài khoản thành công."));
            return;
        }

        final String message = extractMessage(parts, "Đăng ký tài khoản thất bại.");
        notifyRegisterAndClear(new AuthResult(false, message));
    }

    /**
     * Tách thông báo từ response.
     *
     * @param parts mảng dữ liệu đã tách từ response
     * @param defaultMessage thông báo mặc định
     * @return thông báo phù hợp để hiển thị
     */
    private String extractMessage(final String[] parts, final String defaultMessage) {
        if (parts.length > 1 && !parts[1].isBlank()) {
            return parts[1];
        }

        return defaultMessage;
    }

    /**
     * Lấy một phần dữ liệu trong response nếu tồn tại.
     *
     * @param parts mảng dữ liệu đã tách từ response
     * @param index vị trí cần lấy
     * @return giá trị tại vị trí tương ứng hoặc null nếu không tồn tại
     */
    private String extractPart(final String[] parts, final int index) {
        if (parts.length > index && !parts[index].isBlank()) {
            return parts[index];
        }

        return null;
    }

    /**
     * Kiểm tra dữ liệu có chứa ký tự phân tách protocol hay không.
     *
     * @param value giá trị cần kiểm tra
     * @return true nếu chứa ký tự phân tách
     */
    private boolean containsSeparator(final String value) {
        return value != null && value.contains(Protocol.SEPARATOR);
    }

    /**
     * Gửi kết quả đăng nhập về UI và xóa callback đang chờ.
     *
     * @param result kết quả đăng nhập
     */
    private void notifyLoginAndClear(final AuthResult result) {
        if (loginCallback != null) {
            loginCallback.onResult(result);
            loginCallback = null;
        }
    }

    /**
     * Gửi kết quả đăng ký về UI và xóa callback đang chờ.
     *
     * @param result kết quả đăng ký
     */
    private void notifyRegisterAndClear(final AuthResult result) {
        if (registerCallback != null) {
            registerCallback.onResult(result);
            registerCallback = null;
        }
    }
}
