package auction_system.server.network.command.bidding;

import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.network.command.Command;
import auction_system.server.services.bidding.AuctionBidService;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
                return buildErrorResponse("Thiếu auctionId");
            }

            final String auctionId = parts[IDX_AUCTION_ID];

            final List<BidTransaction> bids = auctionBidService.getBidHistory(auctionId);

            return buildSuccessResponse(auctionId, bids);
        } catch (Exception e) {
            LOGGER.error("Lỗi khi lấy lịch sử bid.", e);
            return buildErrorResponse("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
        }
    }

    private String buildSuccessResponse(
            final String auctionId,
            final List<BidTransaction> bids) {
        final List<List<String>> bidRecords = new ArrayList<>();
        for (BidTransaction bid : bids) {
            bidRecords.add(toBidRecord(bid));
        }

        // Trả lịch sử bid bằng JSON cho bảng lịch sử trong màn chi tiết.
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.BID_HISTORY.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of(
                                    "auctionId", auctionId,
                                    "count", bidRecords.size(),
                                    "bids", bidRecords)),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response lịch sử bid: {}",
                    exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON BID_HISTORY.", exception);
        }
    }

    private List<String> toBidRecord(final BidTransaction bid) {
        // Giữ mỗi dòng bid theo thứ tự time/bidder/amount mà ViewModel đang dùng.
        return List.of(
                TIME_FORMATTER.format(bid.getTimestamp()),
                String.valueOf(bid.getParticipant().getUsername()),
                String.valueOf(bid.getAmount()));
    }

    private String buildErrorResponse(final String message) {
        // Trả lỗi dạng JSON để client route theo type ERROR.
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.ERROR.name(),
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi lấy lịch sử bid: {}",
                    exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON ERROR.", exception);
        }
    }
}
