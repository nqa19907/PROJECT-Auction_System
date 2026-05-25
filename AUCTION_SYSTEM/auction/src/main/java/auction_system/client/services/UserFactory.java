package auction_system.client.services;

import auction_system.common.models.users.Admin;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;

/**
 * Factory để khởi tạo các đối tượng User dựa trên vai trò.
 */
public final class UserFactory {

    private UserFactory() {
    }

    /**
     * Tạo đối tượng User cụ thể dựa trên role.
     *
     * @param role     Vai trò từ server (ADMIN hoặc PARTICIPANT).
     * @param username Tên người dùng.
     * @param email    Email người dùng.
     * @param balance  Số dư tài khoản.
     * @return Đối tượng User tương ứng.
     */
    public static User create(String role, String username, String email, double balance) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return new Admin(username, email, null);
        }
        return new Participant(username, email, null, balance, role);
    }
}
