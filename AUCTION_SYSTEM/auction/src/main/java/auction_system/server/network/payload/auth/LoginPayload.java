package auction_system.server.network.payload.auth;

import auction_system.server.services.auth.request.LoginRequest;

/**
 * Payload JSON cho request đăng nhập.
 */
public record LoginPayload(String email, String password) {

    /**
     * Kiểm tra payload thiếu các field bắt buộc.
     *
     * @return true nếu thiếu field bắt buộc
     */
    public boolean hasMissingRequiredFields() {
        return isBlank(email) || password == null;
    }

    /**
     * Chuyển payload socket sang request nghiệp vụ của AuthService.
     *
     * @return request đăng nhập tài khoản
     */
    public LoginRequest toLoginRequest() {
        return new LoginRequest(email, password);
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
