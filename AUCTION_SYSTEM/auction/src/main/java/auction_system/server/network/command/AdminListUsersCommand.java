package auction_system.server.network.command;

import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.session.ClientSession;
import java.util.List;
import java.util.Objects;

/**
 * Command trả danh sách user cho màn hình quản trị.
 */
public class AdminListUsersCommand implements Command {

    private final AuctionManager auctionManager;

    public AdminListUsersCommand(final AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        if (!isAdmin(session)) {
            return Protocol.Response.ADMIN_USER_LIST_FAIL.name()
                    + Protocol.SEPARATOR + "Bạn không có quyền quản trị.";
        }

        final List<User> users = auctionManager.getAllUsers();
        final StringBuilder response = new StringBuilder()
                .append(Protocol.Response.ADMIN_USER_LIST.name())
                .append(Protocol.SEPARATOR)
                .append(users.size());

        for (User user : users) {
            final String status = auctionManager.isAlreadyOnline(user.getId())
                    ? "ONLINE"
                    : "OFFLINE";

            response.append(Protocol.RECORD_SEPARATOR)
                    .append(user.getId())
                    .append(Protocol.SEPARATOR).append(user.getUsername())
                    .append(Protocol.SEPARATOR).append(user.getEmail())
                    .append(Protocol.SEPARATOR).append(status);
        }

        return response.toString();
    }

    private boolean isAdmin(final ClientSession session) {
        final User currentUser = session.getCurrentUser();
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRoleName());
    }
}
