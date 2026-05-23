package auction_system.server.network.command;

import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.services.AuthService;
import auction_system.server.session.ClientSession;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý lệnh đăng nhập.
 */
public class LoginCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginCommand.class);
    private final AuctionManager auctionManager;
    private final AuthService authService;

    public LoginCommand(AuthService authService, AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.authService = Objects.requireNonNull(authService, "authService");
    }

    /**
     * Xử lý đăng nhập bằng Email và Password.
     *
     * <p>Lệnh:       {@code LOGIN|email|password}
     * Thành công: {@code LOGIN_OK|userId|username|email|role|balance}
     * Thất bại:   {@code LOGIN_FAIL|message}
     *
     * @param parts   Mảng tham số từ lệnh đã tách.
     * @param session Phiên làm việc của Client hiện tại để lưu thông tin sau khi đăng nhập.
     */
    @Override
    public String execute(String[] parts, ClientSession session) {
        // Gom chung chuỗi tiền tố báo lỗi để tái sử dụng
        String failPrefix = Protocol.Response.LOGIN_FAIL.name() + Protocol.SEPARATOR;

        if (parts.length < 3) {
            LOGGER.warn("Từ chối đăng nhập: " + "Sai cú pháp lệnh");
            return failPrefix + "Thiếu thông tin đăng nhập";
        }

        // CHỈNH SỬA: Đổi tên biến từ username thành email để phản ánh 
        // đúng dữ liệu từ Client gửi lên
        String email = parts[1].trim();
        String password = parts[2].trim();

        try {
            // Optional<User> biểu diễn kết quả đăng nhập có thể có user hoặc rỗng.
            // Nếu thông tin sai, AuthService trả Optional.empty() thay vì trả null.
            Optional<User> authenticatedUser = authService.login(email, password);
            if (authenticatedUser.isEmpty()) {
                return failPrefix + "Email hoặc mật khẩu không đúng";
            }

            // Tới đây Optional chắc chắn có user vì đã kiểm tra isEmpty() ở trên.
            User user = authenticatedUser.get();

            // Ngăn chặn đăng nhập đồng thời trên nhiều thiết bị
            if (auctionManager.isAlreadyOnline(user.getId())) {
                return failPrefix + "Tài khoản này đang đăng nhập ở nơi khác";
            }

            session.setCurrentUser(user);
            // Sử dụng hành vi của đối tượng thay vì thay đổi trạng thái trực tiếp
            user.setOnline(true);
            auctionManager.userLoggedIn(user);

            String role = user.getRoleName();
            double balance = (user instanceof Participant p) ? p.getBalance() : 0.0;

            LOGGER.info("Đăng nhập thành công: " + email
                    + " [" + role + "]");

            // Trả về gói tin thành công, tầng network phía dưới sẽ tự bắn chuỗi này về Client
            return Protocol.Response.LOGIN_OK.name() + Protocol.SEPARATOR
                    + user.getId() + Protocol.SEPARATOR
                    + user.getUsername() + Protocol.SEPARATOR
                    + user.getEmail() + Protocol.SEPARATOR
                    + role + Protocol.SEPARATOR
                    + balance;
        } catch (Exception e) {
            // Bắt mọi lỗi hệ thống để không làm chết thread của client
            LOGGER.error("Lỗi hệ thống khi đăng nhập cho email: " + email, e);
            return failPrefix + "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.";
        }
    }
}
