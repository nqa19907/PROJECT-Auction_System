package auction_system.common.dto;

import auction_system.common.models.Vehicle;
import java.time.LocalDateTime;

/**
 * DTO đại diện cho phương tiện giao thông (Vehicle) trong quá trình truyền dữ liệu qua mạng.
 *
 * <p><b>Vị trí trong luồng dữ liệu real-time:</b></p>
 * <pre>
 *  [Server]  Vehicle (model) ──fromItem()──▶ VehicleDto ──Socket──▶ [Client]
 *  [Client]  VehicleDto ──toItem()──▶ Vehicle (model, nếu Server cần tái tạo)
 * </pre>
 *
 * <p><b>Nguyên tắc thiết kế:</b></p>
 * <ul>
 *   <li><b>SRP</b> – Chỉ mang dữ liệu đặc thù của Vehicle, không chứa logic nghiệp vụ.</li>
 *   <li><b>LSP</b> – Có thể thay thế {@link ItemDto} ở mọi nơi nhận ItemDto.</li>
 *   <li><b>Static factory</b> – {@code fromItem()} và {@code toItem()} tách biệt
 *       việc chuyển đổi khỏi constructor, tránh constructor phình to.</li>
 * </ul>
 */
public final class VehicleDto extends ItemDto {

    /**
     * serialVersionUID riêng của VehicleDto — khác với lớp cha và các sibling
     * để tránh nhầm lẫn khi deserialize qua ObjectInputStream.
     */
    private static final long serialVersionUID = 4L;

    /**
     * Năm sản xuất tối thiểu hợp lệ.
     * Mốc 1886 là năm chiếc ô tô đầu tiên được sản xuất (Benz Patent-Motorwagen).
     */
    private static final int MIN_MANUFACTURING_YEAR = 1886;

    /** Giá trị ODO tối thiểu — không được âm. */
    private static final double MIN_MILEAGE = 0.0;

    private final String make;
    private final String model;
    private final int manufacturingYear;
    private final double mileage;

    /**
     * Constructor mặc định — cần thiết cho Java deserialization qua ObjectInputStream.
     *
     * <p>Không dùng trực tiếp; hãy dùng {@link #fromItem(Vehicle)}.</p>
     */
    public VehicleDto() {
        super();
        this.make = null;
        this.model = null;
        this.manufacturingYear = 0;
        this.mileage = MIN_MILEAGE;
    }

    /**
     * Constructor đầy đủ — chỉ dùng nội bộ bởi static factory {@link #fromItem(Vehicle)}.
     *
     * @param id                ID duy nhất (UUID).
     * @param itemName          Tên phương tiện.
     * @param description       Mô tả phương tiện.
     * @param startPrice        Giá khởi điểm.
     * @param currentPrice      Giá hiện tại.
     * @param sellerId          ID người bán.
     * @param condition         Tình trạng phương tiện.
     * @param imagePath         Đường dẫn hình ảnh.
     * @param createdAt         Thời điểm tạo.
     * @param make              Hãng sản xuất, không được rỗng.
     * @param model             Mẫu xe, không được rỗng.
     * @param manufacturingYear Năm sản xuất, phải {@code >= 1886}.
     * @param mileage           Số km đã đi (ODO), phải {@code >= 0}.
     *
     * @throws IllegalArgumentException nếu make, model rỗng hoặc năm/ODO không hợp lệ.
     */
    private VehicleDto(
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

        super(id, "Vehicle", itemName, description,
                startPrice, currentPrice,
                sellerId, condition, imagePath, createdAt);

        validateMake(make);
        validateModel(model);
        validateManufacturingYear(manufacturingYear);
        validateMileage(mileage);

        this.make = make;
        this.model = model;
        this.manufacturingYear = manufacturingYear;
        this.mileage = mileage;
    }

    // -------------------------------------------------------------------------
    // Static factory methods
    // -------------------------------------------------------------------------

