package auction_system.server.network.command;

import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.session.ClientSession;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Xử lý lệnh rời khỏi một phiên đấu giá mà client đang theo dõi.
 */
public class LeaveAuctionCommand implements Command {
    private static final Logger LOGGER = Logger.getLogger(LeaveAuctionCommand.class.getName());
    private final AuctionManager auctionManager;

    public LeaveAuctionCommand(AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager,"auctionManager");
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
                return Protocol.Response.ERROR.name() + Protocol.SEPARATOR + "Thiếu auctionId";
            }

            String auctionId = parts[1];
            session.leaveAuction(auctionId);

            return Protocol.Response.LEAVE_OK.name() + Protocol.SEPARATOR + auctionId;
        } catch (Exception e) {
            String username = session.isLoggedIn() 
                    ? session.getCurrentUser().getUsername() : "guest";
            LOGGER.log(Level.SEVERE, "Lỗi hệ thống khi xử lý lệnh rời phiên đấu giá cho " 
                    + username, e);
            return Protocol.Response.ERROR.name() + Protocol.SEPARATOR 
                    + "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.";
        }
    }
}