package auction_system.common.dto;

import auction_system.common.models.Item;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO (Data Transfer Object) trừu tượng đại diện cho thông tin một sản phẩm đấu giá.
 *
 * <p><b>Mục đích trong kiến trúc Client–Server:</b></p>
 * <ul>
 *   <li>Được serialize và truyền qua mạng (Socket / RMI) giữa Server và Client.</li>
 *   <li>Tách biệt hoàn toàn khỏi domain model {@link Item} — Client không cần
 *       biết gì về logic nghiệp vụ phía Server.</li>
 *   <li>Mỗi loại Item (Art, Electronic, Vehicle) sẽ có DTO con riêng
 *       kế thừa lớp này.</li>
 * </ul>
 *
 * <p><b>Nguyên tắc thiết kế được áp dụng:</b></p>
 * <ul>
 *   <li><b>SRP</b> – Lớp này chỉ chịu trách nhiệm mang dữ liệu, không chứa logic.</li>
 *   <li><b>OCP</b> – Có thể mở rộng bằng cách tạo lớp DTO con mà không sửa lớp này.</li>
 *   <li><b>LSP</b> – Mọi lớp con đều có thể thay thế {@code ItemDTO} mà không
 *       làm vỡ hệ thống.</li>
 *   <li><b>Serializable</b> – Bắt buộc để truyền qua ObjectOutputStream / Socket.</li>
 * </ul>
 *
 * <p><b>Cách sử dụng điển hình (real-time auction):</b></p>
 * <pre>{@code
 * // Server side:
 * ArtDTO dto = ArtDTO.fromItem((Art) item);
 * outputStream.writeObject(dto);        // gửi qua mạng
 *
 * // Client side:
 * ItemDTO received = (ItemDTO) inputStream.readObject();
 * System.out.println(received.getDisplaySummary());
 * }</pre>
 */
public abstract class ItemDTO implements Serializable {

    // -------------------------------------------------------------------------
    // Checkstyle: serialVersionUID bắt buộc cho mọi lớp Serializable
    // -------------------------------------------------------------------------
    private static final long serialVersionUID = 1L;

    // -------------------------------------------------------------------------
    // Checkstyle: hằng số phải là UPPER_SNAKE_CASE, khai báo private static final
    // -------------------------------------------------------------------------
    /** Giá trị mặc định khi giá chưa được thiết lập. */
    private static final double DEFAULT_PRICE = 0.0;

    // -------------------------------------------------------------------------
    // Checkstyle: field phải là private, không được là public/protected trực tiếp
    // -------------------------------------------------------------------------
    private final String id;
    private final String itemType;
    private final String itemName;
    private final String description;
    private final double startPrice;

    /**
     * Giá hiện tại — volatile để hỗ trợ cập nhật real-time từ nhiều luồng
     * (Server broadcast giá mới, Swing EDT cập nhật UI).
     *
     * <p>Lưu ý: Dùng {@code volatile} đảm bảo visibility giữa các thread,
     * nhưng nếu cần atomicity thì phải dùng synchronized block ở tầng gọi.</p>
     */
    private volatile double currentPrice;

    private final String sellerId;
    private final String condition;
    private final String imagePath;
    private final LocalDateTime createdAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Constructor mặc định cho phép Jackson / Kryo deserialize
     * (cần thiết khi nhận dữ liệu từ mạng hoặc database).
     */
    protected ItemDTO() {
        this.id = null;
        this.itemType = null;
        this.itemName = null;
        this.description = null;
        this.startPrice = DEFAULT_PRICE;
        this.currentPrice = DEFAULT_PRICE;
        this.sellerId = null;
        this.condition = null;
        this.imagePath = null;
        this.createdAt = null;
    }

