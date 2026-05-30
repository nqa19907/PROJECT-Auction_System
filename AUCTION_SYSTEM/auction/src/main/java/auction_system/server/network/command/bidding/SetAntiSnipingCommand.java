package auction_system.server.network.command.bidding;

import auction_system.common.models.auctions.Auction;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý yêu cầu bật/tắt chống đặt giá phút chót cho phiên đấu giá.
 */
public class SetAntiSnipingCommand implements Command {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SetAntiSnipingCommand.class);
    private static final int MIN_PARTS = 3;
    private static final int IDX_AUCTION_ID = 1;
    private static final int IDX_ENABLED = 2;

    /** Manager điều phối trạng thái auction và lưu database. */
    private final AuctionManager auctionManager;

    /**
     * Khởi tạo command cập nhật chống đặt giá phút chót.
     *
     * @param auctionManager manager điều phối phiên đấu giá
     */
    public SetAntiSnipingCommand(final AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    /**
     * Thực thi yêu cầu bật/tắt chống đặt giá phút chót.
     *
     * @param parts tham số request đã tách theo protocol
     * @param session phiên làm việc của client
     * @return response cập nhật trạng thái hoặc lỗi
     */
    @Override
    public String execute(final String[] parts, final ClientSession session) {
        try {
            if (!session.isLoggedIn()) {
                return buildFailResponse("Bạn cần đăng nhập trước.");
            }

            if (parts.length < MIN_PARTS) {
                return buildFailResponse("Thiếu thông tin chống đặt giá phút chót.");
            }

            final String auctionId = parts[IDX_AUCTION_ID];
            final boolean enabled = Boolean.parseBoolean(parts[IDX_ENABLED]);
            final Auction auction = auctionManager.updateAntiSniping(
                    auctionId,
                    session.getCurrentUser(),
                    enabled);

            return buildSuccessResponse(auction);
        } catch (IllegalArgumentException exception) {
            return buildFailResponse(exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.warn("Lỗi hệ thống khi cập nhật chống đặt giá phút chót.", exception);
            return buildFailResponse("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
        }
    }

    /**
     * Tạo response lỗi cho yêu cầu cập nhật chống đặt giá phút chót.
     *
     * @param message thông báo lỗi
     * @return response lỗi theo protocol
     */
    private String buildFailResponse(final String message) {
        // Trả lỗi anti-sniping bằng JSON cho control checkbox phía client.
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.ANTI_SNIPING_UPDATE_FAIL.name(),
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi chống đặt giá phút chót: {}",
                    exception.getMessage());
            throw new IllegalStateException(
                    "Không tạo được JSON ANTI_SNIPING_UPDATE_FAIL.",
                    exception);
        }
    }

    private String buildSuccessResponse(final Auction auction) {
        // Trả trạng thái anti-sniping bằng JSON cho control checkbox phía client.
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.ANTI_SNIPING_UPDATED.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of(
                                    "auctionId", auction.getId(),
                                    "enabled", auction.isAntiSnipingEnabled())),
                            "Cập nhật chống đặt giá phút chót thành công."));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response chống đặt giá phút chót: {}",
                    exception.getMessage());
            throw new IllegalStateException(
                    "Không tạo được JSON ANTI_SNIPING_UPDATED.",
                    exception);
        }
    }
}
