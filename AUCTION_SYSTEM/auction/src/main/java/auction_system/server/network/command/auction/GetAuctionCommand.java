package auction_system.server.network.command.auction;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.items.Item;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý lệnh lấy thông tin chi tiết của một phiên đấu giá.
 */
public class GetAuctionCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetAuctionCommand.class);
    private final AuctionManager auctionManager;

    public GetAuctionCommand(AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    /**
     * Thực thi lệnh lấy chi tiết phiên đấu giá.
     *
     * <p>Lệnh:       {@code GET_AUCTION|auctionId}
     * Thành công: {@code AUCTION_DETAIL|auctionId|itemName|desc|startPrice|currentPrice}
     *             {@code |status|endTime|sellerName}
     * Thất bại:   {@code ERROR|message}
     *
     * @param parts   Mảng tham số từ lệnh đã tách.
     * @param session Phiên làm việc của Client (không dùng).
     * @return Chuỗi phản hồi cho client.
     */
    @Override
    public String execute(String[] parts, ClientSession session) {
        try {
            if (parts.length < 2) {
                return buildErrorResponse("Thiếu auctionId");
            }

            Auction auction = auctionManager.getAuctionById(parts[1]);
            if (auction == null) {
                return buildErrorResponse("Không tìm thấy phiên đấu giá");
            }

            return buildSuccessResponse(auction);
        } catch (Exception e) {
            String auctionId = (parts.length > 1) ? parts[1] : "unknown";
            LOGGER.error("Lỗi hệ thống khi lấy chi tiết phiên đấu giá "
                    + auctionId, e);
            return buildErrorResponse("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
        }
    }

    private String buildSuccessResponse(final Auction auction) {
        List<String> auctionRecord = toAuctionRecord(auction);

        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.AUCTION_DETAIL.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of("auction", auctionRecord)),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response chi tiết phiên: {}",
                    exception.getMessage());
            return buildStringSuccessResponse(auctionRecord);
        }
    }

    private List<String> toAuctionRecord(final Auction auction) {
        Item item = auction.getItem();
        double currentPrice = (auction.getCurrentHighestBid() != null)
                ? auction.getCurrentHighestBid().getAmount()
                : item.getStartPrice();

        return List.of(
                String.valueOf(auction.getId()),
                String.valueOf(item.getItemName()),
                String.valueOf(item.getDescription()),
                String.valueOf(item.getStartPrice()),
                String.valueOf(currentPrice),
                String.valueOf(auction.getStatus()),
                String.valueOf(auction.getStartTime()),
                String.valueOf(auction.getEndTime()),
                resolveSellerName(auction),
                resolveSellerId(auction),
                String.valueOf(auction.isAntiSnipingEnabled()));
    }

    private String buildStringSuccessResponse(final List<String> auctionRecord) {
        return Protocol.Response.AUCTION_DETAIL.name()
                + Protocol.SEPARATOR
                + String.join(Protocol.SEPARATOR, auctionRecord);
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
            LOGGER.warn("Không tạo được JSON lỗi lấy chi tiết phiên: {}",
                    exception.getMessage());
            return Protocol.Response.ERROR.name()
                    + Protocol.SEPARATOR
                    + message;
        }
    }

    private String resolveSellerName(final Auction auction) {
        if (auction.getParticipant() != null && auction.getParticipant().getUsername() != null) {
            return auction.getParticipant().getUsername();
        }

        return "";
    }

    /**
     * Lấy mã người bán từ phiên hoặc item để client kiểm tra quyền quan sát/đặt giá.
     *
     * @param auction phiên đấu giá cần đọc thông tin người bán
     * @return mã người bán, hoặc chuỗi rỗng nếu thiếu dữ liệu
     */
    private String resolveSellerId(final Auction auction) {
        if (auction.getParticipant() != null && auction.getParticipant().getId() != null) {
            return auction.getParticipant().getId();
        }

        if (auction.getItem() != null && auction.getItem().getSellerId() != null) {
            return auction.getItem().getSellerId();
        }

        return "";
    }
}
