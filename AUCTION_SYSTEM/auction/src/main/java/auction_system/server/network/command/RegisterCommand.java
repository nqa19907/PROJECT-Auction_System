package auction_system.server.network.command;

import auction_system.common.models.users.Bidder;
import auction_system.common.models.users.Seller;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.session.ClientSession;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý lệnh đăng ký tài khoản mới.
 */
public class RegisterCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterCommand.class);
    private final AuctionManager auctionManager;

    public RegisterCommand(AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    /**
     * Thực thi lệnh đăng ký tài khoản mới.
     *
     * <p>Lệnh:       {@code REGISTER|username|email|password|role}
     * Thành công: {@code REGISTER_OK}
     * Thất bại:   {@code REGISTER_FAIL|message}
     *
     * @param parts   Mảng tham số từ lệnh đã tách.
     * @param session Phiên làm việc của Client (không dùng).
     * @return Chuỗi phản hồi cho client.
     */
    @Override
    public String execute(String[] parts, ClientSession session) {
        try {
            if (parts.length < 5) {
                return Protocol.Response.REGISTER_FAIL.name() + Protocol.SEPARATOR 
                        + "Thiếu thông tin đăng ký";
            }

            String username = parts[1];
            String email = parts[2];
            String password = parts[3];

            if (auctionManager.isUsernameTaken(username)) {
                return Protocol.Response.REGISTER_FAIL.name() + Protocol.SEPARATOR 
                        + "Tên đăng nhập đã tồn tại";
            }

            if (!email.contains("@")) {
                return Protocol.Response.REGISTER_FAIL.name() + Protocol.SEPARATOR 
                        + "Email không hợp lệ";
            }

            if (password.length() < 6) {
                return Protocol.Response.REGISTER_FAIL.name() + Protocol.SEPARATOR 
                        + "Mật khẩu phải có ít nhất 6 ký tự";
            }

            String role = parts[4].toUpperCase();
            // Khởi tạo đối tượng User tương ứng với vai trò được yêu cầu
            User newUser = "SELLER".equals(role)
                    ? new Seller(username, email, password, 0.0)
                    : new Bidder(username, email, password, 0.0);

            auctionManager.registerUser(newUser);
            LOGGER.info("Đăng ký mới: " + username + " [" + role + "]");
            return Protocol.Response.REGISTER_OK.name();
        } catch (Exception e) {
            String username = (parts.length > 1) ? parts[1] : "unknown";
            LOGGER.error("Lỗi hệ thống khi xử lý lệnh đăng ký cho "
                    + username, e);
            return Protocol.Response.REGISTER_FAIL.name() + Protocol.SEPARATOR 
                    + "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.";
        }
    }
}
