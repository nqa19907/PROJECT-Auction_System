package auction_system.server.network.command.bidding;

import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Command cho phep user chinh sua thong tin phien do chinh minh dang.
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
                    + Protocol.SEPARATOR + "Chua dang nhap.";
        }
        if (parts.length < REQUIRED_PART_COUNT) {
            return Protocol.Response.UPDATE_MY_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Thieu du lieu cap nhat phien.";
        }

        try {
            final boolean updated = auctionManager.updateMyAuctionInfo(
                    required(parts[AUCTION_ID_INDEX], "Thieu ma phien."),
                    session.getCurrentUser().getId(),
                    required(parts[CATEGORY_INDEX], "Thieu danh muc."),
                    required(parts[ITEM_NAME_INDEX], "Thieu ten tai san."),
                    required(parts[DESCRIPTION_INDEX], "Thieu mo ta."),
                    required(parts[CONDITION_INDEX], "Thieu tinh trang."),
                    // endTime client gui len theo dinh dang ISO-8601.
                    LocalDateTime.parse(
                            required(parts[END_TIME_INDEX], "Thieu thoi gian ket thuc."))
            );

            if (!updated) {
                return Protocol.Response.UPDATE_MY_AUCTION_FAIL.name()
                        + Protocol.SEPARATOR + "Khong tim thay phien dau gia.";
            }

            return Protocol.Response.UPDATE_MY_AUCTION_OK.name()
                    + Protocol.SEPARATOR + "Cap nhat phien thanh cong.";
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
