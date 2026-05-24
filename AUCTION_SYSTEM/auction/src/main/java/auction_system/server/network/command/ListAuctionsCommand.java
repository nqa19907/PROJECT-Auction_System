package auction_system.server.network.command;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.items.Item;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.session.ClientSession;
import java.util.List;
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
            StringBuilder response = new StringBuilder();
            response.append(Protocol.Response.AUCTION_LIST.name())
                    .append(Protocol.SEPARATOR).append(auctions.size());

            for (Auction auction : auctions) {
                Item item = auction.getItem();
                // Lấy giá cao nhất hiện tại, nếu chưa có lượt đặt nào thì hiển thị giá khởi điểm
                double currentPrice = (auction.getCurrentHighestBid() != null)
                        ? auction.getCurrentHighestBid().getAmount()
                        : item.getStartPrice();

                // Dùng ký tự phân tách để ngăn cách các dòng phản hồi
                response.append(Protocol.RECORD_SEPARATOR)
                        .append(auction.getId())
                        .append(Protocol.SEPARATOR).append(item.getItemName())
                        .append(Protocol.SEPARATOR).append(currentPrice)
                        .append(Protocol.SEPARATOR).append(auction.getStatus().name())
                        .append(Protocol.SEPARATOR).append(auction.getEndTime())
                        .append(Protocol.SEPARATOR).append(item.getClass().getSimpleName())
                        .append(Protocol.SEPARATOR).append(item.getStartPrice());
            }
            return response.toString();
        } catch (Exception e) {
            LOGGER.error("Lỗi hệ thống khi lấy danh sách phiên đấu giá", e);
            return Protocol.Response.ERROR.name() + Protocol.SEPARATOR 
                    + "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.";
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
}
