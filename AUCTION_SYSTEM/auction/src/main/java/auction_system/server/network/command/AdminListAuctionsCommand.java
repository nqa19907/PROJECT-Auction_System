package auction_system.server.network.command;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.items.Item;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.session.ClientSession;
import java.util.List;
import java.util.Objects;

/**
 * Command trả danh sách phiên đấu giá cho màn hình quản trị.
 */
public class AdminListAuctionsCommand implements Command {

    private final AuctionManager auctionManager;

    public AdminListAuctionsCommand(final AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        if (!isAdmin(session)) {
            return Protocol.Response.ADMIN_AUCTION_LIST_FAIL.name()
                    + Protocol.SEPARATOR + "Bạn không có quyền quản trị.";
        }

        final List<Auction> auctions = auctionManager.getAllAuctions();
        final StringBuilder response = new StringBuilder()
                .append(Protocol.Response.ADMIN_AUCTION_LIST.name())
                .append(Protocol.SEPARATOR)
                .append(auctions.size());

        for (Auction auction : auctions) {
            response.append(Protocol.RECORD_SEPARATOR)
                    .append(auction.getId())
                    .append(Protocol.SEPARATOR).append(itemName(auction))
                    .append(Protocol.SEPARATOR).append(sellerName(auction))
                    .append(Protocol.SEPARATOR).append(currentPrice(auction))
                    .append(Protocol.SEPARATOR).append(statusName(auction));
        }

        return response.toString();
    }

    private boolean isAdmin(final ClientSession session) {
        final User currentUser = session.getCurrentUser();
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRoleName());
    }

    private String itemName(final Auction auction) {
        final Item item = auction.getItem();
        return item != null ? item.getItemName() : "(Khong co ten)";
    }

    private String sellerName(final Auction auction) {
        return auction.getParticipant() != null
                ? auction.getParticipant().getUsername()
                : "(Khong ro)";
    }

    private double currentPrice(final Auction auction) {
        final BidTransaction highestBid = auction.getCurrentHighestBid();
        if (highestBid != null) {
            return highestBid.getAmount();
        }

        final Item item = auction.getItem();
        return item != null ? item.getCurrentPrice() : 0.0;
    }

    private String statusName(final Auction auction) {
        return auction.getStatus() != null ? auction.getStatus().name() : "UNKNOWN";
    }
}
