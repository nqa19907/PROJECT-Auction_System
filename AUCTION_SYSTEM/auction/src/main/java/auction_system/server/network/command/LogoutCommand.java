package auction_system.server.network.command;

import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.session.ClientSession;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Xử lý lệnh đăng xuất của người dùng.
 */
public class LogoutCommand implements Command {
    private static final Logger LOGGER = Logger.getLogger(LogoutCommand.class.getName());

    /**
     * Thực thi lệnh đăng xuất.
     *
     * <p>Lệnh:       {@code LOGOUT}
     * Thành công: {@code LOGOUT_OK}
     * Thất bại:   {@code ERROR|message}
     *
     * @param parts   Mảng tham số từ lệnh đã tách (không dùng).
     * @param session Phiên làm việc của Client.
     * @return Chuỗi phản hồi cho client.
     */
    @Override
    public String execute(String[] parts, ClientSession session) {
        try {
            User currentUser = session.getCurrentUser();
            if (currentUser != null) {
                AuctionManager.getInstance().userLoggedOut(currentUser);
                currentUser.setOnline(false);
                LOGGER.info("Đăng xuất: " + currentUser.getUsername());
                session.setCurrentUser(null);
            }
            // Đảm bảo client hủy theo dõi tất cả các phiên đấu giá đang tham gia
            session.leaveAllAuctions();
            return Protocol.Response.LOGOUT_OK.name();
        } catch (Exception e) {
            String username = session.isLoggedIn() 
                    ? session.getCurrentUser().getUsername() : "guest";
            LOGGER.log(Level.SEVERE, "Lỗi hệ thống khi xử lý lệnh đăng xuất cho " + username, e);
            return Protocol.Response.ERROR.name() + Protocol.SEPARATOR 
                    + "Lỗi máy chủ nội bộ khi đăng xuất.";
        }
    }
}