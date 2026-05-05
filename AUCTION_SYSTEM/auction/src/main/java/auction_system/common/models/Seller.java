package auction_system.common.models;

import auction_system.common.exceptions.InvalidItemException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp đại diện cho người bán trong hệ thống.
 */
public class Seller extends Participant {
    private float rating;
    private List<Item> managedItems;

    @Override
    public void update(String message) {}

    /**
     * Khởi tạo một người bán mới.
     *
     * @param username Tên đăng nhập.
     * @param email    Địa chỉ email.
     * @param password Mật khẩu.
     * @param balance  Số dư tài khoản ban đầu.
     * @param rating   Đánh giá uy tín của người bán.
     */
    public Seller(String username, String email, String password, double balance,
                  float rating) {
        super(username, email, password, balance);
        this.rating = rating;
        managedItems = new ArrayList<>();
    }

    /**
     * Đăng một sản phẩm mới để đưa vào đấu giá.
     *
     * @param item Sản phẩm cần đăng.
     */
    public void listItemForAuction(Item item) {
        if (item == null) {
            throw new InvalidItemException("Sản phẩm không hợp lệ!");
        }

        // Đảm bảo item do chính seller này sở hữu
        item.setSellerId(this.getId());
        this.managedItems.add(item);
    }

    /**
     * Gỡ bỏ một sản phẩm khỏi danh sách quản lý.
     *
     * @param item Sản phẩm cần gỡ bỏ.
     */
    public void delistItem(Item item) {
        // Hàm remove tự động trả về true nếu xóa thành công, false nếu không có trong list
        boolean removed = this.managedItems.remove(item);
        if (!removed) {
            throw new InvalidItemException("Không tìm thấy sản phẩm này trong danh sách của bạn.");
        }
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }


}
