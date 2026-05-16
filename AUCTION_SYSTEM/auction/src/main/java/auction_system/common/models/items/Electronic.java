package auction_system.common.models.items;

/**
 * Lớp đại diện cho sản phẩm đấu giá là thiết bị điện tử.
 */
public class Electronic extends Item {

    /**
     * Khởi tạo một thiết bị điện tử.
     *
     * @param itemName       Tên sản phẩm.
     * @param description    Mô tả chi tiết.
     * @param startPrice     Giá khởi điểm.
     * @param sellerId       ID của người bán.
     * @param condition      Tình trạng sản phẩm.
     * @param imagePath      Đường dẫn hình ảnh.
     * @param brand          Thương hiệu.
     * @param warrantyMonths Số tháng bảo hành.
     */
    public Electronic(String itemName, String description, double startPrice, String sellerId) {
        super(itemName, description, startPrice, sellerId);

    }

}
