package auction_system.server.network.command;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.items.Item;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.session.ClientSession;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Xử lý lệnh lấy thông tin chi tiết của một phiên đấu giá.
 */
public class GetAuctionCommand implements Command {
    private static final Logger LOGGER = Logger.getLogger(GetAuctionCommand.class.getName());
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
                return Protocol.Response.ERROR.name() + Protocol.SEPARATOR + "Thiếu auctionId";
            }

            Auction auction = auctionManager.getAuctionById(parts[1]);
            if (auction == null) {
                return Protocol.Response.ERROR.name() + Protocol.SEPARATOR 
                        + "Không tìm thấy phiên đấu giá";
            }

            Item item = auction.getItem();
            // Ưu tiên hiển thị giá đấu cao nhất hiện tại, nếu chưa có ai đặt thì dùng giá khởi điểm
            double currentPrice = (auction.getCurrentHighestBid() != null)
                    ? auction.getCurrentHighestBid().getAmount()
                    : item.getStartPrice();

            // TODO: Review this longass return;
            // Trả về toàn bộ chi tiết phiên đấu giá nối với nhau bằng dấu phân cách
            return Protocol.Response.AUCTION_DETAIL.name()
                    + Protocol.SEPARATOR + auction.getId()
                    + Protocol.SEPARATOR + item.getItemName()
                    + Protocol.SEPARATOR + item.getDescription()
                    + Protocol.SEPARATOR + item.getStartPrice()
                    + Protocol.SEPARATOR + currentPrice
                    + Protocol.SEPARATOR + auction.getStatus().name()
                    + Protocol.SEPARATOR + auction.getEndTime()
                    + Protocol.SEPARATOR + auction.getSeller().getUsername();
        } catch (Exception e) {
            String auctionId = (parts.length > 1) ? parts[1] : "unknown";
            LOGGER.log(Level.SEVERE, "Lỗi hệ thống khi lấy chi tiết phiên đấu giá "
                    + auctionId, e);
            return Protocol.Response.ERROR.name() + Protocol.SEPARATOR 
                    + "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.";
        }
    }
}