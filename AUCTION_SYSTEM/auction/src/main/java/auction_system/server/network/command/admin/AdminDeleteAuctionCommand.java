package auction_system.server.network.command.admin;


import auction_system.common.models.auctions.Auction;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;

/**
 * Command cho phép ADMIN xóa hẳn một phiên đấu giá.
 */
public class AdminDeleteAuctionCommand implements Command {

    private static final int IDX_AUCTION_ID = 1;
    private final AuctionManager auctionManager;

    public AdminDeleteAuctionCommand(final AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        User currentUser = session.getCurrentUser();
        if (currentUser == null) {
            return Protocol.Response.ADMIN_DELETE_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Bạn chưa đăng nhập.";
        }

        if (!"ADMIN".equalsIgnoreCase(currentUser.getRoleName())) {
            return Protocol.Response.ADMIN_DELETE_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Bạn không có quyền quản trị.";
        }

        if (parts.length <= IDX_AUCTION_ID || parts[IDX_AUCTION_ID].isBlank()) {
            return Protocol.Response.ADMIN_DELETE_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Thiếu mã phiên đấu giá.";
        }

        String auctionId = parts[IDX_AUCTION_ID].trim();
        boolean deleted = auctionManager.deleteAuction(auctionId);

        if (!deleted) {
            return Protocol.Response.ADMIN_DELETE_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Không tìm thấy phiên để xóa.";
        }

        return Protocol.Response.ADMIN_DELETE_AUCTION_OK.name()
                + Protocol.SEPARATOR + auctionId;
    }
}