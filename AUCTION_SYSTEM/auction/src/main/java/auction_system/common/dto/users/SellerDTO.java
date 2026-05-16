package auction_system.common.dto.users;

import java.io.Serializable;

/**
 * DTO chứa thông tin người bán để truyền qua mạng Socket.
 * Đã loại bỏ mật khẩu và danh sách quản lý sản phẩm để đảm bảo bảo mật.
 * Không có hàm Setter để đảm bảo tính bất biến và an toàn đa luồng.
 */
public final class SellerDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String username;
    private final String email;
    private final double balance;
    private final float rating;
    private final String roleName;

    /**
     * Khởi tạo một gói dữ liệu SellerDTO bất biến.
     *
     * @param username Tên đăng nhập của người bán.
     * @param email Địa chỉ email.
     * @param balance Số dư tài khoản.
     * @param rating Đánh giá uy tín của người bán.
     * @param roleName Vai trò người dùng.
     */
    public SellerDTO(
            final String username,
            final String email,
            final double balance,
            final float rating,
            final String roleName) {

        this.username = username;
        this.email = email;
        this.balance = balance;
        this.rating = rating;
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
     * Lấy đánh giá uy tín của người bán.
     *
     * @return rating.
     */
    public float getRating() {
        return this.rating;
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