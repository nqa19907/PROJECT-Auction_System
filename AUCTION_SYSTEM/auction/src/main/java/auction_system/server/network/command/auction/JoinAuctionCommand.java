package auction_system.server.network.command.auction;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
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
 * Xử lý lệnh tham gia một phiên đấu giá.
 */
public class JoinAuctionCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(JoinAuctionCommand.class);
    private final AuctionManager auctionManager;

    public JoinAuctionCommand(AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    /**
     * Thực thi lệnh tham gia phiên đấu giá.
     *
     * <p>Lệnh:       {@code JOIN_AUCTION|auctionId}
     * Thành công: {@code JOIN_OK|auctionId}
     * Thất bại:   {@code JOIN_FAIL|message}
     *
     * @param parts   Mảng tham số từ lệnh đã tách.
     * @param session Phiên làm việc của Client.
     * @return Chuỗi phản hồi cho client.
     */
    @Override
    public String execute(String[] parts, ClientSession session) {
        try {
            if (!session.isLoggedIn()) {
                return buildErrorResponse("Bạn cần đăng nhập trước");
            }
            if (parts.length < 2) {
                return buildFailResponse("Thiếu auctionId");
            }

            String auctionId = parts[1];
            Auction auction = auctionManager.getAuctionById(auctionId);

            if (auction == null) {
                return buildFailResponse("Không tìm thấy phiên đấu giá");
            }

            // Không cho phép theo dõi nếu phiên đấu giá đã kết thúc hoặc bị huỷ
            if (auction.getStatus() == AuctionStatus.FINISHED
                    || auction.getStatus() == AuctionStatus.CANCELED) {
                return buildFailResponse("Phiên đấu giá đã kết thúc hoặc bị huỷ");
            }

            session.joinAuction(auctionId);

            LOGGER.info(session.getCurrentUser().getUsername() + " tham gia phiên: " + auctionId);
            return buildSuccessResponse(auctionId);
        } catch (Exception e) {
            String username = session.isLoggedIn() 
                    ? session.getCurrentUser().getUsername() : "guest";
            LOGGER.error("Lỗi hệ thống khi xử lý lệnh tham gia phiên đấu giá cho "
                    + username, e);
            return buildFailResponse("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
        }
    }

    private String buildSuccessResponse(final String auctionId) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.JOIN_OK.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of("auctionId", auctionId)),
                            "Tham gia phiên đấu giá thành công."));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response tham gia phiên: {}",
                    exception.getMessage());
            return Protocol.Response.JOIN_OK.name()
                    + Protocol.SEPARATOR
                    + auctionId;
        }
    }

    private String buildFailResponse(final String message) {
        return buildFailureLikeResponse(Protocol.Response.JOIN_FAIL.name(), message);
    }

    private String buildErrorResponse(final String message) {
        return buildFailureLikeResponse(Protocol.Response.ERROR.name(), message);
    }

    private String buildFailureLikeResponse(final String type, final String message) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            type,
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi tham gia phiên: {}", exception.getMessage());
            return type
                    + Protocol.SEPARATOR
                    + message;
        }
    }
}
