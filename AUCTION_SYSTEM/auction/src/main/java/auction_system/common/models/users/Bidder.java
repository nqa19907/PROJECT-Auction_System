package auction_system.common.models.users;


/**
 * Lớp đại diện cho người đấu giá (người mua) trong hệ thống.
 */
public class Bidder extends Participant {

    /**
     * Khởi tạo một người đấu giá mới.
     *
     * @param username Tên đăng nhập.
     * @param email    Địa chỉ email.
     * @param password Mật khẩu.
     * @param balance  Số dư tài khoản ban đầu.
     */
    public Bidder(String username, String email, String password, double balance) {
        super(username, email, password, balance);
    }

    @Override
    public void update(String message) {
        System.out.println("[NOTIFY]: " + message);
    }

    @Override
    public String getRoleName() {
        return "BIDDER";
    }


}