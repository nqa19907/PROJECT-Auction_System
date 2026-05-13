package auction_system.server.patterns.command;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.items.Item;
import auction_system.common.models.constants.Protocol;
import auction_system.server.patterns.singleton.AuctionManager;
import auction_system.server.session.ClientSession;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Xử lý lệnh lấy danh sách tất cả các phiên đấu giá.
 */
public class ListAuctionsCommand implements Command {
    private static final Logger LOGGER = Logger.getLogger(ListAuctionsCommand.class.getName());

    /**
     * Thực thi lệnh lấy danh sách phiên đấu giá.
     * <p>
     * Lệnh:       {@code LIST_AUCTIONS}
     * Thành công: {@code AUCTION_LIST|n} và theo sau là n dòng, mỗi dòng có định dạng:
     * {@code auctionId|itemName|currentPrice|status|endTime}
     * Thất bại:   {@code ERROR|message}
     *
     * @param parts   Mảng tham số từ lệnh đã tách (không dùng).
     * @param session Phiên làm việc của Client (không dùng).
     * @return Chuỗi phản hồi cho client, có thể chứa nhiều dòng.
     */
    @Override
    public String execute(String[] parts, ClientSession session) {
        try {
            List<Auction> auctions = AuctionManager.getInstance().getAllAuctions();
            StringBuilder response = new StringBuilder();
            response.append(Protocol.RES_AUCTION_LIST).append(Protocol.SEPARATOR).append(auctions.size());

            for (Auction auction : auctions) {
                Item item = auction.getItem();
                // Lấy giá cao nhất hiện tại, nếu chưa có lượt đặt nào thì hiển thị giá khởi điểm
                double currentPrice = (auction.getCurrentHighestBid() != null)
                        ? auction.getCurrentHighestBid().getAmount()
                        : item.getStartPrice();

                response.append("\n") // Dùng ký tự xuống dòng để ngăn cách các dòng phản hồi
                        .append(auction.getId())
                        .append(Protocol.SEPARATOR).append(item.getItemName())
                        .append(Protocol.SEPARATOR).append(currentPrice)
                        .append(Protocol.SEPARATOR).append(auction.getStatus().name())
                        .append(Protocol.SEPARATOR).append(auction.getEndTime());
            }
            return response.toString();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi hệ thống khi lấy danh sách phiên đấu giá", e);
            return Protocol.RES_ERROR + Protocol.SEPARATOR + "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.";
        }
    }
}