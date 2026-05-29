package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.client.network.dto.AuthResult;
import auction_system.common.models.users.User;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;
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
    private static final int loginUserIdIndex = 1;
    private static final int loginUsernameIndex = 2;
    private static final int loginEmailIndex = 3;
    private static final int loginRoleIndex = 4;
    private static final int loginBalanceIndex = 5;
    private static final int minBalanceUpdatedPartCount = 2;
    private static final int balanceUpdatedValueIndex = 1;

    private AuthCallback loginCallback;
    private AuthCallback registerCallback;
    private AuthCallback logoutCallback;

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
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.LOGOUT_OK.name(),
                this::handleLogoutResponse);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.BALANCE_UPDATED.name(),
                this::handleBalanceUpdatedResponse);
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

        final String request = buildLoginRequest(email, password);

        final NetworkClient networkClient = NetworkClient.getInstance();
        if (!networkClient.ensureConnected()) {
            notifyLoginAndClear(new AuthResult(false, "Mất kết nối tới máy chủ"));
            return;
        }

        final boolean sent = networkClient.sendCommand(request);
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

        final String request = buildRegisterRequest(username, email, password, roleName);

        final NetworkClient networkClient = NetworkClient.getInstance();
        if (!networkClient.ensureConnected()) {
            notifyRegisterAndClear(new AuthResult(false, "Mất kết nối tới máy chủ"));
            return;
        }

        final boolean sent = networkClient.sendCommand(request);
        if (!sent) {
            notifyRegisterAndClear(new AuthResult(false, "Mất kết nối tới máy chủ."));
        }
    }

    /**
     * Gửi yêu cầu đăng xuất tới server và không chờ callback.
     */
    public void logout() {
        logout(result -> {
        });
    }

    /**
     * Gửi yêu cầu đăng xuất tới server.
     *
     * @param callback callback nhận kết quả đăng xuất
     */
    public void logout(final AuthCallback callback) {
        logoutCallback = callback;

        final NetworkClient networkClient = NetworkClient.getInstance();
        if (!networkClient.ensureConnected()) {
            notifyLogoutAndClear(new AuthResult(false, "Mất kết nối tới máy chủ"));
            return;
        }

        final boolean sent = networkClient.sendCommand(buildLogoutRequest());
        if (!sent) {
            notifyLogoutAndClear(new AuthResult(false, "Mất kết nối tới máy chủ."));
        }
    }

    /**
     * Tạo request JSON cho lệnh đăng xuất.
     *
     * @return request JSON, hoặc protocol string cũ nếu serialize thất bại
     */
    private String buildLogoutRequest() {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            null,
                            Protocol.Command.LOGOUT.name(),
                            null,
                            null,
                            null));
        } catch (JsonProcessingException exception) {
            logger.warning("Không tạo được JSON request đăng xuất: " + exception.getMessage());
            return Protocol.Command.LOGOUT.name();
        }
    }

    /**
     * Tạo request JSON cho lệnh đăng nhập.
     *
     * @param email email người dùng nhập
     * @param password mật khẩu gốc
     * @return request JSON, hoặc protocol string cũ nếu serialize thất bại
     */
    private String buildLoginRequest(final String email, final String password) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            null,
                            Protocol.Command.LOGIN.name(),
                            null,
                            JsonProtocol.payloadOf(List.of(email, password)),
                            null));
        } catch (JsonProcessingException exception) {
            logger.warning("Không tạo được JSON request đăng nhập: " + exception.getMessage());
            return Protocol.Command.LOGIN.name()
                    + Protocol.SEPARATOR
                    + email
                    + Protocol.SEPARATOR
                    + password;
        }
    }

    /**
     * Tạo request JSON cho lệnh đăng ký.
     *
     * @param username tên đăng nhập
     * @param email email đăng ký
     * @param password mật khẩu gốc
     * @param roleName vai trò người dùng
     * @return request JSON, hoặc protocol string cũ nếu serialize thất bại
     */
    private String buildRegisterRequest(
            final String username,
            final String email,
            final String password,
            final String roleName) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            null,
                            Protocol.Command.REGISTER.name(),
                            null,
                            JsonProtocol.payloadOf(List.of(username, email, password, roleName)),
                            null));
        } catch (JsonProcessingException exception) {
            logger.warning("Không tạo được JSON request đăng ký: " + exception.getMessage());
            return Protocol.Command.REGISTER.name()
                    + Protocol.SEPARATOR
                    + username
                    + Protocol.SEPARATOR
                    + email
                    + Protocol.SEPARATOR
                    + password
                    + Protocol.SEPARATOR
                    + roleName;
        }
    }

    /**
     * Lấy người dùng đang đăng nhập ở client.
     *
     * @return người dùng hiện tại hoặc null nếu chưa đăng nhập
     */
    public User getCurrentUser() {
        return UserSessionService.getInstance().getCurrentUser();
    }

    /**
     * Xử lý phản hồi đăng nhập từ server.
     *
     * @param response phản hồi dạng chuỗi từ server
     */
    private void handleLoginResponse(final String response) {
        logger.info("Xử lý phản hồi đăng nhập: " + response);

        if (JsonProtocol.isJsonObject(response)) {
            handleLoginJsonResponse(response);
            return;
        }

        final String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
        final String command = parts[0];

        if (Protocol.Response.LOGIN_OK.name().equals(command)) {
            final AuthResult result = new AuthResult(
                    true,
                    null,
                    extractPart(parts, loginUserIdIndex),
                    extractPart(parts, loginUsernameIndex),
                    extractPart(parts, loginEmailIndex),
                    extractPart(parts, loginRoleIndex),
                    parseDouble(extractPart(parts, loginBalanceIndex)));
            storeCurrentUser(result);
            notifyLoginAndClear(result);
            return;
        }

        final String message = extractMessage(parts, "Email hoặc mật khẩu không đúng.");
        notifyLoginAndClear(new AuthResult(false, message));
    }

    /**
     * Xử lý phản hồi đăng nhập dạng JSON.
     *
     * @param response phản hồi JSON từ server
     */
    private void handleLoginJsonResponse(final String response) {
        try {
            final JsonMessage message = JsonProtocol.parse(response);
            if (Protocol.Response.LOGIN_OK.name().equals(message.type())) {
                final JsonNode payload = message.payload();
                final AuthResult result = new AuthResult(
                        true,
                        null,
                        textValue(payload, "userId"),
                        textValue(payload, "username"),
                        textValue(payload, "email"),
                        textValue(payload, "roleName"),
                        numberValue(payload, "balance"));
                storeCurrentUser(result);
                notifyLoginAndClear(result);
                return;
            }

            final String errorMessage = message.message() == null || message.message().isBlank()
                    ? "Email hoặc mật khẩu không đúng."
                    : message.message();
            notifyLoginAndClear(new AuthResult(false, errorMessage));
        } catch (IOException exception) {
            logger.warning("Không thể đọc JSON phản hồi đăng nhập: " + exception.getMessage());
            notifyLoginAndClear(new AuthResult(false, "Email hoặc mật khẩu không đúng."));
        }
    }

    /**
     * Xử lý phản hồi đăng ký từ server.
     *
     * @param response phản hồi dạng chuỗi từ server
     */
    private void handleRegisterResponse(final String response) {
        logger.info("Xử lý phản hồi đăng ký: " + response);

        if (JsonProtocol.isJsonObject(response)) {
            handleRegisterJsonResponse(response);
            return;
        }

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
     * Xử lý phản hồi đăng ký dạng JSON.
     *
     * @param response phản hồi JSON từ server
     */
    private void handleRegisterJsonResponse(final String response) {
        try {
            final JsonMessage message = JsonProtocol.parse(response);
            if (Protocol.Response.REGISTER_OK.name().equals(message.type())) {
                notifyRegisterAndClear(new AuthResult(true, "Đăng ký tài khoản thành công."));
                return;
            }

            final String errorMessage = message.message() == null || message.message().isBlank()
                    ? "Đăng ký tài khoản thất bại."
                    : message.message();
            notifyRegisterAndClear(new AuthResult(false, errorMessage));
        } catch (IOException exception) {
            logger.warning("Không thể đọc JSON phản hồi đăng ký: " + exception.getMessage());
            notifyRegisterAndClear(new AuthResult(false, "Đăng ký tài khoản thất bại."));
        }
    }

    /**
     * Xử lý phản hồi đăng xuất từ server.
     *
     * @param response phản hồi dạng chuỗi từ server
     */
    private void handleLogoutResponse(final String response) {
        logger.info("Xử lý phản hồi đăng xuất: " + response);
        UserSessionService.getInstance().clearSession();
        notifyLogoutAndClear(new AuthResult(true, null));
    }

    /**
     * Cập nhật số dư realtime độc lập với màn hình hiện tại.
     *
     * <p>Handler này đặt ở AuthService vì AuthService luôn được khởi tạo trong
     * vòng đời đăng nhập, nên người bị outbid vẫn thấy số dư được hoàn ngay cả
     * khi không đứng ở màn chi tiết đấu giá.
     *
     * @param response phản hồi BALANCE_UPDATED từ server
     */
    private void handleBalanceUpdatedResponse(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
        if (parts.length < minBalanceUpdatedPartCount) {
            return;
        }

        UserSessionService.getInstance().updateCurrentUserBalance(
                parseDouble(parts[balanceUpdatedValueIndex]));
    }

    /**
     * Lưu người dùng vừa đăng nhập thành công vào session client.
     *
     * @param result kết quả đăng nhập thành công
     */
    private void storeCurrentUser(final AuthResult result) {
        final User user = UserFactory.create(
                result.getRoleName(),
                result.getUserId(),
                result.getUsername(),
                result.getEmail(),
                result.getBalance());
        UserSessionService.getInstance().setCurrentUser(user);
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
     * Lấy chuỗi từ payload JSON nếu tồn tại.
     *
     * @param payload payload JSON
     * @param fieldName tên field cần đọc
     * @return giá trị text hoặc null nếu không có
     */
    private String textValue(final JsonNode payload, final String fieldName) {
        if (payload == null || payload.path(fieldName).isMissingNode()) {
            return null;
        }

        return payload.path(fieldName).asText(null);
    }

    /**
     * Lấy số từ payload JSON nếu tồn tại.
     *
     * @param payload payload JSON
     * @param fieldName tên field cần đọc
     * @return giá trị số hoặc 0 nếu không có
     */
    private double numberValue(final JsonNode payload, final String fieldName) {
        if (payload == null || payload.path(fieldName).isMissingNode()) {
            return 0;
        }

        return payload.path(fieldName).asDouble(0);
    }

    /**
     * Chuyển chuỗi số dư từ server sang double.
     *
     * @param value giá trị số dạng chuỗi
     * @return giá trị double, hoặc 0 nếu dữ liệu không hợp lệ
     */
    private double parseDouble(final String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            logger.warning("Không thể phân tích giá trị số: " + value);
            return 0;
        }
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

    /**
     * Gửi kết quả đăng xuất về UI và xóa callback đang chờ.
     *
     * @param result kết quả đăng xuất
     */
    private void notifyLogoutAndClear(final AuthResult result) {
        if (logoutCallback != null) {
            logoutCallback.onResult(result);
            logoutCallback = null;
        }
    }
}
