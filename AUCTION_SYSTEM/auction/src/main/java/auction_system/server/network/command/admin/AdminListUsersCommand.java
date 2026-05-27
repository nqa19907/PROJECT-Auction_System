package auction_system.server.network.command.admin;

import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
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
        /*
         * Danh sách user là dữ liệu quản trị nên command kiểm tra quyền ở server.
         * Controller client không thể tự quyết định quyền chỉ bằng màn hình đang mở.
         */
        if (!isAdmin(session)) {
            return Protocol.Response.ADMIN_USER_LIST_FAIL.name()
                    + Protocol.SEPARATOR + "Bạn không có quyền quản trị.";
        }

        final List<User> users = auctionManager.getAllUsers();
        final StringBuilder response = new StringBuilder()
                .append(Protocol.Response.ADMIN_USER_LIST.name())
                .append(Protocol.SEPARATOR)
                .append(users.size());


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

    private boolean isAdmin(final ClientSession session) {
        // Chỉ user đã đăng nhập trong session và có role ADMIN mới được đọc danh sách.
        final User currentUser = session.getCurrentUser();
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRoleName());
    }
}
