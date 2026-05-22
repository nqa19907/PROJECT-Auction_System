package auction_system.common.models.users;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp đại diện cho Quản trị viên hệ thống.
 */
public class Admin extends User {
    private static final Logger LOGGER = LoggerFactory.getLogger(Admin.class);

    public Admin(String username, String email, String password) {
        super(username, email, password);
    }

    @Override
    public String getRoleName() {
        return "ADMIN";
    }

    @Override
    public String getRoleDisplayName() {
        return "Quản trị viên";
    }

    /**
     * Xóa một phiên đấu giá khỏi hệ thống.
     *
     * @param auctionId ID của phiên đấu giá cần xóa.
     */
    public void deleteAuction(String auctionId) {
        LOGGER.info("Admin [{}] đang yêu cầu xóa phiên đấu giá: {}", getUsername(), auctionId);
        // Logic gửi lệnh DELETE_AUCTION lên server sẽ được viết sau
    }

    /**
     * Xóa người dùng khỏi hệ thống.
     *
     * @param userId ID của người dùng cần xóa.
     */
    public void deleteUser(String userId) {
        LOGGER.info("Admin [{}] đang yêu cầu xóa người dùng: {}", getUsername(), userId);
        // Logic gửi lệnh DELETE_USER lên server sẽ được viết sau
    }

    @Override
    public void update(String message) {
        LOGGER.info("[ADMIN NOTIFY]: " + message);
    }
}