    /**
     * Constructor đầy đủ tham số — dùng khi tạo DTO từ model hoặc database.
     *
     * <p>Thiết kế: constructor là {@code protected} để chỉ lớp con và
     * factory method được phép tạo instance, tránh tạo ItemDTO "rỗng"
     * từ bên ngoài gói.</p>
     *
     * @param id           ID duy nhất của sản phẩm (UUID), không được null.
     * @param itemType     Loại sản phẩm (e.g. "Art", "Electronic", "Vehicle").
     * @param itemName     Tên sản phẩm, không được rỗng.
     * @param description  Mô tả sản phẩm.
     * @param startPrice   Giá khởi điểm, phải >= 0.
     * @param currentPrice Giá hiện tại, phải >= startPrice.
     * @param sellerId     ID người bán, không được null.
     * @param condition    Tình trạng sản phẩm.
     * @param imagePath    Đường dẫn ảnh (có thể null nếu chưa có ảnh).
     * @param createdAt    Thời điểm tạo sản phẩm.
     * @throws IllegalArgumentException nếu id, itemName hoặc sellerId không hợp lệ,
     *                                  hoặc giá trị giá âm.
     */
    protected ItemDTO(
            String id,
            String itemType,
            String itemName,
            String description,
            double startPrice,
            double currentPrice,
            String sellerId,
            String condition,
            String imagePath,
            LocalDateTime createdAt) {

        // Checkstyle: validate tham số ngay trong constructor thay vì để NPE âm thầm
        validateId(id);
        validateItemName(itemName);
        validateSellerId(sellerId);
        validatePrices(startPrice, currentPrice);

        this.id = id;
        this.itemType = itemType;
        this.itemName = itemName;
        this.description = description;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.sellerId = sellerId;
        this.condition = condition;
        this.imagePath = imagePath;
        this.createdAt = createdAt;
    }

    // -------------------------------------------------------------------------
    // Abstract methods — lớp con bắt buộc implement (OCP + SRP)
    // -------------------------------------------------------------------------

    /**
     * Trả về chuỗi tóm tắt thông tin để hiển thị trên danh sách đấu giá.
     *
     * <p>Mỗi loại item (Art, Electronic, Vehicle) tự quyết định
     * cách hiển thị thông tin đặc thù của mình.</p>
     *
     * @return Chuỗi mô tả ngắn gọn, không null.
     */
    public abstract String getDisplaySummary();

    // -------------------------------------------------------------------------
    // Real-time update method
    // -------------------------------------------------------------------------

    /**
     * Cập nhật giá hiện tại khi nhận broadcast từ Server.
     *
     * <p>Phương thức này được gọi bởi luồng nhận dữ liệu mạng khi
     * có bid mới thắng. {@code volatile} đảm bảo Swing EDT thấy
     * giá trị mới ngay lập tức.</p>
     *
     * @param newPrice Giá mới từ server, phải >= giá hiện tại.
     * @throws IllegalArgumentException nếu newPrice < currentPrice.
     */
    public void updateCurrentPrice(double newPrice) {
        // Checkstyle: không cho phép giá mới thấp hơn giá hiện tại
        if (newPrice < this.currentPrice) {
            throw new IllegalArgumentException(
                    String.format(
                            "Giá mới (%.2f) không được thấp hơn giá hiện tại (%.2f).",
                            newPrice, this.currentPrice));
        }
        this.currentPrice = newPrice;
    }

    // -------------------------------------------------------------------------
    // Getters — không có setter cho các field bất biến (immutable design)
    // -------------------------------------------------------------------------
    // Checkstyle: mỗi getter phải có Javadoc ngắn mô tả ý nghĩa

    /** @return ID duy nhất của sản phẩm (UUID). */
    public String getId() {
        return id;
    }

    /** @return Loại sản phẩm, ví dụ: "Art", "Electronic", "Vehicle". */
    public String getItemType() {
        return itemType;
    }

    /** @return Tên sản phẩm. */
    public String getItemName() {
        return itemName;
    }

    /** @return Mô tả chi tiết sản phẩm. */
    public String getDescription() {
        return description;
    }

    /** @return Giá khởi điểm — bất biến sau khi tạo. */
    public double getStartPrice() {
        return startPrice;
    }

    /**
     * Trả về giá hiện tại của sản phẩm trong phiên đấu giá.
     *
     * <p>Giá trị này có thể thay đổi theo thời gian thực khi có bid mới.</p>
     *
     * @return Giá hiện tại (>= startPrice).
     */
    public double getCurrentPrice() {
        return currentPrice;
    }

