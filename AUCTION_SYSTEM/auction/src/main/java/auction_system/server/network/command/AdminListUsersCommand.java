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
        /*
         * Danh sách user là dữ liệu quản trị nên command kiểm tra quyền ở server.
         * Controller client không thể tự quyết định quyền chỉ bằng màn hình đang mở.
         */
        if (!isAdmin(session)) {
            return Protocol.Response.ADMIN_USER_LIST_FAIL.name()
                    + Protocol.SEPARATOR + "Bạn không có quyền quản trị.";
        }

        /*
         * Lấy toàn bộ user từ registry runtime, sau đó ghép thêm trạng thái online
         * từ OnlineUserRegistry thông qua AuctionManager facade.
         */
        final List<User> users = auctionManager.getAllUsers();
        final StringBuilder response = new StringBuilder()
                .append(Protocol.Response.ADMIN_USER_LIST.name())
                .append(Protocol.SEPARATOR)
                .append(users.size());

        // Status online là runtime state từ socket registry, không lưu cố định trong database.
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
        // Chỉ user đã đăng nhập trong session và có role ADMIN mới được đọc danh sách.
        final User currentUser = session.getCurrentUser();
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRoleName());
    }
}
