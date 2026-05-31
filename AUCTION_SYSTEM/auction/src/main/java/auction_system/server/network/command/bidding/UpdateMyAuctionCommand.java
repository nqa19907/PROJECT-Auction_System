package auction_system.server.network.command.bidding;

import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Command cho phép user chỉnh sửa thông tin phiên do chính mình đăng.
 */
public final class UpdateMyAuctionCommand implements Command {

    private static final int REQUIRED_PART_COUNT = 11;
    private static final int AUCTION_ID_INDEX = 1;
    private static final int CATEGORY_INDEX = 2;
    private static final int ITEM_NAME_INDEX = 3;
    private static final int DESCRIPTION_INDEX = 4;
    private static final int START_PRICE_INDEX = 5;
    private static final int BID_STEP_INDEX = 6;
    private static final int IMAGE_PATH_INDEX = 7;
    private static final int CONDITION_INDEX = 8;
    private static final int START_TIME_INDEX = 9;
    private static final int END_TIME_INDEX = 10;
    private final AuctionManager auctionManager;

    public UpdateMyAuctionCommand(final AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        // Kiểm tra session và số lượng field trước khi đọc payload đã chuyển thành parts.
        if (session == null || session.getCurrentUser() == null) {
            return failure("Chưa đăng nhập.");
        }
        if (parts.length < REQUIRED_PART_COUNT) {
            return failure("Thiếu dữ liệu cập nhật phiên.");
        }

        try {
            // Validate từng field và chỉ cập nhật auction thuộc user hiện tại.
            final boolean updated = auctionManager.updateMyAuctionInfo(
                    required(parts[AUCTION_ID_INDEX], "Thiếu mã phiên."),
                    session.getCurrentUser().getId(),
                    required(parts[CATEGORY_INDEX], "Thiếu danh mục."),
                    required(parts[ITEM_NAME_INDEX], "Thiếu tên tài sản."),
                    required(parts[DESCRIPTION_INDEX], "Thiếu mô tả."),
                    required(parts[CONDITION_INDEX], "Thiếu tình trạng."),
                    parsePositiveMoney(parts[START_PRICE_INDEX], "giá khởi điểm"),
                    parsePositiveMoney(parts[BID_STEP_INDEX], "giá tối thiểu"),
                    optional(parts[IMAGE_PATH_INDEX]),
                    LocalDateTime.parse(required(parts[START_TIME_INDEX],
                            "Thiếu thời gian bắt đầu.")),
                    LocalDateTime.parse(required(parts[END_TIME_INDEX],
                            "Thiếu thời gian kết thúc.")));

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

    private double parsePositiveMoney(final String value, final String fieldName) {
        final double amount = Double.parseDouble(required(value, "Thiếu " + fieldName + "."));
        if (amount <= 0) {
            throw new IllegalArgumentException("Trường " + fieldName + " phải lớn hơn 0.");
        }
        return amount;
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
