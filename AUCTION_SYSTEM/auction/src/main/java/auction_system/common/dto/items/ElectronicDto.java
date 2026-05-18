package auction_system.common.dto;

import auction_system.common.models.Electronic;
import java.time.LocalDateTime;

/**
 * DTO đại diện cho thiết bị điện tử (Electronic) trong quá trình truyền dữ liệu qua mạng.
 *
 * <p><b>Vị trí trong luồng dữ liệu real-time:</b></p>
 * <pre>
 *  [Server]  Electronic (model) ──fromItem()──▶ ElectronicDto ──Socket──▶ [Client]
 *  [Client]  ElectronicDto ──toItem()──▶ Electronic (model, nếu Server cần tái tạo)
 * </pre>
 *
 * <p><b>Nguyên tắc thiết kế:</b></p>
 * <ul>
 *   <li><b>SRP</b> – Chỉ mang dữ liệu đặc thù của Electronic, không chứa logic nghiệp vụ.</li>
 *   <li><b>LSP</b> – Có thể thay thế {@link ItemDto} ở mọi nơi nhận ItemDto.</li>
 *   <li><b>Static factory</b> – {@code fromItem()} và {@code toItem()} tách biệt
 *       việc chuyển đổi khỏi constructor, tránh constructor phình to.</li>
 * </ul>
 */
public final class ElectronicDto extends ItemDto {

    /**
     * serialVersionUID riêng của ElectronicDto — khác với lớp cha và các sibling
     * để tránh nhầm lẫn khi deserialize qua ObjectInputStream.
     */
    private static final long serialVersionUID = 3L;

    private final String brand;
    private final int warrantyMonths;

    /**
     * Constructor mặc định — cần thiết cho Java deserialization qua ObjectInputStream.
     *
     * <p>Không dùng trực tiếp; hãy dùng {@link #fromItem(Electronic)}.</p>
     */
    public ElectronicDto() {
        super();
        this.brand = null;
        this.warrantyMonths = 0;
    }

    /**
     * Constructor đầy đủ — chỉ dùng nội bộ bởi static factory {@link #fromItem(Electronic)}.
     *
     * @param id             ID duy nhất (UUID).
     * @param itemName       Tên thiết bị.
     * @param description    Mô tả thiết bị.
     * @param startPrice     Giá khởi điểm.
     * @param currentPrice   Giá hiện tại.
     * @param sellerId       ID người bán.
     * @param condition      Tình trạng thiết bị.
     * @param imagePath      Đường dẫn hình ảnh.
     * @param createdAt      Thời điểm tạo.
     * @param brand          Thương hiệu, không được rỗng.
     * @param warrantyMonths Số tháng bảo hành, phải {@code >= 0}.
     *
     * @throws IllegalArgumentException nếu brand rỗng hoặc warrantyMonths âm.
     */
    private ElectronicDto(
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

        super(id, "Electronic", itemName, description,
                startPrice, currentPrice,
                sellerId, condition, imagePath, createdAt);

        if (brand == null || brand.trim().isEmpty()) {
            throw new IllegalArgumentException("Thương hiệu thiết bị không được để trống.");
        }
        if (warrantyMonths < 0) {
            throw new IllegalArgumentException(
                    "Số tháng bảo hành không được âm: " + warrantyMonths);
        }

        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    // -------------------------------------------------------------------------
    // Static factory methods
    // -------------------------------------------------------------------------

    /**
     * Tạo {@code ElectronicDto} từ domain model {@link Electronic}.
     *
     * <p>Dùng phía <b>Server</b> trước khi gửi dữ liệu xuống Client qua Socket.</p>
     *
     * <p>Ví dụ sử dụng:</p>
     * <pre>{@code
     * Electronic item = electronicRepository.findById(id);
     * ElectronicDto dto = ElectronicDto.fromItem(item);
     * objectOutputStream.writeObject(dto);
     * }</pre>
     *
     * @param electronic Domain model nguồn, không được null.
     *
     * @return {@code ElectronicDto} chứa đầy đủ thông tin của {@code electronic}.
     *
     * @throws IllegalArgumentException nếu {@code electronic} là null.
     */
    public static ElectronicDto fromItem(Electronic electronic) {
        if (electronic == null) {
            throw new IllegalArgumentException(
                    "Electronic model không được null khi tạo ElectronicDto.");
        }
        return new ElectronicDto(
                electronic.getId(),
                electronic.getItemName(),
                electronic.getDescription(),
                electronic.getStartPrice(),
                electronic.getCurrentPrice(),
                electronic.getSellerId(),
                electronic.getCondition(),
                electronic.getImagePath(),
                electronic.getCreatedAt(),
                electronic.getBrand(),
                electronic.getWarrantyMonths());
    }

    /**
     * Tái tạo domain model {@link Electronic} từ DTO này.
     *
     * <p>Dùng phía Server khi cần xử lý nghiệp vụ sau khi nhận DTO từ service khác,
     * hoặc phục vụ việc test và tái tạo trạng thái.</p>
     *
     * @return {@link Electronic} tương ứng với dữ liệu của DTO này.
     */
    public Electronic toItem() {
        Electronic electronic = new Electronic(
                getItemName(),
                getDescription(),
                getStartPrice(),
                getSellerId(),
                getCondition(),
                getImagePath(),
                this.brand,
                this.warrantyMonths);
        if (getCurrentPrice() > getStartPrice()) {
            electronic.setCurrentPrice(getCurrentPrice());
        }
        return electronic;
    }

    // -------------------------------------------------------------------------
    // Implement abstract method từ ItemDto
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Định dạng hiển thị:
     * {@code "Điện tử | <Tên> | Hãng: <Brand> | Bảo hành: <N> tháng | Tình trạng: <X>"}</p>
     */
    @Override
    public String getDisplaySummary() {
        String warranty = warrantyMonths > 0
                ? warrantyMonths + " tháng"
                : "Không bảo hành";
        return String.format(
                "Điện tử | %s | Hãng: %s | Bảo hành: %s | Tình trạng: %s",
                getItemName(), brand, warranty, getCondition());
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Trả về thương hiệu của thiết bị.
     *
     * @return Tên thương hiệu, ví dụ: {@code "Samsung"}, {@code "Apple"}, {@code "Sony"}.
     */
    public String getBrand() {
        return brand;
    }

    /**
     * Trả về số tháng bảo hành của thiết bị.
     *
     * <p>Giá trị {@code 0} nghĩa là thiết bị không có bảo hành.
     * Kết hợp với {@link #getCondition()} để đánh giá tổng thể thiết bị.</p>
     *
     * @return Số tháng bảo hành, luôn {@code >= 0}.
     */
    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    /**
     * Trả về chuỗi mô tả ElectronicDto dùng cho logging và debug.
     *
     * <p>Để hiển thị trên UI, dùng {@link #getDisplaySummary()} thay thế.</p>
     *
     * @return Chuỗi thông tin rút gọn bao gồm thông tin lớp cha và thông tin Electronic.
     */
    @Override
    public String toString() {
        return super.toString()
                + String.format(
                " -> Electronic{brand='%s', warrantyMonths=%d}",
                brand, warrantyMonths);
    }
}