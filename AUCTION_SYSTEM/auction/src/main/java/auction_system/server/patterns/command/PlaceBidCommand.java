package auction_system.server.patterns.command;

import auction_system.common.exceptions.AuctionClosedException;
import auction_system.common.exceptions.InvalidBidException;
import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.constants.Protocol;
import auction_system.common.models.users.Bidder;
import auction_system.common.models.users.User;
import auction_system.server.patterns.singleton.AuctionManager;
import auction_system.server.session.ClientSession;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Xử lý lệnh đặt giá từ client.
 */
public class PlaceBidCommand implements Command {
    private static final Logger LOGGER = Logger.getLogger(PlaceBidCommand.class.getName());

    /**
     * Thực thi lệnh đặt giá.
     *
     * <p>Lệnh:       {@code PLACE_BID|auctionId|amount}
     * Thành công: {@code BID_OK|auctionId|newPrice}
     * Thất bại:   {@code BID_FAIL|message} hoặc {@code ERROR|message}
     *
     * @param parts   Mảng tham số từ lệnh đã tách.
     * @param session Phiên làm việc của Client.
     * @return Chuỗi phản hồi cho client.
     */
    @Override
    public String execute(String[] parts, ClientSession session) {
        try {
            if (!session.isLoggedIn()) {
                return Protocol.RES_ERROR + Protocol.SEPARATOR + "Bạn cần đăng nhập trước";
            }

            User currentUser = session.getCurrentUser();
            // Xác minh quyền: chỉ người mua (Bidder) mới được phép đặt giá
            if (!(currentUser instanceof Bidder)) {
                return Protocol.RES_BID_FAIL + Protocol.SEPARATOR 
                        + "Chỉ người mua (Bidder) mới có thể đặt giá";
            }

            if (parts.length < 3) {
                return Protocol.RES_BID_FAIL + Protocol.SEPARATOR + "Thiếu thông tin đặt giá";
            }

            String auctionId = parts[1];
            double amount;
            try {
                amount = Double.parseDouble(parts[2]);
            } catch (NumberFormatException ex) {
                return Protocol.RES_BID_FAIL + Protocol.SEPARATOR + "Số tiền không hợp lệ";
            }

            if (amount <= 0) {
                return Protocol.RES_BID_FAIL + Protocol.SEPARATOR + "Số tiền phải lớn hơn 0";
            }

            Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);
            if (auction == null) {
                return Protocol.RES_BID_FAIL + Protocol.SEPARATOR + "Không tìm thấy phiên đấu giá";
            }

            Bidder bidder = (Bidder) currentUser;
            BidTransaction bid = new BidTransaction(bidder, amount);

            // Lớp Auction sẽ tự kiểm tra tính hợp lệ của số tiền và thời gian,
            // nếu sai sẽ ném ra Exception
            try {
                auction.placeBid(bid);
                LOGGER.info(bidder.getUsername() + " đặt " + amount + " cho phiên " + auctionId);
                return Protocol.RES_BID_OK + Protocol.SEPARATOR 
                        + auctionId + Protocol.SEPARATOR + amount;
            } catch (AuctionClosedException | InvalidBidException ex) {
                return Protocol.RES_BID_FAIL + Protocol.SEPARATOR + ex.getMessage();
            }
        } catch (Exception e) {
            String username = session.isLoggedIn() 
                    ? session.getCurrentUser().getUsername() : "guest";
            LOGGER.log(Level.SEVERE, "Lỗi hệ thống khi xử lý lệnh đặt giá cho " 
                    + username, e);
            return Protocol.RES_BID_FAIL + Protocol.SEPARATOR 
                    + "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.";
        }
    }
}