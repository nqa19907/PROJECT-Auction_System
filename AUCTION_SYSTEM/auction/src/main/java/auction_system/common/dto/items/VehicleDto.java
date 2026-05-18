package auction_system.common.dto;

import auction_system.common.models.Electronic;
import java.time.LocalDateTime;

/**
 * DTO đại diện cho thiết bị điện tử (Electronic) trong quá trình truyền dữ liệu qua mạng.
 *
 * <p><b>Vị trí trong luồng dữ liệu real-time:</b></p>
 * <pre>
 *  [Server]  Electronic (model) ──fromItem()──▶ ElectronicDTO ──Socket──▶ [Client]
 *  [Client]  ElectronicDTO ──toItem()──▶ Electronic (model, nếu Server cần tái tạo)
 * </pre>
 *
 * <p><b>Nguyên tắc thiết kế:</b></p>
 * <ul>
 *   <li><b>SRP</b> – Chỉ mang dữ liệu đặc thù của Electronic, không chứa logic nghiệp vụ.</li>
 *   <li><b>LSP</b> – Có thể thay thế {@link ItemDTO} ở mọi nơi nhận ItemDTO.</li>
 *   <li><b>Static factory</b> – Tách biệt việc chuyển đổi khỏi constructor.</li>
 * </ul>
 */
public final class ElectronicDTO extends ItemDTO {

    // -------------------------------------------------------------------------
    // Checkstyle: serialVersionUID bắt buộc — phải khác với lớp cha và các sibling
    // -------------------------------------------------------------------------
    private static final long serialVersionUID = 3L;

    // -------------------------------------------------------------------------
    // Checkstyle: field private final — bất biến sau khi DTO được tạo
    // -------------------------------------------------------------------------
    private final String brand;
    private final int warrantyMonths;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Constructor mặc định — cần thiết cho Java deserialization qua ObjectInputStream.
     *
     * <p>Không dùng trực tiếp; hãy dùng {@link #fromItem(Electronic)}.</p>
     */
    public ElectronicDTO() {
        super();
        this.brand = null;
        this.warrantyMonths = 0;
    }

    /**
     * Constructor đầy đủ — dùng nội bộ bởi static factory methods.
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
     * @param brand          Thương hiệu.
     * @param warrantyMonths Số tháng bảo hành (>= 0).
     */
    private ElectronicDTO(
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

        // Checkstyle: validate trước khi gán — brand không được rỗng
        if (brand == null || brand.trim().isEmpty()) {
            throw new IllegalArgumentException("Thương hiệu thiết bị không được để trống.");
        }
        // Checkstyle: số tháng bảo hành không âm (0 = không bảo hành, hợp lệ)
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
     * Tạo {@code ElectronicDTO} từ domain model {@link Electronic}.
     *
     * <p>Dùng phía <b>Server</b> trước khi gửi dữ liệu xuống Client qua Socket.</p>
     *
     * <p>Ví dụ:</p>
     * <pre>{@code
     * Electronic item = electronicRepository.findById(id);
     * ElectronicDTO dto = ElectronicDTO.fromItem(item);
     * objectOutputStream.writeObject(dto);
     * }</pre>
     *
     * @param electronic Domain model, không được null.
     * @return {@code ElectronicDTO} chứa đầy đủ thông tin của {@code electronic}.
     * @throws IllegalArgumentException nếu {@code electronic} là null.
     */
    public static ElectronicDTO fromItem(Electronic electronic) {
        // Checkstyle: guard clause — kiểm tra null ngay đầu method
        if (electronic == null) {
            throw new IllegalArgumentException(
                    "Electronic model không được null khi tạo ElectronicDTO.");
        }
        return new ElectronicDTO(
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
     * <p>Dùng phía <b>Server</b> nếu cần xử lý nghiệp vụ sau khi nhận DTO
     * từ một service khác, hoặc phục vụ việc test / tái tạo trạng thái.</p>
     *
     * @return {@link Electronic} tương ứng với DTO này.
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
    // Implement abstract method từ ItemDTO
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Định dạng:
     * {@code "Điện tử | <Tên> | Hãng: <Brand> | Bảo hành: <N> tháng | <Tình trạng>"}
     */
    @Override
    public String getDisplaySummary() {
        // Checkstyle: dùng String.format thay vì concatenation nhiều dòng
        String warranty = warrantyMonths > 0
                ? warrantyMonths + " tháng"
                : "Không bảo hành";
        return String.format(
                "Điện tử | %s | Hãng: %s | Bảo hành: %s | Tình trạng: %s",
                getItemName(), brand, warranty, getCondition());
    }

    // -------------------------------------------------------------------------
    // Getters — không có setter (immutable sau khi tạo)
    // -------------------------------------------------------------------------

    /** @return Thương hiệu thiết bị (ví dụ: "Samsung", "Apple", "Sony"). */
    public String getBrand() {
        return brand;
    }

    /**
     * Trả về số tháng bảo hành còn lại tính từ thời điểm đấu giá.
     *
     * <p>Giá trị {@code 0} nghĩa là thiết bị không có bảo hành.</p>
     *
     * @return Số tháng bảo hành (>= 0).
     */
    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    // -------------------------------------------------------------------------
    // toString — logging/debug, không dùng cho UI
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Thêm thông tin đặc thù của Electronic vào chuỗi từ lớp cha.</p>
     */
    @Override
    public String toString() {
        return super.toString()
                + String.format(
                " -> Electronic{brand='%s', warrantyMonths=%d}",
                brand, warrantyMonths);
    }
}
