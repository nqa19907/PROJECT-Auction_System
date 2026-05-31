package auction_system.server.services.auth.request;

/**
 * Request nghiệp vụ để đăng ký tài khoản mới.
 */
public record RegisterRequest(
        String username,
        String email,
        String password,
        String roleName) {
}
