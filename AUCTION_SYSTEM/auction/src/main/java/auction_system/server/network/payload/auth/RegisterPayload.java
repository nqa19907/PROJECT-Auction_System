package auction_system.server.network.payload.auth;

import auction_system.server.services.auth.request.RegisterRequest;

/**
 * Payload JSON cho request đăng ký.
 */
public record RegisterPayload(
        String username,
        String email,
        String password,
        String roleName) {

    /**
     * Kiểm tra payload thiếu các field bắt buộc.
     *
     * @return true nếu thiếu field bắt buộc
     */
    public boolean hasMissingRequiredFields() {
        return isBlank(username)
                || isBlank(email)
                || password == null
                || isBlank(roleName);
    }

    /**
     * Chuyển payload socket sang request nghiệp vụ của AuthService.
     *
     * @return request đăng ký tài khoản
     */
    public RegisterRequest toRegisterRequest() {
        return new RegisterRequest(username, email, password, roleName);
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
