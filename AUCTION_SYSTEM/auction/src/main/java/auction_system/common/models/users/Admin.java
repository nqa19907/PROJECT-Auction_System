package auction_system.common.models.users;

import auction_system.common.network.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp đại diện cho Quản trị viên hệ thống.
 */
public class Admin extends User {
    private static final Logger LOGGER = LoggerFactory.getLogger(Admin.class);

    public Admin(final String username, final String email, final String password) {
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
     * Tạo request xóa một phiên đấu giá.
     *
     * @param auctionId id của phiên đấu giá cần xóa
     * @return command gửi lên server
     */
    public String deleteAuction(final String auctionId) {
        LOGGER.info(
                "Admin [{}] đang yêu cầu xóa phiên đấu giá: {}",
                getUsername(),
                auctionId
        );
        return Protocol.Command.ADMIN_DELETE_AUCTION.name()
                + Protocol.SEPARATOR + auctionId;
    }

    /**
     * Tạo request xóa một người dùng.
     *
     * @param userId id của người dùng cần xóa
     * @return command gửi lên server
     */
    public String deleteUser(final String userId) {
        LOGGER.info(
                "Admin [{}] đang yêu cầu xóa người dùng: {}",
                getUsername(),
                userId
        );
        return Protocol.Command.ADMIN_DELETE_USER.name()
                + Protocol.SEPARATOR + userId;
    }

    @Override
    public void update(final String message) {
        LOGGER.info("[ADMIN NOTIFY]: {}", message);
    }
}
