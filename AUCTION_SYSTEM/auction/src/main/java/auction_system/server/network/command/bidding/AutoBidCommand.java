package auction_system.server.network.command.bidding;

import auction_system.common.network.Protocol;
import auction_system.server.network.command.Command;
import auction_system.server.services.autobid.AutoBidService;
import auction_system.server.services.bidding.AuctionBidService;
import auction_system.server.session.ClientSession;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command nhận yêu cầu bật hoặc cập nhật đấu giá tự động.
 *
 * <p>Command chỉ đọc request và trả response. Nghiệp vụ lưu cấu hình thuộc
 * {@link AutoBidService}; phần tạo bid ngay sau enable thuộc
 * {@link AuctionBidService}.
 */
public final class AutoBidCommand implements Command {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AutoBidCommand.class);

    private static final int MIN_ENABLE_AUTO_BID_PARTS = 4;
    private static final int IDX_AUCTION_ID = 1;
    private static final int IDX_MAX_AMOUNT = 2;
    private static final int IDX_STEP_AMOUNT = 3;
    
    private final AutoBidService autoBidService;
    private final AuctionBidService auctionBidService;

    /**
     * Khởi tạo command bật auto-bid.
     *
     * @param autoBidService service quản lý cấu hình auto-bid
     * @param auctionBidService service xử lý đặt giá và trigger auto-bid ngay
     */
    public AutoBidCommand(
            final AutoBidService autoBidService,
            final AuctionBidService auctionBidService) {

        this.autoBidService = Objects.requireNonNull(autoBidService, "autoBidService");
        this.auctionBidService = Objects.requireNonNull(auctionBidService, "auctionBidService");
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        try {
            if (session.getCurrentUser() == null) {
                return fail("Bạn cần đăng nhập trước khi bật auto-bid.");
            }

            if (parts.length < MIN_ENABLE_AUTO_BID_PARTS) {
                return fail("Thiếu thông tin auto-bid.");
            }

            final String auctionId = parts[IDX_AUCTION_ID];
            final long maxAmount = parsePositiveAmount(parts[IDX_MAX_AMOUNT], "Giá tối đa");
            final long stepAmount = parsePositiveAmount(parts[IDX_STEP_AMOUNT], "Bước tăng");
            
            autoBidService.enableAutoBid(
                    auctionId,
                    session.getCurrentUser(),
                    maxAmount,
                    stepAmount
            );

            // Sau khi lưu setting, thử tạo auto-bid ngay nếu phiên hiện tại đã đủ điều kiện.
            auctionBidService.triggerAutoBidAfterEnable(
                    auctionId,
                    session.getCurrentUser());

            LOGGER.info(
                    "Đã lưu auto-bid. user={}, auctionId={}, maxAmount={}, stepAmount={}",
                    session.getCurrentUser().getUsername(),
                    auctionId,
                    maxAmount,
                    stepAmount
            );

            return Protocol.Response.AUTO_BID_OK.name()
                    + Protocol.SEPARATOR
                    + "Đã bật đấu giá tự động.";
        } catch (IllegalArgumentException e) {
            return fail(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Lỗi khi xử lý yêu cầu auto-bid.", e);
            return fail("Không thể bật đấu giá tự động.");
        }
    }

    private long parsePositiveAmount(final String rawValue, final String fieldName) {
        try {
            final long amount = Long.parseLong(rawValue);
            if (amount <= 0) {
                throw new IllegalArgumentException(fieldName + " phải là số dương.");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " không hợp lệ.");
        }
    }

    private String fail(final String message) {
        return Protocol.Response.AUTO_BID_FAIL.name()
                + Protocol.SEPARATOR
                + message;
    }
}
