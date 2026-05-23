package auction_system.server.network.command;

import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.session.ClientSession;

/**
 * Command cho phép ADMIN dừng (cancel) một phiên đấu giá.
 */
public class AdminCancelAuctionCommand implements Command {

    private static final int IDX_AUCTION_ID = 1;
    private final AuctionManager auctionManager;

    public AdminCancelAuctionCommand(final AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        // 1) Kiểm tra đăng nhập
        User currentUser = session.getCurrentUser();
        if (currentUser == null) {
            return Protocol.Response.ADMIN_CANCEL_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Bạn chưa đăng nhập.";
        }

        // 2) Kiểm tra quyền ADMIN
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRoleName())) {
            return Protocol.Response.ADMIN_CANCEL_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Bạn không có quyền quản trị.";
        }

        // 3) Kiểm tra tham số
        if (parts.length <= IDX_AUCTION_ID || parts[IDX_AUCTION_ID].isBlank()) {
            return Protocol.Response.ADMIN_CANCEL_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Thiếu mã phiên đấu giá.";
        }

        String auctionId = parts[IDX_AUCTION_ID];

        // 4) Dừng phiên
        boolean canceled = auctionManager.cancelAuction(auctionId);
        if (!canceled) {
            return Protocol.Response.ADMIN_CANCEL_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Không tìm thấy phiên đấu giá.";
        }

        return Protocol.Response.ADMIN_CANCEL_AUCTION_OK.name()
                + Protocol.SEPARATOR + auctionId;
    }
}