package auction_system.common.models.users;

import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import com.fasterxml.jackson.core.JsonProcessingException;
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
        return buildAdminActionRequest(Protocol.Command.ADMIN_DELETE_AUCTION, auctionId);
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
        return buildAdminActionRequest(Protocol.Command.ADMIN_DELETE_USER, userId);
    }

    private String buildAdminActionRequest(
            final Protocol.Command command,
            final String entityId) {
        // Gửi request admin action bằng JSON scalar id cho ClientHandler.
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            null,
                            command.name(),
                            null,
                            JsonProtocol.payloadOf(entityId),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON request admin action: {}", exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON " + command.name(), exception);
        }
    }

    @Override
    public void update(final String message) {
        LOGGER.info("[ADMIN NOTIFY]: {}", message);
    }
}
