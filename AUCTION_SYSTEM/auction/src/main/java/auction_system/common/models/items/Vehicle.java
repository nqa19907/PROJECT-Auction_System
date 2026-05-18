package auction_system.common.models.items;

/**
 * Lớp đại diện cho sản phẩm đấu giá là phương tiện giao thông.
 */
public class Vehicle extends Item {

    /**
     * Khởi tạo một phương tiện giao thông.
     *
     * @param itemName    Tên phương tiện.
     * @param description Mô tả chi tiết.
     * @param startPrice  Giá khởi điểm.
     * @param sellerId    ID của người bán.
     */

    public Vehicle(String itemName, String description, Double startPrice, String sellerId) {
        super(itemName, description, startPrice, sellerId);
    }

    @Override
    public String getCategory() {
        return "VEHICLE";
    }

}