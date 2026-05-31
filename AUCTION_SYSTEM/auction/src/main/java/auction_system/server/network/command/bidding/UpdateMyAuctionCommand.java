package auction_system.server.network.command.bidding;

import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.JsonPayloadCommand;
import auction_system.server.network.payload.bidding.UpdateMyAuctionPayload;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Command cho phép user chỉnh sửa thông tin phiên do chính mình đăng.
 */
public final class UpdateMyAuctionCommand implements JsonPayloadCommand {

    private final AuctionManager auctionManager;

    public UpdateMyAuctionCommand(final AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    @Override
    public String execute(final JsonNode payload, final ClientSession session) {
        // Kiểm tra session và số lượng field trước khi đọc payload đã chuyển thành parts.
        if (session == null || session.getCurrentUser() == null) {
            return failure("Chưa đăng nhập.");
        }

        final UpdateMyAuctionPayload updatePayload;
        try {
            updatePayload = JsonProtocol.payloadAs(payload, UpdateMyAuctionPayload.class);
        } catch (IllegalArgumentException exception) {
            return failure("Thiếu dữ liệu cập nhật phiên.");
        }

        if (updatePayload.hasMissingRequiredFields()) {
            return failure("Thiếu dữ liệu cập nhật phiên.");
        }

        try {
            // Validate từng field và chỉ cập nhật auction thuộc user hiện tại.
            final boolean updated = auctionManager.updateMyAuctionInfo(
                    required(updatePayload.auctionId(), "Thiếu mã phiên."),
                    session.getCurrentUser().getId(),
                    required(updatePayload.category(), "Thiếu danh mục."),
                    required(updatePayload.itemName(), "Thiếu tên tài sản."),
                    required(updatePayload.description(), "Thiếu mô tả."),
                    required(updatePayload.condition(), "Thiếu tình trạng."),
                    LocalDateTime.parse(required(updatePayload.endTime(),
                            "Thiếu thời gian kết thúc.")),
                    optional(updatePayload.imagePath()));

            // Phân biệt cập nhật thành công với trường hợp auction không còn tồn tại.
            return updated
                    ? response(Protocol.Response.UPDATE_MY_AUCTION_OK, "OK",
                            "Cập nhật phiên thành công.")
                    : failure("Không tìm thấy phiên đấu giá.");
        } catch (RuntimeException exception) {
            return failure(exception.getMessage());
        }
    }

    private String required(final String value, final String message) {
        // Chuẩn hóa chuỗi bắt buộc trước khi chuyển dữ liệu xuống core.
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String optional(final String value) {
        return value == null ? "" : value.trim();
    }

    private String failure(final String message) {
        // Chuẩn hóa mọi lỗi nghiệp vụ thành UPDATE_MY_AUCTION_FAIL.
        return response(Protocol.Response.UPDATE_MY_AUCTION_FAIL, "FAIL", message);
    }

    private String response(
            final Protocol.Response type,
            final String status,
            final String message) {
        // Response update không cần payload, chỉ cần type, status và message.
        return JsonProtocol.stringifyRequired(
                new JsonMessage(type.name(), null, status, null, message));
    }
}
