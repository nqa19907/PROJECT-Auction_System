package auction_system.server.network.command.bidding;

import auction_system.common.exceptions.InvalidBidException;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.network.command.JsonPayloadCommand;
import auction_system.server.services.autobid.AutoBidService;
import auction_system.server.services.bidding.AuctionBidService;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
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
public final class AutoBidCommand implements JsonPayloadCommand {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AutoBidCommand.class);

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
    public String execute(final JsonNode payload, final ClientSession session) {
        try {
            // Kiểm tra session và payload trước khi đọc cấu hình auto-bid.
            if (session.getCurrentUser() == null) {
                return fail("Bạn cần đăng nhập trước khi bật auto-bid.");
            }

            if (payload == null
                    || payload.path("auctionId").asText("").isBlank()
                    || payload.path("maxAmount").isMissingNode()
                    || payload.path("stepAmount").isMissingNode()) {
                return fail("Thiếu thông tin auto-bid.");
            }

            final String auctionId = payload.path("auctionId").asText();
            final long maxAmount = parsePositiveAmount(
                    payload.path("maxAmount").asText(),
                    "Giá tối đa");
            final long stepAmount = parsePositiveAmount(
                    payload.path("stepAmount").asText(),
                    "Bước tăng");

            // Lưu cấu hình và kích hoạt vòng bid tự động ngay sau khi enable.
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

            // Trả lại cấu hình đã lưu để client đồng bộ trạng thái form.
            return success(auctionId, maxAmount, stepAmount);
        } catch (InvalidBidException | IllegalArgumentException e) {
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
        try {
            // Đóng gói lỗi nghiệp vụ thành JSON để client route qua AUTO_BID_FAIL.
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.AUTO_BID_FAIL.name(),
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi bật auto-bid: {}", exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON AUTO_BID_FAIL.", exception);
        }
    }

    private String success(
            final String auctionId,
            final long maxAmount,
            final long stepAmount) {
        final String message = "Đã bật đấu giá tự động.";
        try {
            // Trả đầy đủ cấu hình đã lưu trong payload JSON.
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.AUTO_BID_OK.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of(
                                    "auctionId", auctionId,
                                    "maxAmount", maxAmount,
                                    "stepAmount", stepAmount)),
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response bật auto-bid: {}",
                    exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON AUTO_BID_OK.", exception);
        }
    }
}
