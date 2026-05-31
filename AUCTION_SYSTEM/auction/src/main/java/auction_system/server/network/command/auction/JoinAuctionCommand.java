package auction_system.server.network.command.auction;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
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
 * Xử lý lệnh tham gia một phiên đấu giá.
 */
public class JoinAuctionCommand implements JsonPayloadCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(JoinAuctionCommand.class);
    private final AuctionManager auctionManager;

    public JoinAuctionCommand(AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    /**
     * Thực thi lệnh tham gia phiên đấu giá.
     *
     * <p>Nhận request JSON {@code JOIN_AUCTION} và trả JSON {@code JOIN_OK} hoặc {@code JOIN_FAIL}.
     *
     * @param payload Payload JSON của request.
     * @param session Phiên làm việc của Client.
     * @return Chuỗi phản hồi cho client.
     */
    @Override
    public String execute(final JsonNode payload, final ClientSession session) {
        try {
            if (!session.isLoggedIn()) {
                return buildErrorResponse("Bạn cần đăng nhập trước");
            }
            final String auctionId = readAuctionId(payload);
            if (auctionId.isBlank()) {
                return buildFailResponse("Thiếu auctionId");
            }

            Auction auction = auctionManager.getAuctionById(auctionId);

            if (auction == null) {
                return buildFailResponse("Không tìm thấy phiên đấu giá");
            }

            // Không cho phép theo dõi nếu phiên đấu giá đã kết thúc hoặc bị huỷ
            if (auction.getStatus() == AuctionStatus.FINISHED
                    || auction.getStatus() == AuctionStatus.CANCELED) {
                return buildFailResponse("Phiên đấu giá đã kết thúc hoặc bị huỷ");
            }

            session.joinAuction(auctionId);

            LOGGER.info(session.getCurrentUser().getUsername() + " tham gia phiên: " + auctionId);
            return buildSuccessResponse(auctionId);
        } catch (Exception e) {
            String username = session.isLoggedIn() 
                    ? session.getCurrentUser().getUsername() : "guest";
            LOGGER.error("Lỗi hệ thống khi xử lý lệnh tham gia phiên đấu giá cho "
                    + username, e);
            return buildFailResponse("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
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
            LOGGER.warn("Không map được payload tham gia phiên: {}", exception.getMessage());
            return "";
        }
    }

    private String buildSuccessResponse(final String auctionId) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.JOIN_OK.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of("auctionId", auctionId)),
                            "Tham gia phiên đấu giá thành công."));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response tham gia phiên: {}",
                    exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON JOIN_OK.", exception);
        }
    }

    private String buildFailResponse(final String message) {
        return buildFailureLikeResponse(Protocol.Response.JOIN_FAIL.name(), message);
    }

    private String buildErrorResponse(final String message) {
        return buildFailureLikeResponse(Protocol.Response.ERROR.name(), message);
    }

    private String buildFailureLikeResponse(final String type, final String message) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            type,
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi tham gia phiên: {}", exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON JOIN_FAIL.", exception);
        }
    }
}
