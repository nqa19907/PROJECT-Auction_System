package auction_system.server.network.command.auction;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;
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
                return Protocol.Response.ERROR.name() + Protocol.SEPARATOR 
                        + "Bạn cần đăng nhập trước";
            }
            if (parts.length < 2) {
                return Protocol.Response.JOIN_FAIL.name() + Protocol.SEPARATOR + "Thiếu auctionId";
            }

            String auctionId = parts[1];
            Auction auction = auctionManager.getAuctionById(auctionId);

            if (auction == null) {
                return Protocol.Response.JOIN_FAIL.name() + Protocol.SEPARATOR 
                        + "Không tìm thấy phiên đấu giá";
            }

            // Không cho phép theo dõi nếu phiên đấu giá đã kết thúc hoặc bị huỷ
            if (auction.getStatus() == AuctionStatus.FINISHED
                    || auction.getStatus() == AuctionStatus.CANCELED) {
                return Protocol.Response.JOIN_FAIL.name() + Protocol.SEPARATOR 
                        + "Phiên đấu giá đã kết thúc hoặc bị huỷ";
            }

            session.joinAuction(auctionId);

            LOGGER.info(session.getCurrentUser().getUsername() + " tham gia phiên: " + auctionId);
            return Protocol.Response.JOIN_OK.name() + Protocol.SEPARATOR + auctionId;
        } catch (Exception e) {
            String username = session.isLoggedIn() 
                    ? session.getCurrentUser().getUsername() : "guest";
            LOGGER.error("Lỗi hệ thống khi xử lý lệnh tham gia phiên đấu giá cho "
                    + username, e);
            return Protocol.Response.JOIN_FAIL.name() + Protocol.SEPARATOR 
                    + "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.";
        }
    }
}