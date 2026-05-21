package auction_system.common.models.users;

import auction_system.common.models.items.Item;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp đại diện cho người bán trong hệ thống.
 */
public class Seller extends Participant {


    @Override
    public void update(String message) {
        System.out.println("[NOTIFY]: " + message);
    }

    /**
     * Khởi tạo một người bán mới.
     *
     * @param username Tên đăng nhập.
     * @param email    Địa chỉ email.
     * @param password Mật khẩu.
     * @param balance  Số dư tài khoản ban đầu.
     */
    public Seller(String username, String email, String password, double balance) {
        super(username, email, password, balance);
    }

    @Override
    public String getRoleName() {
        return "SELLER";
    }

}
