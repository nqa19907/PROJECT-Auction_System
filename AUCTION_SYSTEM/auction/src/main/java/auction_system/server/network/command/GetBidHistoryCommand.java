package auction_system.server.network.command;

import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.network.Protocol;
import auction_system.server.services.AuctionBidService;
import auction_system.server.session.ClientSession;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý các lệnh lấy lịch sử đặt giá của một phiên đấu giá.
 */
public class GetBidHistoryCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetBidHistoryCommand.class);
    private static final int MIN_PARTS = 2;
    private static final int IDX_AUCTION_ID = 1;
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AuctionBidService auctionBidService;

    /**
     * Khởi tạo command lấy lịch sử bid.
     *
     * @param auctionBidService service xử lý nghiệp vụ bid
     */
    public GetBidHistoryCommand(final AuctionBidService auctionBidService) {
        this.auctionBidService =
            Objects.requireNonNull(auctionBidService, "auctionBidService");
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        try {
            if (parts.length < MIN_PARTS || parts[IDX_AUCTION_ID].isBlank()) {
                return Protocol.Response.ERROR.name()
                        + Protocol.SEPARATOR
                        + "Thiếu auctionId";
            }

            final String auctionId = parts[IDX_AUCTION_ID];

            final List<BidTransaction> bids = auctionBidService.getBidHistory(auctionId);

            final StringBuilder response = new StringBuilder();
            response.append(Protocol.Response.BID_HISTORY.name())
                    .append(Protocol.SEPARATOR)
                    .append(auctionId)
                    .append(Protocol.SEPARATOR)
                    .append(bids.size());

            for (BidTransaction bid : bids) {
                response.append(Protocol.RECORD_SEPARATOR)
                        .append(TIME_FORMATTER.format(bid.getTimestamp()))
                        .append(Protocol.SEPARATOR)
                        .append(bid.getParticipant().getUsername())
                        .append(Protocol.SEPARATOR)
                        .append(bid.getAmount());
            }

            return response.toString();
        } catch (Exception e) {
            LOGGER.error("Lỗi khi lấy lịch sử bid.", e);
            return Protocol.Response.ERROR.name()
                    + Protocol.SEPARATOR
                    + "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.";
        }
    }
}
