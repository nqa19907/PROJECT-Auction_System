package auction_system.server.network.command.bidding;

import auction_system.common.models.auctions.Auction;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;
import java.util.List;
import java.util.Objects;

/**
 * Command trả về danh sách phiên đấu giá do user hiện tại đăng bán.
 */
public final class ListMyAuctionsCommand implements Command {

    private static final String CONDITION_PREFIX = "\nTình trạng: ";
    private final AuctionManager auctionManager;

    public ListMyAuctionsCommand(final AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        if (session == null || session.getCurrentUser() == null) {
            return Protocol.Response.ERROR.name() + Protocol.SEPARATOR + "Chưa đăng nhập.";
        }

        final String currentUserId = session.getCurrentUser().getId();
        final List<Auction> myAuctions = auctionManager.getAllAuctions().stream()
                .filter(auction -> auction.getParticipant() != null)
                .filter(auction -> currentUserId.equals(auction.getParticipant().getId()))
                .toList();

        final StringBuilder response = new StringBuilder();
        response.append(Protocol.Response.MY_AUCTION_LIST.name())
                .append(Protocol.SEPARATOR)
                .append(myAuctions.size());

        for (Auction auction : myAuctions) {
            final double currentPrice = (auction.getCurrentHighestBid() != null)
                    ? auction.getCurrentHighestBid().getAmount()
                    : auction.getItem().getCurrentPrice();

            final String rawDescription = safe(auction.getItem().getDescription());
            final String[] extracted = extractDescriptionAndCondition(rawDescription);
            final String category = safe(auction.getItem().getCategory());

            response.append(Protocol.RECORD_SEPARATOR)
                    .append(auction.getId()).append(Protocol.SEPARATOR)
                    .append(safe(auction.getItem().getItemName())).append(Protocol.SEPARATOR)
                    .append(currentPrice).append(Protocol.SEPARATOR)
                    .append(auction.getStatus()).append(Protocol.SEPARATOR)
                    .append(auction.getEndTime()).append(Protocol.SEPARATOR)
                    .append(category).append(Protocol.SEPARATOR)
                    .append(extracted[0]).append(Protocol.SEPARATOR)
                    .append(extracted[1]);
        }
        return response.toString();
    }

    /**
     * Tách mô tả thuần và tình trạng từ format đang lưu ở server.
     *
     * @param fullDescription mô tả đang lưu trong item
     * @return mảng gồm mô tả thuần và tình trạng
     */
    private String[] extractDescriptionAndCondition(final String fullDescription) {
        final int index = fullDescription.lastIndexOf(CONDITION_PREFIX);
        if (index < 0) {
            return new String[]{fullDescription, ""};
        }
        final String description = fullDescription.substring(0, index);
        final String condition = fullDescription.substring(index + CONDITION_PREFIX.length());
        return new String[]{description, condition};
    }

    /**
     * Làm sạch dữ liệu để tránh phá vỡ protocol khi tách chuỗi.
     *
     * @param value chuỗi đầu vào
     * @return chuỗi đã làm sạch
     */
    private String safe(final String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace(Protocol.SEPARATOR, " ")
                .replace(Protocol.RECORD_SEPARATOR, " ")
                .trim();
    }
}
