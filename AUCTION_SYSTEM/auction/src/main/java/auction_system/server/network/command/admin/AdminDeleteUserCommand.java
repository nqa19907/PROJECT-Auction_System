package auction_system.server.network.command.admin;

import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;

/**
 * Command cho phép ADMIN xóa người dùng (trừ ADMIN và trừ chính mình).
 */
public class AdminDeleteUserCommand implements Command {

    private static final int IDX_USER_ID = 1;
    private final AuctionManager auctionManager;

    public AdminDeleteUserCommand(final AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        User currentUser = session.getCurrentUser();
        if (currentUser == null) {
            return Protocol.Response.ADMIN_DELETE_USER_FAIL.name()
                    + Protocol.SEPARATOR + "Bạn chưa đăng nhập.";
        }

        if (!"ADMIN".equalsIgnoreCase(currentUser.getRoleName())) {
            return Protocol.Response.ADMIN_DELETE_USER_FAIL.name()
                    + Protocol.SEPARATOR + "Bạn không có quyền quản trị.";
        }

        if (parts.length <= IDX_USER_ID || parts[IDX_USER_ID].isBlank()) {
            return Protocol.Response.ADMIN_DELETE_USER_FAIL.name()
                    + Protocol.SEPARATOR + "Thiếu mã người dùng.";
        }

        String targetUserId = parts[IDX_USER_ID].trim();

        // Chặn admin tự xóa chính mình
        if (targetUserId.equals(currentUser.getId())) {
            return Protocol.Response.ADMIN_DELETE_USER_FAIL.name()
                    + Protocol.SEPARATOR + "Không thể xóa chính tài khoản ADMIN đang đăng nhập.";
        }

        User targetUser = auctionManager.findUserById(targetUserId);
        if (targetUser == null) {
            return Protocol.Response.ADMIN_DELETE_USER_FAIL.name()
                    + Protocol.SEPARATOR + "Không tìm thấy người dùng.";
        }

        // Chặn xóa mọi tài khoản ADMIN
        if ("ADMIN".equalsIgnoreCase(targetUser.getRoleName())) {
            return Protocol.Response.ADMIN_DELETE_USER_FAIL.name()
                    + Protocol.SEPARATOR + "Không được phép xóa tài khoản ADMIN.";
        }

        boolean deleted = auctionManager.deleteUser(targetUserId);
        if (!deleted) {
            return Protocol.Response.ADMIN_DELETE_USER_FAIL.name()
                    + Protocol.SEPARATOR + "Xóa người dùng thất bại.";
        }

        return Protocol.Response.ADMIN_DELETE_USER_OK.name()
                + Protocol.SEPARATOR + targetUserId;
    }
}