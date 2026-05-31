package auction_system.server.network.command.admin;

import auction_system.common.models.users.User;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.JsonPayloadCommand;
import auction_system.server.network.payload.UserIdPayload;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command cho phép ADMIN xóa người dùng (trừ ADMIN và trừ chính mình).
 */
public class AdminDeleteUserCommand implements JsonPayloadCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminDeleteUserCommand.class);

    private final AuctionManager auctionManager;

    public AdminDeleteUserCommand(final AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public String execute(final JsonNode payload, final ClientSession session) {
        // Quyền admin luôn lấy từ session server, không tin dữ liệu client gửi lên.
        User currentUser = session.getCurrentUser();
        if (currentUser == null) {
            return buildFailureResponse("Bạn chưa đăng nhập.");
        }

        if (!"ADMIN".equalsIgnoreCase(currentUser.getRoleName())) {
            return buildFailureResponse("Bạn không có quyền quản trị.");
        }

        // Payload chỉ chứa userId cần xóa; map bằng DTO để tránh phụ thuộc thứ tự tham số.
        final UserIdPayload userIdPayload;
        try {
            userIdPayload = JsonProtocol.payloadAs(payload, UserIdPayload.class);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Không map được payload xóa user admin: {}",
                    exception.getMessage());
            return buildFailureResponse("Thiếu mã người dùng.");
        }

        if (userIdPayload.hasMissingUserId()) {
            return buildFailureResponse("Thiếu mã người dùng.");
        }

        String targetUserId = userIdPayload.userId().trim();

        // Chặn admin tự xóa chính mình.
        if (targetUserId.equals(currentUser.getId())) {
            return buildFailureResponse("Không thể xóa chính tài khoản ADMIN đang đăng nhập.");
        }

        User targetUser = auctionManager.findUserById(targetUserId);
        if (targetUser == null) {
            return buildFailureResponse("Không tìm thấy người dùng.");
        }

        // Chặn xóa mọi tài khoản ADMIN.
        if ("ADMIN".equalsIgnoreCase(targetUser.getRoleName())) {
            return buildFailureResponse("Không được phép xóa tài khoản ADMIN.");
        }

        boolean deleted = auctionManager.deleteUser(targetUserId);
        if (!deleted) {
            return buildFailureResponse("Xóa người dùng thất bại.");
        }

        return buildSuccessResponse(targetUserId);
    }

    private String buildSuccessResponse(final String userId) {
        // OK response JSON mang id đã xóa để client xóa đúng dòng user.
        return buildJsonResponse(
                Protocol.Response.ADMIN_DELETE_USER_OK,
                "OK",
                JsonProtocol.payloadOf(Map.of("id", userId, "userId", userId)),
                "Xóa người dùng thành công.");
    }

    private String buildFailureResponse(final String message) {
        // FAIL response JSON mang message để dashboard hiển thị lỗi rõ ràng.
        return buildJsonResponse(
                Protocol.Response.ADMIN_DELETE_USER_FAIL,
                "FAIL",
                null,
                message);
    }

    private String buildJsonResponse(
            final Protocol.Response type,
            final String status,
            final com.fasterxml.jackson.databind.JsonNode payload,
            final String message) {
        // Dùng một helper chung để đóng gói response xóa user bằng JSON.
        try {
            return JsonProtocol.stringify(new JsonMessage(
                    type.name(),
                    null,
                    status,
                    payload,
                    message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response xóa user admin: {}",
                    exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON " + type.name(), exception);
        }
    }
}
