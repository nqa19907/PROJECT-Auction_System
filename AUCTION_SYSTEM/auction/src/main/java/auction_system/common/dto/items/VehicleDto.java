package auction_system.common.dto.items;

/**
 * Lớp Dto đại diện cho dữ liệu chuyển đổi của phương tiện giao thông (Vehicle).
 */
public class VehicleDto extends ItemDto {
    private static final long serialVersionUID = 1L;

    /**
     * Khởi tạo một đối tượng VehicleDto trống.
     */
    public VehicleDto() {
        super();
    }

    /**
     * Khởi tạo một đối tượng VehicleDto với đầy đủ thông tin.
     *
     * @param id          ID của phương tiện.
     * @param itemName    Tên phương tiện.
     * @param description Mô tả chi tiết.
     * @param startPrice  Giá khởi điểm.
     * @param sellerId    ID của người bán.
     */
    public VehicleDto(String id, String itemName, String description,
            double startPrice, String sellerId) {
        super(id, itemName, description, startPrice, sellerId);
    }

    @Override
    public String toString() {
        return "VehicleDto{} " + super.toString();
    }
}
