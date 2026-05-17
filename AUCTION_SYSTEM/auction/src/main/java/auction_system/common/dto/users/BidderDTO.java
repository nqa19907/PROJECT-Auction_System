package auction_system.common.dto.users;

import java.io.Serializable;

/**
 * DTO chứa thông tin người đấu giá để truyền qua mạng Socket.
 * Đã loại bỏ mật khẩu để bảo mật và không có hàm Setter để đảm bảo an toàn đa luồng.
 */
public final class BidderDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final String email;
    private final double balance;
    private final String roleName;

    /**
     * Khởi tạo một gói dữ liệu BidderDTO bất biến.
     *
     * @param username Tên đăng nhập của người đấu giá.
     * @param email Địa chỉ email.
     * @param balance Số dư tài khoản.
     * @param roleName Vai trò của người dùng.
     */
    public BidderDto(
            final String username,
            final String email,
            final double balance,
            final String roleName) {

        this.username = username;
        this.email = email;
        this.balance = balance;
        this.roleName = roleName;
    }

    /**
     * Lấy tên đăng nhập.
     *
     * @return tên đăng nhập.
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Lấy địa chỉ email.
     *
     * @return email.
     */
    public String getEmail() {
        return this.email;
    }

    /**
     * Lấy số dư tài khoản.
     *
     * @return số dư.
     */
    public double getBalance() {
        return this.balance;
    }

    /**
     * Lấy vai trò người dùng.
     *
     * @return tên vai trò.
     */
    public String getRoleName() {
        return this.roleName;
    }
}