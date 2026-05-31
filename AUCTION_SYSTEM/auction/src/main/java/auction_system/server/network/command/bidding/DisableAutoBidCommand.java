package auction_system.server.network.command.bidding;

import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.network.command.JsonPayloadCommand;
import auction_system.server.network.payload.bidding.AuctionIdPayload;
import auction_system.server.services.autobid.AutoBidService;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command nhận yêu cầu tắt đấu giá tự động của user hiện tại.
 */
public final class DisableAutoBidCommand implements JsonPayloadCommand {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DisableAutoBidCommand.class);

    private final AutoBidService autoBidService;

    /**
     * Khởi tạo command tắt auto-bid.
     *
     * @param autoBidService service quản lý cấu hình auto-bid
     */
    public DisableAutoBidCommand(final AutoBidService autoBidService) {
        this.autoBidService = Objects.requireNonNull(autoBidService, "autoBidService");
    }

    @Override
    public String execute(final JsonNode payload, final ClientSession session) {
        try {
            if (session.getCurrentUser() == null) {
                return fail("Bạn cần đăng nhập trước khi tắt auto-bid.");
            }

            final AuctionIdPayload auctionIdPayload;
            try {
                auctionIdPayload = JsonProtocol.payloadAs(payload, AuctionIdPayload.class);
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("Không map được payload tắt auto-bid: {}", exception.getMessage());
                return fail("Thiếu mã phiên đấu giá.");
            }

            if (auctionIdPayload.hasMissingAuctionId()) {
                return fail("Thiếu mã phiên đấu giá.");
            }

            final String auctionId = auctionIdPayload.auctionId();
            autoBidService.disableAutoBid(auctionId, session.getCurrentUser());

            LOGGER.info(
                    "Đã tắt auto-bid. user={}, auctionId={}",
                    session.getCurrentUser().getUsername(),
                    auctionId);

            return success(auctionId);
        } catch (IllegalArgumentException exception) {
            return fail(exception.getMessage());
        } catch (Exception exception) {
            LOGGER.error("Lỗi khi xử lý yêu cầu tắt auto-bid.", exception);
            return fail("Không thể tắt đấu giá tự động.");
        }
    }

    private String fail(final String message) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.AUTO_BID_FAIL.name(),
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi tắt auto-bid: {}", exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON AUTO_BID_FAIL.", exception);
        }
    }

    private String success(final String auctionId) {
        final String message = "Đã tắt đấu giá tự động.";
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.AUTO_BID_OK.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of("auctionId", auctionId)),
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response tắt auto-bid: {}",
                    exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON AUTO_BID_OK.", exception);
        }
    }
}