    /** @return ID của người bán sản phẩm. */
    public String getSellerId() {
        return sellerId;
    }

    /** @return Tình trạng sản phẩm (ví dụ: "Mới", "Đã qua sử dụng"). */
    public String getCondition() {
        return condition;
    }

    /** @return Đường dẫn hình ảnh sản phẩm, có thể null nếu chưa có ảnh. */
    public String getImagePath() {
        return imagePath;
    }

    /** @return Thời điểm sản phẩm được tạo trong hệ thống. */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // -------------------------------------------------------------------------
    // equals / hashCode — dựa trên id (identity by ID, không phải giá trị)
    // -------------------------------------------------------------------------
    // Checkstyle: equals và hashCode phải được override cùng nhau

    /**
     * So sánh hai DTO theo ID — phù hợp với việc dùng trong Set / Map
     * khi quản lý danh sách item đấu giá.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemDTO)) {
            return false;
        }
        ItemDTO other = (ItemDTO) o;
        return Objects.equals(this.id, other.id);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // -------------------------------------------------------------------------
    // toString — dùng cho logging, không dùng để hiển thị UI
    // -------------------------------------------------------------------------
    // Checkstyle: toString không được chứa thông tin nhạy cảm (sellerId giữ lại
    // vì đây là internal DTO, không expose ra ngoài API public)

    /**
     * Trả về chuỗi mô tả DTO dùng cho logging/debug.
     *
     * <p>Lưu ý: Không dùng phương thức này để hiển thị UI —
     * hãy dùng {@link #getDisplaySummary()} thay thế.</p>
     *
     * @return Chuỗi thông tin DTO.
     */
    @Override
    public String toString() {
        return String.format(
                "%s{id='%s', itemType='%s', itemName='%s', startPrice=%.2f, currentPrice=%.2f}",
                getClass().getSimpleName(),
                id,
                itemType,
                itemName,
                startPrice,
                currentPrice);
    }

    // -------------------------------------------------------------------------
    // Private validation helpers — tách logic validate ra khỏi constructor
    // (SRP: constructor không nên xử lý cả logic validate phức tạp)
    // -------------------------------------------------------------------------

    /**
     * Kiểm tra ID hợp lệ.
     *
     * @param id ID cần kiểm tra.
     * @throws IllegalArgumentException nếu id null hoặc rỗng.
     */
    private static void validateId(String id) {
        // Checkstyle: không dùng == null mà dùng Objects.isNull hoặc điều kiện rõ ràng
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID sản phẩm không được để trống.");
        }
    }

    /**
     * Kiểm tra tên sản phẩm hợp lệ.
     *
     * @param itemName Tên sản phẩm cần kiểm tra.
     * @throws IllegalArgumentException nếu tên null hoặc rỗng.
     */
    private static void validateItemName(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        }
    }

    /**
     * Kiểm tra ID người bán hợp lệ.
     *
     * @param sellerId ID người bán cần kiểm tra.
     * @throws IllegalArgumentException nếu sellerId null hoặc rỗng.
     */
    private static void validateSellerId(String sellerId) {
        if (sellerId == null || sellerId.trim().isEmpty()) {
            throw new IllegalArgumentException("ID người bán không được để trống.");
        }
    }

    /**
     * Kiểm tra giá hợp lệ.
     *
     * @param startPrice   Giá khởi điểm.
     * @param currentPrice Giá hiện tại.
     * @throws IllegalArgumentException nếu giá âm hoặc currentPrice < startPrice.
     */
    private static void validatePrices(double startPrice, double currentPrice) {
        // Checkstyle: mỗi điều kiện validate phải có thông báo lỗi rõ ràng
        if (startPrice < 0) {
            throw new IllegalArgumentException(
                    "Giá khởi điểm không được âm: " + startPrice);
        }
        if (currentPrice < startPrice) {
            throw new IllegalArgumentException(
                    String.format(
                            "Giá hiện tại (%.2f) không được nhỏ hơn giá khởi điểm (%.2f).",
                            currentPrice, startPrice));
        }
    }
}
