package auction_system.common.dto.users;

import java.io.Serializable;

/**
 * Lớp Dto đại diện cho dữ liệu chuyển đổi của người đấu giá (Bidder).
 */
public class BidderDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String email;
    private double balance;

    /**
     * Khởi tạo một đối tượng BidderDto trống.
     */
    public BidderDto() {
    }

    /**
     * Khởi tạo một đối tượng BidderDto với đầy đủ thông tin.
     *
     * @param id       ID của người đấu giá.
     * @param username Tên đăng nhập.
     * @param email    Địa chỉ email.
     * @param balance  Số dư tài khoản.
     */
    public BidderDto(String id, String username, String email, double balance) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.balance = balance;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "BidderDto{"
                + "id='" + id + '\''
                + ", username='" + username + '\''
                + ", email='" + email + '\''
                + ", balance=" + balance
                + '}';
    }
}
