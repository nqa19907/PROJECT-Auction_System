package auction_system.server.network.command;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.session.ClientSession;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Xử lý lệnh tham gia một phiên đấu giá.
 */
public class JoinAuctionCommand implements Command {
    private static final Logger LOGGER = Logger.getLogger(JoinAuctionCommand.class.getName());

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
                return Protocol.RES_ERROR + Protocol.SEPARATOR + "Bạn cần đăng nhập trước";
            }
            if (parts.length < 2) {
                return Protocol.RES_JOIN_FAIL + Protocol.SEPARATOR + "Thiếu auctionId";
            }

            String auctionId = parts[1];
            Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);

            if (auction == null) {
                return Protocol.RES_JOIN_FAIL + Protocol.SEPARATOR + "Không tìm thấy phiên đấu giá";
            }

            // Không cho phép theo dõi nếu phiên đấu giá đã kết thúc hoặc bị huỷ
            if (auction.getStatus() == AuctionStatus.FINISHED
                    || auction.getStatus() == AuctionStatus.CANCELED) {
                return Protocol.RES_JOIN_FAIL + Protocol.SEPARATOR 
                        + "Phiên đấu giá đã kết thúc hoặc bị huỷ";
            }

            session.joinAuction(auctionId);

            LOGGER.info(session.getCurrentUser().getUsername() + " tham gia phiên: " + auctionId);
            return Protocol.RES_JOIN_OK + Protocol.SEPARATOR + auctionId;
        } catch (Exception e) {
            String username = session.isLoggedIn() 
                    ? session.getCurrentUser().getUsername() : "guest";
            LOGGER.log(Level.SEVERE, "Lỗi hệ thống khi xử lý lệnh tham gia phiên đấu giá cho " 
                    + username, e);
            return Protocol.RES_JOIN_FAIL + Protocol.SEPARATOR 
                    + "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.";
        }
    }
}