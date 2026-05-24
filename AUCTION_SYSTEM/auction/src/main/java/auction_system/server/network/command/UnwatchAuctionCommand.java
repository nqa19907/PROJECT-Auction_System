package auction_system.server.network.command;

import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.session.ClientSession;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý lệnh dừng theo dõi realtime một phiên đấu giá.
 */
public class UnwatchAuctionCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnwatchAuctionCommand.class);
    private final AuctionManager auctionManager;

    public UnwatchAuctionCommand(AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    /**
     * Thực thi lệnh dừng theo dõi realtime phiên đấu giá.
     *
     * <p>Lệnh:       {@code UNWATCH_AUCTION|auctionId}
     * Thành công: {@code UNWATCH_OK|auctionId}
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
            session.unwatchAuction(auctionId);

            return Protocol.Response.UNWATCH_OK.name() + Protocol.SEPARATOR + auctionId;
        } catch (Exception e) {
            String username = session.isLoggedIn() 
                    ? session.getCurrentUser().getUsername() : "guest";
            LOGGER.error("Lỗi hệ thống khi xử lý lệnh dừng theo dõi realtime phiên đấu giá cho "
                    + username, e);
            return Protocol.Response.ERROR.name() + Protocol.SEPARATOR 
                    + "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.";
        }
    }
}
