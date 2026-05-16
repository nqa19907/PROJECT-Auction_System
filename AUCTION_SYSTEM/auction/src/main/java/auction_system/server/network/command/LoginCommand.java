package auction_system.server.network.command;

import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.session.ClientSession;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Xử lý lệnh đăng nhập.
 */
public class LoginCommand implements Command {
    private static final Logger LOGGER = Logger.getLogger(LoginCommand.class.getName());

    /**
     * Xử lý đăng nhập bằng Email và Password.
     *
     * <p>Lệnh:       {@code LOGIN|email|password}
     * Thành công: {@code LOGIN_OK|userId|username|role}
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
            LOGGER.warning("Từ chối đăng nhập: " 
                    + "Sai cú pháp lệnh");
            return failPrefix + "Thiếu thông tin đăng nhập";
        }

        // CHỈNH SỬA: Đổi tên biến từ username thành email để phản ánh 
        // đúng dữ liệu từ Client gửi lên
        String email = parts[1].trim();
        String password = parts[2].trim();

        try {
            // Gọi hàm findUserByCredentials (đã được sửa ở AuctionManager để quét theo email)
            User user = AuctionManager.getInstance().findUserByCredentials(email, password);
            if (user == null) {
                return failPrefix + "Email hoặc mật khẩu không đúng";
            }

            // Ngăn chặn đăng nhập đồng thời trên nhiều thiết bị
            if (AuctionManager.getInstance().isAlreadyOnline(user.getId())) {
                return failPrefix + "Tài khoản này đang đăng nhập ở nơi khác";
            }

            session.setCurrentUser(user);
            // Sử dụng hành vi của đối tượng thay vì thay đổi trạng thái trực tiếp
            user.setOnline(true);
            AuctionManager.getInstance().userLoggedIn(user);

            String role = user.getRoleName();
            LOGGER.info("Đăng nhập thành công: " + email 
                    + " [" + role + "]");

            // Trả về gói tin thành công, tầng network phía dưới sẽ tự bắn chuỗi này về Client
            return Protocol.Response.LOGIN_OK.name() + Protocol.SEPARATOR 
                    + user.getId() + Protocol.SEPARATOR
                    + user.getUsername() + Protocol.SEPARATOR
                    + role;
        } catch (Exception e) {
            // Bắt mọi lỗi hệ thống để không làm chết thread của client
            LOGGER.log(Level.SEVERE, "Lỗi hệ thống khi đăng nhập cho email: " + email, e);
            return failPrefix + "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.";
        }
    }
}