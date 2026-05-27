package auction_system.server.network.command;

import auction_system.common.network.Protocol;
import auction_system.server.services.AutoBidService;
import auction_system.server.session.ClientSession;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command nhận yêu cầu tắt đấu giá tự động của user hiện tại.
 */
public final class DisableAutoBidCommand implements Command {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DisableAutoBidCommand.class);

    private static final int MIN_DISABLE_AUTO_BID_PARTS = 2;
    private static final int IDX_AUCTION_ID = 1;

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
    public String execute(final String[] parts, final ClientSession session) {
        try {
            if (session.getCurrentUser() == null) {
                return fail("Bạn cần đăng nhập trước khi tắt auto-bid.");
            }

            if (parts.length < MIN_DISABLE_AUTO_BID_PARTS) {
                return fail("Thiếu mã phiên đấu giá.");
            }

            final String auctionId = parts[IDX_AUCTION_ID];
            autoBidService.disableAutoBid(auctionId, session.getCurrentUser());

            LOGGER.info(
                    "Đã tắt auto-bid. user={}, auctionId={}",
                    session.getCurrentUser().getUsername(),
                    auctionId);

            return Protocol.Response.AUTO_BID_OK.name()
                    + Protocol.SEPARATOR
                    + "Đã tắt đấu giá tự động.";
        } catch (IllegalArgumentException exception) {
            return fail(exception.getMessage());
        } catch (Exception exception) {
            LOGGER.error("Lỗi khi xử lý yêu cầu tắt auto-bid.", exception);
            return fail("Không thể tắt đấu giá tự động.");
        }
    }

    private String fail(final String message) {
        return Protocol.Response.AUTO_BID_FAIL.name()
                + Protocol.SEPARATOR
                + message;
    }
}
