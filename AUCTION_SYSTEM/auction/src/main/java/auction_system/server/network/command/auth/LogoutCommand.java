package auction_system.server.network.command.auth;

import auction_system.common.models.users.User;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý lệnh đăng xuất của người dùng.
 */
public class LogoutCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutCommand.class);
    private final AuctionManager auctionManager;

    public LogoutCommand(AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

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
                auctionManager.userLoggedOut(currentUser);
                currentUser.setOnline(false);
                LOGGER.info("Đăng xuất: " + currentUser.getUsername());
                session.setCurrentUser(null);
            }
            // Đảm bảo client hủy theo dõi tất cả các phiên đấu giá đang tham gia
            session.leaveAllAuctions();
            return buildSuccessResponse();
        } catch (Exception e) {
            String username = session.isLoggedIn() 
                    ? session.getCurrentUser().getUsername() : "guest";
            LOGGER.error("Lỗi hệ thống khi xử lý lệnh đăng xuất cho " + username, e);
            return buildErrorResponse("Lỗi máy chủ nội bộ khi đăng xuất.");
        }
    }

    private String buildSuccessResponse() {
        // Trả kết quả đăng xuất bằng JSON cho AuthService.
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.LOGOUT_OK.name(),
                            null,
                            "OK",
                            null,
                            "Đăng xuất thành công."));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response đăng xuất: {}", exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON LOGOUT_OK.", exception);
        }
    }

    private String buildErrorResponse(final String message) {
        // Trả lỗi đăng xuất bằng JSON cho AuthService.
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.ERROR.name(),
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi đăng xuất: {}", exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON ERROR.", exception);
        }
    }
}
