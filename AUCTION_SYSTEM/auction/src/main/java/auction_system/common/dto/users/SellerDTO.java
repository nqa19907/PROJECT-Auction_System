package auction_system.common.dto.users;

import java.io.Serializable;
import java.util.List;

/**
 * Lớp Dto đại diện cho dữ liệu chuyển đổi của người bán (Seller).
 */
public class SellerDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String email;
    private double balance;
    private float rating;
    private List<String> managedItemIds; // Chuyển đổi List<Item> thành List ID để tối ưu Dto

    /**
     * Khởi tạo một đối tượng SellerDto trống.
     */
    public SellerDto() {
    }

    /**
     * Khởi tạo một đối tượng SellerDto với đầy đủ thông tin.
     *
     * @param id             ID của người bán.
     * @param username       Tên đăng nhập.
     * @param email          Địa chỉ email.
     * @param balance        Số dư tài khoản.
     * @param rating         Đánh giá uy tín.
     * @param managedItemIds Danh sách ID sản phẩm quản lý.
     */
    public SellerDto(String id, String username, String email, double balance,
            float rating, List<String> managedItemIds) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.balance = balance;
        this.rating = rating;
        this.managedItemIds = managedItemIds;
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

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public List<String> getManagedItemIds() {
        return managedItemIds;
    }

    public void setManagedItemIds(List<String> managedItemIds) {
        this.managedItemIds = managedItemIds;
    }

    @Override
    public String toString() {
        return "SellerDto{"
                + "id='" + id + '\''
                + ", username='" + username + '\''
                + ", email='" + email + '\''
                + ", balance=" + balance
                + ", rating=" + rating
                + ", managedItemIds=" + managedItemIds
                + '}';
    }
}