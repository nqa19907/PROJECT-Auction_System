package auction_system.server.network.command;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.session.ClientSession;
import java.util.List;

/**
 * Command cho phép ADMIN lấy danh sách toàn bộ phiên đấu giá qua socket.
 */
public class AdminListAuctionsCommand implements Command {
    private final AuctionManager auctionManager;

    public AdminListAuctionsCommand(final AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        // 1) Kiểm tra đã đăng nhập chưa
        final User currentUser = session.getCurrentUser();
        if (currentUser == null) {
            return Protocol.Response.ADMIN_AUCTION_LIST_FAIL.name()
                    + Protocol.SEPARATOR + "Bạn chưa đăng nhập.";
        }

        // 2) Kiểm tra có phải ADMIN không
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRoleName())) {
            return Protocol.Response.ADMIN_AUCTION_LIST_FAIL.name()
                    + Protocol.SEPARATOR + "Bạn không có quyền quản trị.";
        }

        // 3) Lấy full tất cả phiên (không lọc trạng thái)
        final List<Auction> auctions = auctionManager.getAllAuctions();

        // 4) Build response
        final StringBuilder response = new StringBuilder();
        response.append(Protocol.Response.ADMIN_AUCTION_LIST.name())
                .append(Protocol.SEPARATOR).append(auctions.size());

        for (Auction auction : auctions) {
            String itemName = auction.getItem() != null
                    ? auction.getItem().getItemName()
                    : "(Không có ten)";
            String seller = auction.getParticipant() != null
                    ? auction.getParticipant().getUsername()
                    : "(Không rõ)";
            String currentPrice = auction.getItem() != null
                    ? String.valueOf(auction.getItem().getCurrentPrice())
                    : "0";
            String status = auction.getStatus() != null
                    ? auction.getStatus().name()
                    : "UNKNOWN";

            response.append(Protocol.RECORD_SEPARATOR)
                    .append(auction.getId()).append(Protocol.SEPARATOR)
                    .append(itemName).append(Protocol.SEPARATOR)
                    .append(seller).append(Protocol.SEPARATOR)
                    .append(currentPrice).append(Protocol.SEPARATOR)
                    .append(status);
        }

        return response.toString();
    }
}