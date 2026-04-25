package auction_system.common.models;

import java.util.ArrayList;
import java.util.List;

public class Seller extends Participant {
    private float rating;
    private List<Item> managedItems;

    @Override
    public void update(String message) {}

    public Seller(String username, String email, String password, double balance,
                  float rating) {
        super(username, email, password, balance);
        this.rating = rating;
        managedItems = new ArrayList<>();
    }

    public void listItemForAuction(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Sản phẩm không hợp lệ!");
        }

        // Đảm bảo item do chính seller này sở hữu
        item.setSellerId(this.getId());
        this.managedItems.add(item);
    }

    public void delistItem(Item item) {
        // Hàm remove tự động trả về true nếu xóa thành công, false nếu không có trong list
        boolean removed = this.managedItems.remove(item);
        if (!removed) {
            throw new IllegalArgumentException("Không tìm thấy sản phẩm này trong danh sách của bạn.");
        }
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }


}
