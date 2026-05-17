package auction_system.common.dto.items;

/**
 * Lớp Dto đại diện cho dữ liệu chuyển đổi của thiết bị điện tử (Electronic).
 */
public class ElectronicDto extends ItemDto {
    private static final long serialVersionUID = 1L;

    /**
     * Khởi tạo một đối tượng ElectronicDto trống.
     */
    public ElectronicDto() {
        super();
    }

    /**
     * Khởi tạo một đối tượng ElectronicDto với đầy đủ thông tin.
     *
     * @param id          ID của sản phẩm.
     * @param itemName    Tên sản phẩm.
     * @param description Mô tả chi tiết.
     * @param startPrice  Giá khởi điểm.
     * @param sellerId    ID của người bán.
     */
    public ElectronicDto(String id, String itemName, String description,
            double startPrice, String sellerId) {
        super(id, itemName, description, startPrice, sellerId);
    }

    @Override
    public String toString() {
        return "ElectronicDto{} " + super.toString();
    }
}