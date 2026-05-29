package auction_system.server.network.command.auction;

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
 * Xử lý lệnh rời khỏi một phiên đấu giá mà client đang theo dõi.
 */
public class LeaveAuctionCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeaveAuctionCommand.class);
    private final AuctionManager auctionManager;

    public LeaveAuctionCommand(AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    /**
     * Thực thi lệnh rời phiên đấu giá.
     *
     * <p>Lệnh:       {@code LEAVE_AUCTION|auctionId}
     * Thành công: {@code LEAVE_OK|auctionId}
     * Thất bại:   {@code ERROR|message}
     *
     * @param parts   Mảng tham số từ lệnh đã tách.
     * @param session Phiên làm việc của Client.
     * @return Chuỗi phản hồi cho client.
     */
    @Override
    public String execute(String[] parts, ClientSession session) {
        try {
            if (parts.length < 2) {
                return buildErrorResponse("Thiếu auctionId");
            }

            String auctionId = parts[1];
            session.leaveAuction(auctionId);

            return buildSuccessResponse(auctionId);
        } catch (Exception e) {
            String username = session.isLoggedIn() 
                    ? session.getCurrentUser().getUsername() : "guest";
            LOGGER.error("Lỗi hệ thống khi xử lý lệnh rời phiên đấu giá cho "
                    + username, e);
            return buildErrorResponse("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
        }
    }

    private String buildSuccessResponse(final String auctionId) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.LEAVE_OK.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of("auctionId", auctionId)),
                            "Rời phiên đấu giá thành công."));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response rời phiên: {}", exception.getMessage());
            return Protocol.Response.LEAVE_OK.name()
                    + Protocol.SEPARATOR
                    + auctionId;
        }
    }

    private String buildErrorResponse(final String message) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.ERROR.name(),
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi rời phiên: {}", exception.getMessage());
            return Protocol.Response.ERROR.name()
                    + Protocol.SEPARATOR
                    + message;
        }
    }
}
