package auction_system.server.network.command.admin;

import auction_system.common.models.users.User;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command cho phép ADMIN dừng (cancel) một phiên đấu giá.
 */
public class AdminCancelAuctionCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminCancelAuctionCommand.class);

    private static final int IDX_AUCTION_ID = 1;
    private final AuctionManager auctionManager;

    public AdminCancelAuctionCommand(final AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        // 1) Kiểm tra đăng nhập
        User currentUser = session.getCurrentUser();
        if (currentUser == null) {
            return buildFailureResponse("Bạn chưa đăng nhập.");
        }

        // 2) Kiểm tra quyền ADMIN
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRoleName())) {
            return buildFailureResponse("Bạn không có quyền quản trị.");
        }

        // 3) Kiểm tra tham số
        if (parts.length <= IDX_AUCTION_ID || parts[IDX_AUCTION_ID].isBlank()) {
            return buildFailureResponse("Thiếu mã phiên đấu giá.");
        }

        String auctionId = parts[IDX_AUCTION_ID];

        // 4) Dừng phiên
        boolean canceled = auctionManager.cancelAuction(auctionId);
        if (!canceled) {
            return buildFailureResponse("Không tìm thấy phiên đấu giá.");
        }

        return buildSuccessResponse(auctionId);
    }

    private String buildSuccessResponse(final String auctionId) {
        // OK response JSON mang id phiên đã dừng để client có thể đồng bộ UI.
        return buildJsonResponse(
                Protocol.Response.ADMIN_CANCEL_AUCTION_OK,
                "OK",
                JsonProtocol.payloadOf(Map.of("id", auctionId, "auctionId", auctionId)),
                "Dừng phiên đấu giá thành công.");
    }

    private String buildFailureResponse(final String message) {
        // FAIL response JSON mang message để dashboard hiển thị lỗi rõ ràng.
        return buildJsonResponse(
                Protocol.Response.ADMIN_CANCEL_AUCTION_FAIL,
                "FAIL",
                null,
                message);
    }

    private String buildJsonResponse(
            final Protocol.Response type,
            final String status,
            final com.fasterxml.jackson.databind.JsonNode payload,
            final String message) {
        // Dùng một helper chung để đóng gói response dừng phiên bằng JSON.
        try {
            return JsonProtocol.stringify(new JsonMessage(
                    type.name(),
                    null,
                    status,
                    payload,
                    message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response dừng phiên admin: {}",
                    exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON " + type.name(), exception);
        }
    }
}