    /**
     * Tạo {@code VehicleDto} từ domain model {@link Vehicle}.
     *
     * <p>Dùng phía <b>Server</b> trước khi gửi dữ liệu xuống Client qua Socket.</p>
     *
     * <p>Ví dụ sử dụng:</p>
     * <pre>{@code
     * Vehicle vehicle = vehicleRepository.findById(id);
     * VehicleDto dto = VehicleDto.fromItem(vehicle);
     * objectOutputStream.writeObject(dto);
     * }</pre>
     *
     * @param vehicle Domain model nguồn, không được null.
     *
     * @return {@code VehicleDto} chứa đầy đủ thông tin của {@code vehicle}.
     *
     * @throws IllegalArgumentException nếu {@code vehicle} là null.
     */
    public static VehicleDto fromItem(Vehicle vehicle) {
        if (vehicle == null) {
            throw new IllegalArgumentException(
                    "Vehicle model không được null khi tạo VehicleDto.");
        }
        return new VehicleDto(
                vehicle.getId(),
                vehicle.getItemName(),
                vehicle.getDescription(),
                vehicle.getStartPrice(),
                vehicle.getCurrentPrice(),
                vehicle.getSellerId(),
                vehicle.getCondition(),
                vehicle.getImagePath(),
                vehicle.getCreatedAt(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getManufacturingYear(),
                vehicle.getMileage());
    }

    /**
     * Tái tạo domain model {@link Vehicle} từ DTO này.
     *
     * <p>Dùng phía Server khi cần xử lý nghiệp vụ sau khi nhận DTO từ service khác,
     * hoặc phục vụ việc test và tái tạo trạng thái.</p>
     *
     * @return {@link Vehicle} tương ứng với dữ liệu của DTO này.
     */
    public Vehicle toItem() {
        Vehicle vehicle = new Vehicle(
                getItemName(),
                getDescription(),
                getStartPrice(),
                getSellerId(),
                getCondition(),
                getImagePath(),
                this.make,
                this.model,
                this.manufacturingYear,
                this.mileage);
        if (getCurrentPrice() > getStartPrice()) {
            vehicle.setCurrentPrice(getCurrentPrice());
        }
        return vehicle;
    }

    // -------------------------------------------------------------------------
    // Implement abstract method từ ItemDto
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Định dạng hiển thị:
     * {@code "Xe | <Tên> | <Hãng> <Model> | Đời: <Năm> | ODO: <Km> km | Tình trạng: <X>"}</p>
     */
    @Override
    public String getDisplaySummary() {
        return String.format(
                "Xe | %s | %s %s | Đời: %d | ODO: %,.0f km | Tình trạng: %s",
                getItemName(), make, model,
                manufacturingYear, mileage, getCondition());
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Trả về hãng sản xuất phương tiện.
     *
     * @return Tên hãng, ví dụ: {@code "Toyota"}, {@code "Honda"}, {@code "BMW"}.
     */
    public String getMake() {
        return make;
    }

    /**
     * Trả về mẫu xe của phương tiện.
     *
     * @return Tên mẫu xe, ví dụ: {@code "Camry"}, {@code "Civic"}, {@code "X5"}.
     */
    public String getModel() {
        return model;
    }

    /**
     * Trả về năm sản xuất phương tiện.
     *
     * @return Năm sản xuất, luôn {@code >= 1886}.
     */
    public int getManufacturingYear() {
        return manufacturingYear;
    }

    /**
     * Trả về số km đã đi của phương tiện (chỉ số ODO).
     *
     * <p>Giá trị {@code 0.0} không nhất thiết nghĩa là xe mới —
     * hãy kết hợp với {@link #getCondition()} để đánh giá tổng thể.</p>
     *
     * @return Số km đã đi, luôn {@code >= 0}.
     */
    public double getMileage() {
        return mileage;
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    /**
     * Trả về chuỗi mô tả VehicleDto dùng cho logging và debug.
     *
     * <p>Để hiển thị trên UI, dùng {@link #getDisplaySummary()} thay thế.</p>
     *
     * @return Chuỗi thông tin rút gọn bao gồm thông tin lớp cha và thông tin Vehicle.
     */
    @Override
    public String toString() {
        return super.toString()
                + String.format(
                " -> Vehicle{make='%s', model='%s', year=%d, mileage=%.1f km}",
                make, model, manufacturingYear, mileage);
    }

    // -------------------------------------------------------------------------
    // Private validation helpers
    // -------------------------------------------------------------------------

    /**
     * Kiểm tra hãng sản xuất hợp lệ.
     *
     * @param make Hãng cần kiểm tra.
     *
     * @throws IllegalArgumentException nếu null hoặc rỗng.
     */
    private static void validateMake(String make) {
        if (make == null || make.trim().isEmpty()) {
            throw new IllegalArgumentException("Hãng sản xuất phương tiện không được để trống.");
        }
    }

    /**
     * Kiểm tra mẫu xe hợp lệ.
     *
     * @param model Mẫu xe cần kiểm tra.
     *
     * @throws IllegalArgumentException nếu null hoặc rỗng.
     */
    private static void validateModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            throw new IllegalArgumentException("Mẫu xe không được để trống.");
        }
    }

    /**
     * Kiểm tra năm sản xuất hợp lệ.
     *
     * @param year Năm sản xuất cần kiểm tra.
     *
     * @throws IllegalArgumentException nếu năm nhỏ hơn {@link #MIN_MANUFACTURING_YEAR}.
     */
    private static void validateManufacturingYear(int year) {
        if (year < MIN_MANUFACTURING_YEAR) {
            throw new IllegalArgumentException(
                    String.format(
                            "Năm sản xuất không hợp lệ: %d. Phải >= %d.",
                            year, MIN_MANUFACTURING_YEAR));
        }
    }

    /**
     * Kiểm tra số km đã đi hợp lệ.
     *
     * @param mileage Số km cần kiểm tra.
     *
     * @throws IllegalArgumentException nếu mileage âm.
     */
    private static void validateMileage(double mileage) {
        if (mileage < MIN_MILEAGE) {
            throw new IllegalArgumentException(
                    "Số km đã đi (ODO) không được âm: " + mileage);
        }
    }
}