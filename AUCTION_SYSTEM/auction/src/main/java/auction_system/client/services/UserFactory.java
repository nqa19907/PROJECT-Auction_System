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
        return create(role, null, username, email, balance);
    }

    /**
     * Tạo đối tượng User cụ thể dựa trên role và giữ nguyên id từ server.
     *
     * @param role     Vai trò từ server (ADMIN hoặc PARTICIPANT).
     * @param userId   Mã người dùng do server quản lý.
     * @param username Tên người dùng.
     * @param email    Email người dùng.
     * @param balance  Số dư tài khoản.
     * @return Đối tượng User tương ứng.
     */
    public static User create(
            String role,
            String userId,
            String username,
            String email,
            double balance) {
        final User user;
        if ("ADMIN".equalsIgnoreCase(role)) {
            user = new Admin(username, email, null);
        } else {
            user = new Participant(username, email, null, balance, role);
        }

        if (userId != null && !userId.isBlank()) {
            user.setId(userId);
        }

        return user;
    }
}
