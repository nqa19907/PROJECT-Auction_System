package auction_system.server.network.command.bidding;

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

    private static final int REQUIRED_PART_COUNT = 7;
    private static final int AUCTION_ID_INDEX = 1;
    private static final int CATEGORY_INDEX = 2;
    private static final int ITEM_NAME_INDEX = 3;
    private static final int DESCRIPTION_INDEX = 4;
    private static final int CONDITION_INDEX = 5;
    private static final int END_TIME_INDEX = 6;

    private final AuctionManager auctionManager;

    public UpdateMyAuctionCommand(final AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        if (session == null || session.getCurrentUser() == null) {
            return Protocol.Response.UPDATE_MY_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Chưa đăng nhập.";
        }
        if (parts.length < REQUIRED_PART_COUNT) {
            return Protocol.Response.UPDATE_MY_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Thiếu dữ liệu cập nhật phiên.";
        }

        try {
            final boolean updated = auctionManager.updateMyAuctionInfo(
                    required(parts[AUCTION_ID_INDEX], "Thiếu mã phiên."),
                    session.getCurrentUser().getId(),
                    required(parts[CATEGORY_INDEX], "Thiếu danh mục."),
                    required(parts[ITEM_NAME_INDEX], "Thiếu tên tài sản."),
                    required(parts[DESCRIPTION_INDEX], "Thiếu mô tả."),
                    required(parts[CONDITION_INDEX], "Thiếu tình trạng."),
                    // endTime client gửi lên theo định dạng ISO-8601.
                    LocalDateTime.parse(
                            required(parts[END_TIME_INDEX], "Thiếu thời gian kết thúc."))
            );

            if (!updated) {
                return Protocol.Response.UPDATE_MY_AUCTION_FAIL.name()
                        + Protocol.SEPARATOR + "Không tìm thấy phiên đấu giá.";
            }

            return Protocol.Response.UPDATE_MY_AUCTION_OK.name()
                    + Protocol.SEPARATOR + "Cập nhật phiên thành công.";
        } catch (RuntimeException exception) {
            return Protocol.Response.UPDATE_MY_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + exception.getMessage();
        }
    }

    private String required(final String value, final String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
