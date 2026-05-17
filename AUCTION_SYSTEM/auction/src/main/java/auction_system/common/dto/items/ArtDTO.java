package auction_system.common.dto.items;

/**
 * Lớp Dto đại diện cho dữ liệu chuyển đổi của tác phẩm nghệ thuật (Art).
 */
public class ArtDto extends ItemDto {
    private static final long serialVersionUID = 1L;

    /**
     * Khởi tạo một đối tượng ArtDto trống.
     */
    public ArtDto() {
        super();
    }

    /**
     * Khởi tạo một đối tượng ArtDto với đầy đủ thông tin.
     *
     * @param id          ID của tác phẩm.
     * @param itemName    Tên tác phẩm.
     * @param description Mô tả chi tiết.
     * @param startPrice  Giá khởi điểm.
     * @param sellerId    ID của người bán.
     */
    public ArtDto(String id, String itemName, String description, double startPrice,
            String sellerId) {
        super(id, itemName, description, startPrice, sellerId);
    }

    @Override
    public String toString() {
        return "ArtDto{} " + super.toString();
    }
}