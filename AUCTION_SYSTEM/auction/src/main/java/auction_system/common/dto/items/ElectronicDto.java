package auction_system.common.dto.items;

import java.time.LocalDateTime;

/**
 * DTO đại diện cho sản phẩm đấu giá loại thiết bị điện tử.
 *
 * <p>Kế thừa {@code ItemDto} và bổ sung các thuộc tính đặc thù:
 * thương hiệu và số tháng bảo hành.</p>
 */
public class ElectronicDto extends ItemDto {

    private static final long serialVersionUID = 1L;

    private final String brand;
    private final int warrantyMonths;

    /**
     * Constructor mặc định cho phép Java Serialization deserialize đối tượng.
     */
    protected ElectronicDto() {
        super();
        this.brand = null;
        this.warrantyMonths = 0;
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param id             ID duy nhất của sản phẩm.
     * @param itemName       Tên thiết bị, không được rỗng.
     * @param description    Mô tả chi tiết.
     * @param startPrice     Giá khởi điểm, phải {@code >= 0}.
     * @param currentPrice   Giá hiện tại.
     * @param sellerId       ID người bán.
     * @param condition      Tình trạng thiết bị.
     * @param imagePath      Đường dẫn ảnh, có thể null.
     * @param createdAt      Thời điểm tạo sản phẩm.
     * @param brand          Thương hiệu của thiết bị.
     * @param warrantyMonths Số tháng bảo hành, phải {@code >= 0}.
     */
    public ElectronicDto(
            String id,
            String itemName,
            String description,
            double startPrice,
            double currentPrice,
            String sellerId,
            String condition,
            String imagePath,
            LocalDateTime createdAt,
            String brand,
            int warrantyMonths) {
        super(id, "ELECTRONIC", itemName, description, startPrice, currentPrice,
                sellerId, condition, imagePath, createdAt);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    /**
     * Trả về chuỗi tóm tắt thông tin thiết bị để hiển thị trên giao diện danh sách.
     *
     * @return Chuỗi gồm tên thiết bị, thương hiệu, số tháng bảo hành và tình trạng.
     */
    @Override
    public String getDisplaySummary() {
        return String.format(
                "Điện tử: %s | Hãng: %s | Bảo hành: %d tháng | Tình trạng: %s",
                getItemName(),
                brand,
                warrantyMonths,
                getCondition());
    }

    /**
     * Trả về thương hiệu của thiết bị điện tử.
     *
     * @return Tên thương hiệu, có thể null nếu không có thông tin.
     */
    public String getBrand() {
        return brand;
    }

    /**
     * Trả về số tháng bảo hành còn lại của thiết bị.
     *
     * @return Số tháng bảo hành, luôn {@code >= 0}.
     */
    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    /**
     * Trả về chuỗi mô tả DTO dùng cho logging và debug.
     *
     * @return Chuỗi thông tin đầy đủ bao gồm dữ liệu từ lớp cha và các trường đặc thù.
     */
    @Override
    public String toString() {
        return super.toString() + String.format(
                " -> ElectronicDto{brand='%s', warrantyMonths=%d}",
                brand,
                warrantyMonths);
    }
}