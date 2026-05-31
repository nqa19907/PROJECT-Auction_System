package auction_system.server.services.auth.request;

/**
 * Request nghiệp vụ để đăng nhập tài khoản.
 */
public record LoginRequest(String email, String password) {
}
