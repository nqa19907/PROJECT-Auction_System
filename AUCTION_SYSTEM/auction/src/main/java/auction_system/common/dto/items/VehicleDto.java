package auction_system.common.dto.items;

import java.time.LocalDateTime;

/**
 * DTO đại diện cho sản phẩm đấu giá loại phương tiện giao thông.
 *
 * <p>Kế thừa {@code ItemDto} và bổ sung các thuộc tính đặc thù:
 * hãng sản xuất, dòng xe, năm sản xuất và số km đã đi.</p>
 */
public class VehicleDto extends ItemDto {

    private static final long serialVersionUID = 1L;

    private final String make;
    private final String model;
    private final int manufacturingYear;
    private final double mileage;

    /**
     * Constructor mặc định cho phép Java Serialization deserialize đối tượng.
     */
    protected VehicleDto() {
        super();
        this.make = null;
        this.model = null;
        this.manufacturingYear = 0;
        this.mileage = 0.0;
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param id                ID duy nhất của sản phẩm.
     * @param itemName          Tên phương tiện, không được rỗng.
     * @param description       Mô tả chi tiết.
     * @param startPrice        Giá khởi điểm, phải {@code >= 0}.
     * @param currentPrice      Giá hiện tại.
     * @param sellerId          ID người bán.
     * @param condition         Tình trạng phương tiện.
     * @param imagePath         Đường dẫn ảnh, có thể null.
     * @param createdAt         Thời điểm tạo sản phẩm.
     * @param make              Hãng sản xuất (ví dụ: "Toyota", "Honda").
     * @param model             Dòng xe (ví dụ: "Camry", "Civic").
     * @param manufacturingYear Năm sản xuất, phải {@code > 0}.
     * @param mileage           Số km đã đi (ODO), phải {@code >= 0}.
     */
    public VehicleDto(
            String id,
            String itemName,
            String description,
            double startPrice,
            double currentPrice,
            String sellerId,
            String condition,
            String imagePath,
            LocalDateTime createdAt,
            String make,
            String model,
            int manufacturingYear,
            double mileage) {
        super(id, "VEHICLE", itemName, description, startPrice, currentPrice,
                sellerId, condition, imagePath, createdAt);
        this.make = make;
        this.model = model;
        this.manufacturingYear = manufacturingYear;
        this.mileage = mileage;
    }

    /**
     * Trả về chuỗi tóm tắt thông tin phương tiện để hiển thị trên giao diện danh sách.
     *
     * @return Chuỗi gồm tên xe, hãng, dòng xe, năm sản xuất và số km đã đi.
     */
    @Override
    public String getDisplaySummary() {
        return String.format(
                "Xe: %s | Hãng: %s | Dòng: %s | Đời: %d | ODO: %,.1f km",
                getItemName(),
                make,
                model,
                manufacturingYear,
                mileage);
    }

    /**
     * Trả về hãng sản xuất của phương tiện.
     *
     * @return Tên hãng sản xuất, có thể null nếu không có thông tin.
     */
    public String getMake() {
        return make;
    }

    /**
     * Trả về dòng xe của phương tiện.
     *
     * @return Tên dòng xe, có thể null nếu không có thông tin.
     */
    public String getModel() {
        return model;
    }

    /**
     * Trả về năm sản xuất của phương tiện.
     *
     * @return Năm sản xuất, luôn {@code > 0} sau khi khởi tạo đầy đủ.
     */
    public int getManufacturingYear() {
        return manufacturingYear;
    }

    /**
     * Trả về số km đã đi của phương tiện.
     *
     * @return Số km ODO, luôn {@code >= 0}.
     */
    public double getMileage() {
        return mileage;
    }

    /**
     * Trả về chuỗi mô tả DTO dùng cho logging và debug.
     *
     * @return Chuỗi thông tin đầy đủ bao gồm dữ liệu từ lớp cha và các trường đặc thù.
     */
    @Override
    public String toString() {
        return super.toString() + String.format(
                " -> VehicleDto{make='%s', model='%s', year=%d, mileage=%.1f}",
                make,
                model,
                manufacturingYear,
                mileage);
    }
}