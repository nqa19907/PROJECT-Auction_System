package auction_system.server.network.command.admin;


import auction_system.common.models.auctions.Auction;
import auction_system.common.models.users.User;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command cho phép ADMIN xóa hẳn một phiên đấu giá.
 */
public class AdminDeleteAuctionCommand implements JsonPayloadCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminDeleteAuctionCommand.class);

    private final AuctionManager auctionManager;

    public AdminDeleteAuctionCommand(final AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public String execute(final JsonNode payload, final ClientSession session) {
        User currentUser = session.getCurrentUser();
        if (currentUser == null) {
            return buildFailureResponse("Bạn chưa đăng nhập.");
        }

        if (!"ADMIN".equalsIgnoreCase(currentUser.getRoleName())) {
            return buildFailureResponse("Bạn không có quyền quản trị.");
        }

        final AuctionIdPayload auctionIdPayload;
        try {
            auctionIdPayload = JsonProtocol.payloadAs(payload, AuctionIdPayload.class);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Không map được payload xóa phiên admin: {}",
                    exception.getMessage());
            return buildFailureResponse("Thiếu mã phiên đấu giá.");
        }

        if (auctionIdPayload.hasMissingAuctionId()) {
            return buildFailureResponse("Thiếu mã phiên đấu giá.");
        }

        String auctionId = auctionIdPayload.auctionId().trim();
        boolean deleted = auctionManager.deleteAuction(auctionId);

        if (!deleted) {
            return buildFailureResponse("Không tìm thấy phiên để xóa.");
        }

        return buildSuccessResponse(auctionId);
    }

    private String buildSuccessResponse(final String auctionId) {
        // OK response JSON mang id đã xóa để client xóa đúng dòng phiên.
        return buildJsonResponse(
                Protocol.Response.ADMIN_DELETE_AUCTION_OK,
                "OK",
                JsonProtocol.payloadOf(Map.of("id", auctionId, "auctionId", auctionId)),
                "Xóa phiên đấu giá thành công.");
    }

    private String buildFailureResponse(final String message) {
        // FAIL response JSON mang message để dashboard hiển thị lỗi rõ ràng.
        return buildJsonResponse(
                Protocol.Response.ADMIN_DELETE_AUCTION_FAIL,
                "FAIL",
                null,
                message);
    }

    private String buildJsonResponse(
            final Protocol.Response type,
            final String status,
            final com.fasterxml.jackson.databind.JsonNode payload,
            final String message) {
        // Dùng một helper chung để đóng gói response xóa phiên bằng JSON.
        try {
            return JsonProtocol.stringify(new JsonMessage(
                    type.name(),
                    null,
                    status,
                    payload,
                    message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response xóa phiên admin: {}",
                    exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON " + type.name(), exception);
        }
    }
}
