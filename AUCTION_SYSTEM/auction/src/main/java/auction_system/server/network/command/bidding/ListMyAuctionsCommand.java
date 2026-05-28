package auction_system.server.network.command.bidding;

import auction_system.common.models.auctions.Auction;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;
import java.util.List;
import java.util.Objects;

/**
 * Command trả về danh sách phiên đấu giá do user hiện tại đăng bán.
 */
public final class ListMyAuctionsCommand implements Command {

    /** Thành phần quản lý auction trung tâm của server. */
    private final AuctionManager auctionManager;

    /**
     * Khởi tạo command.
     *
     * @param auctionManager manager dùng để lấy danh sách auction hiện tại
     */
    public ListMyAuctionsCommand(final AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    /**
     * Xử lý lệnh LIST_MY_AUCTIONS từ client.
     *
     * @param parts mảng request đã tách theo protocol
     * @param session phiên làm việc của client đang gọi lệnh
     * @return response dạng text theo protocol MY_AUCTION_LIST
     */
    @Override
    public String execute(final String[] parts, final ClientSession session) {
        // Chặn sớm nếu client chưa đăng nhập.
        if (session == null || session.getCurrentUser() == null) {
            return Protocol.Response.ERROR.name()
                    + Protocol.SEPARATOR
                    + "Chua dang nhap.";
        }

        // Lấy id user hiện tại để lọc các phiên "của tôi".
        final String currentUserId = session.getCurrentUser().getId();

        // Tái sử dụng nguồn dữ liệu auction sẵn có, thêm điều kiện lọc theo người bán.
        final List<Auction> myAuctions = auctionManager.getAllAuctions().stream()
                .filter(auction -> auction.getParticipant() != null)
                .filter(auction -> currentUserId.equals(auction.getParticipant().getId()))
                .toList();

        final StringBuilder response = new StringBuilder();
        response.append(Protocol.Response.MY_AUCTION_LIST.name())
                .append(Protocol.SEPARATOR)
                .append(myAuctions.size());

        // Mỗi record là 1 phiên, nối bằng RECORD_SEPARATOR để client parse trong 1 message.
        for (Auction auction : myAuctions) {
            final double currentPrice = (auction.getCurrentHighestBid() != null)
                    ? auction.getCurrentHighestBid().getAmount()
                    : auction.getItem().getCurrentPrice();

            response.append(Protocol.RECORD_SEPARATOR)
                    .append(auction.getId()).append(Protocol.SEPARATOR)
                    .append(auction.getItem().getItemName()).append(Protocol.SEPARATOR)
                    .append(currentPrice).append(Protocol.SEPARATOR)
                    .append(auction.getStatus()).append(Protocol.SEPARATOR)
                    .append(auction.getEndTime());
        }

        return response.toString();
    }
}
