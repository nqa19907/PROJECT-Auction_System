package auction_system.server.network.command.auction;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.items.Item;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý lệnh lấy danh sách tất cả các phiên đấu giá.
 */
public class ListAuctionsCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListAuctionsCommand.class);
    private final AuctionManager auctionManager;

    public ListAuctionsCommand(AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }
    /**
     * Thực thi lệnh lấy danh sách phiên đấu giá.
     *
     * <p>Lệnh:       {@code LIST_AUCTIONS}
     * Thành công: {@code AUCTION_LIST|n} và theo sau là n dòng, mỗi dòng có định dạng:
     * {@code auctionId|itemName|currentPrice|status|endTime|itemType|startPrice}
     * Thất bại:   {@code ERROR|message}
     *
     * @param parts   Mảng tham số từ lệnh đã tách (không dùng).
     * @param session Phiên làm việc của Client (không dùng).
     * @return Chuỗi phản hồi cho client, có thể chứa nhiều dòng.
     */

    @Override
    public String execute(String[] parts, ClientSession session) {
        try {
            List<Auction> auctions = auctionManager.getAllAuctions().stream()
                    .filter(this::isVisibleToClient)
                    .toList();
            return buildSuccessResponse(auctions);
        } catch (Exception e) {
            LOGGER.error("Lỗi hệ thống khi lấy danh sách phiên đấu giá", e);
            return buildErrorResponse("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
        }
    }

    private String buildSuccessResponse(final List<Auction> auctions) {
        List<List<String>> auctionRecords = new ArrayList<>();
        for (Auction auction : auctions) {
            auctionRecords.add(toAuctionRecord(auction));
        }

        // Trả danh sách phiên bằng JSON cho client render bảng đấu giá.
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.AUCTION_LIST.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of(
                                    "count", auctionRecords.size(),
                                    "auctions", auctionRecords)),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response danh sách phiên: {}",
                    exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON AUCTION_LIST.", exception);
        }
    }

    private List<String> toAuctionRecord(final Auction auction) {
        Item item = auction.getItem();
        double currentPrice = (auction.getCurrentHighestBid() != null)
                ? auction.getCurrentHighestBid().getAmount()
                : item.getStartPrice();

        // Giữ thứ tự field giống protocol cũ để client render bảng không đổi.
        return List.of(
                String.valueOf(auction.getId()),
                String.valueOf(item.getItemName()),
                String.valueOf(currentPrice),
                String.valueOf(auction.getStatus()),
                String.valueOf(auction.getStartTime()),
                String.valueOf(auction.getEndTime()),
                item.getClass().getSimpleName(),
                String.valueOf(item.getStartPrice()),
                resolveSellerId(auction),
                String.valueOf(auction.isAntiSnipingEnabled()));
    }

    private String buildErrorResponse(final String message) {
        // Trả lỗi dạng JSON để NetworkClient route theo type ERROR.
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.ERROR.name(),
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi lấy danh sách phiên: {}",
                    exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON ERROR.", exception);
        }
    }

    /**
     * Kiểm tra phiên còn nên hiển thị trên danh sách tham gia đấu giá hay không.
     *
     * @param auction phiên đấu giá cần kiểm tra
     * @return true nếu phiên chưa kết thúc hoặc chưa bị hủy
     */
    private boolean isVisibleToClient(final Auction auction) {
        return auction.getStatus() != AuctionStatus.FINISHED
                && auction.getStatus() != AuctionStatus.CANCELED;
    }

    /**
     * Lấy mã người bán từ phiên hoặc item để client chỉ cho phép quan sát sản phẩm của mình.
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
