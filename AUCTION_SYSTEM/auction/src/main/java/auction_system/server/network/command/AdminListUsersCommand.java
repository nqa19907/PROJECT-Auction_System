package auction_system.server.network.command;

import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.session.ClientSession;
import java.util.List;

/**
 * Command cho phép ADMIN lấy danh sách người dùng qua socket.
 */
public class AdminListUsersCommand implements Command {
    private final AuctionManager auctionManager;

    public AdminListUsersCommand(final AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        User currentUser = session.getCurrentUser();
        if (currentUser == null || !"ADMIN".equalsIgnoreCase(currentUser.getRoleName())) {
            return Protocol.Response.ERROR.name()
                    + Protocol.SEPARATOR + "Bạn không có quyền quản trị.";
        }

        List<User> users = auctionManager.getAllUsers();

        StringBuilder response = new StringBuilder();
        response.append(Protocol.Response.ADMIN_USER_LIST.name())
                .append(Protocol.SEPARATOR).append(users.size());

        for (User u : users) {
            response.append(Protocol.RECORD_SEPARATOR)
                    .append(u.getId()).append(Protocol.SEPARATOR)
                    .append(u.getUsername()).append(Protocol.SEPARATOR)
                    .append(u.getEmail()).append(Protocol.SEPARATOR)
                    .append(u.isOnline() ? "ONLINE" : "OFFLINE").append(Protocol.SEPARATOR)
                    .append(u.getRoleName());
        }

        return response.toString();
    }
}
