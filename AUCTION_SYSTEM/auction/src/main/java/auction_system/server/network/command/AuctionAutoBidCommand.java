package auction_system.server.network.command;

import auction_system.common.network.Protocol;
import auction_system.server.services.AutoBidService;
import auction_system.server.session.ClientSession;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command tạm thời nhận yêu cầu bật đấu giá tự động.
 *
 * <p>Ở bước này command chỉ validate format request và trả phản hồi OK/FAIL
 * để hoàn thiện luồng client-server. Nghiệp vụ lưu cấu hình auto-bid và xử lý
 * tự động trả giá sẽ được nối sau trong service riêng.
 */
public final class AuctionAutoBidCommand implements Command {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuctionAutoBidCommand.class);

    private static final int MIN_ENABLE_AUTO_BID_PARTS = 4;
    private static final int IDX_AUCTION_ID = 1;
    private static final int IDX_MAX_AMOUNT = 2;
    private static final int IDX_STEP_AMOUNT = 3;
    
    private final AutoBidService autoBidService;

    /**
     * Khởi tạo command bật auto-bid.
     *
     * @param autoBidService service quản lý cấu hình auto-bid
     */
    public AuctionAutoBidCommand(final AutoBidService autoBidService) {
        this.autoBidService = Objects.requireNonNull(autoBidService, "autoBidService");
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