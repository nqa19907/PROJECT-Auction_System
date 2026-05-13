package auction_system.server.patterns.command;

import auction_system.common.models.users.User;
import auction_system.server.patterns.singleton.AuctionManager;
import auction_system.server.session.ClientSession;
import auction_system.common.models.constants.Protocol;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginCommand implements Command {
    private static final Logger LOGGER = Logger.getLogger(LoginCommand.class.getName());


    /**
     * Xử lý đăng nhập.
     * <p>
     * Lệnh:       {@code LOGIN|username|password}
     * Thành công: {@code LOGIN_OK|userId|username|role}
     * Thất bại:   {@code LOGIN_FAIL|message}
     *
     * @param parts   Mảng tham số từ lệnh đã tách.
     * @param session Phiên làm việc của Client hiện tại để lưu thông tin sau khi đăng nhập.
     */
    @Override
    public String execute(String[] parts, ClientSession session) {
        if (parts.length < 3) {
            LOGGER.warning("Từ chối đăng nhập: Sai cú pháp lệnh");
            return Protocol.RES_LOGIN_FAIL + Protocol.SEPARATOR + "Thiếu thông tin đăng nhập";
        }

        String username = parts[1].trim();
        String password = parts[2].trim();

        try {
            User user = AuctionManager.getInstance().findUserByCredentials(username, password);
            if (user == null) {
                return Protocol.RES_LOGIN_FAIL + Protocol.SEPARATOR
                        + "Tên đăng nhập hoặc mật khẩu không đúng";
            }

            // Ngăn chặn đăng nhập đồng thời trên nhiều thiết bị
            if (AuctionManager.getInstance().isAlreadyOnline(user.getId())) {
                return Protocol.RES_LOGIN_FAIL + Protocol.SEPARATOR
                        + "Tài khoản này đang đăng nhập ở nơi khác";
            }

            session.setCurrentUser(user);
            user.setOnline(true);
            AuctionManager.getInstance().userLoggedIn(user);

            String role = user.getRoleName();
            LOGGER.info("Đăng nhập thành công: " + username + " [" + role + "]");

            return Protocol.RES_LOGIN_OK + Protocol.SEPARATOR + user.getId() + Protocol.SEPARATOR + user.getUsername() + Protocol.SEPARATOR + role;
        } catch (Exception e) {
            // Bắt mọi lỗi hệ thống để không làm chết thread
            LOGGER.log(Level.SEVERE, "Lỗi hệ thống khi đăng nhập cho user: " + username, e);
            return Protocol.RES_LOGIN_FAIL + Protocol.SEPARATOR
                    + "Lỗi máy chủ nội bộ. Vui lòng thử lại sau.";
        }
    }
}
