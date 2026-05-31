package auction_system.server.network.command.auction;

import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.JsonPayloadCommand;
import auction_system.server.network.payload.AuctionIdPayload;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý lệnh rời khỏi một phiên đấu giá mà client đang theo dõi.
 */
public class LeaveAuctionCommand implements JsonPayloadCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaveAuctionCommand.class);
    private final AuctionManager auctionManager;

    public LeaveAuctionCommand(AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    /**
     * Thực thi lệnh rời phiên đấu giá.
     *
     * <p>Nhận request JSON {@code LEAVE_AUCTION} và trả JSON {@code LEAVE_OK} hoặc {@code ERROR}.
     *
     * @param payload Payload JSON của request.
     * @param session Phiên làm việc của Client.
     * @return Chuỗi phản hồi cho client.
     */
    @Override
    public String execute(final JsonNode payload, final ClientSession session) {
        try {
            final String auctionId = readAuctionId(payload);
            if (auctionId.isBlank()) {
                return buildErrorResponse("Thiếu auctionId");
            }

            session.leaveAuction(auctionId);

            return buildSuccessResponse(auctionId);
        } catch (Exception e) {
            String username = session.isLoggedIn() 
                    ? session.getCurrentUser().getUsername() : "guest";
            LOGGER.error("Lỗi hệ thống khi xử lý lệnh rời phiên đấu giá cho "
                    + username, e);
            return buildErrorResponse("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
        }
    }

    private String readAuctionId(final JsonNode payload) {
        try {
            final AuctionIdPayload auctionIdPayload =
                    JsonProtocol.payloadAs(payload, AuctionIdPayload.class);
            if (auctionIdPayload.hasMissingAuctionId()) {
                return "";
            }
            return auctionIdPayload.auctionId();
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Không map được payload rời phiên: {}", exception.getMessage());
            return "";
        }
    }

    private String buildSuccessResponse(final String auctionId) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.LEAVE_OK.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of("auctionId", auctionId)),
                            "Rời phiên đấu giá thành công."));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response rời phiên: {}", exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON LEAVE_OK.", exception);
        }
    }

    private String buildErrorResponse(final String message) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.ERROR.name(),
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi rời phiên: {}", exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON ERROR.", exception);
        }
    }
}
