package auction_system.server.network.command.auth;

import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.network.command.Command;
import auction_system.server.services.auth.AuthService;
import auction_system.server.core.AuctionManager;
import auction_system.server.session.ClientSession;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý lệnh đăng ký tài khoản mới từ client.
 *
 * <p>Lệnh client gửi lên:
 *
 * <pre>{@code
 * REGISTER|username|email|password|role
 * }</pre>
 *
 * <p>Phản hồi thành công:
 *
 * <pre>{@code
 * REGISTER_OK
 * }</pre>
 *
 * <p>Phản hồi thất bại:
 *
 * <pre>{@code
 * REGISTER_FAIL|message
 * }</pre>
 */
public class RegisterCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterCommand.class);
    private final AuthService authService;
    private final AuctionManager auctionManager;

    /**
     * Khởi tạo command đăng ký tài khoản.
     *
     * @param authService service xử lý nghiệp vụ đăng ký tài khoản
     * @param auctionManager manager dùng để phát sự kiện realtime
     */
    public RegisterCommand(
            final AuthService authService,
            final AuctionManager auctionManager) {
        this.authService = Objects.requireNonNull(authService, "authService");
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    /**
     * Thực thi lệnh đăng ký tài khoản.
     *
     * @param parts mảng tham số được tách từ lệnh client gửi lên
     * @param session phiên làm việc hiện tại của client
     * @return phản hồi gửi về client
     */
    @Override
    public String execute(final String[] parts, final ClientSession session) {
        if (parts.length < 5) {
            return buildFailResponse("Thiếu thông tin đăng ký");
        }

        final String username = parts[1];
        final String email = parts[2];
        final String password = parts[3];
        final String roleName = parts[4];

        try {
            final User registeredUser =
                    authService.register(username, email, password, roleName);

            LOGGER.info(
                    "Đăng ký mới: "
                            + registeredUser.getUsername()
                            + " ["
                            + registeredUser.getRoleName()
                            + "]");
            // Đồng bộ user mới vào registry của AuctionManager để các luồng quản trị
            // (ví dụ xóa user theo ID) nhìn thấy ngay lập tức.
            auctionManager.registerUser(registeredUser);
            // Báo cho các màn hình quản trị đang mở biết danh sách user đã đổi.
            auctionManager.notifyUserListChanged();

            return Protocol.Response.REGISTER_OK.name();
        } catch (IllegalArgumentException exception) {
            return buildFailResponse(exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Lỗi hệ thống khi xử lý đăng ký cho email: " + email,
                    exception);

            return buildFailResponse("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
        }
    }

    /**
     * Tạo phản hồi đăng ký thất bại theo đúng giao thức.
     *
     * @param message thông báo lỗi gửi về client
     * @return chuỗi phản hồi thất bại
     */
    private String buildFailResponse(final String message) {
        return Protocol.Response.REGISTER_FAIL.name()
                + Protocol.SEPARATOR
                + message;
    }
}
