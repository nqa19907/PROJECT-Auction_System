package auction_system.common.dto.items;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO (Data Transfer Object) trừu tượng đại diện cho thông tin một sản phẩm đấu giá.
 *
 * <p><b>Mục đích trong kiến trúc Client–Server:</b></p>
 * <ul>
 *   <li>Được serialize và truyền qua mạng (Socket / ObjectStream) giữa Server và Client.</li>
 *   <li>Tách biệt hoàn toàn khỏi domain model {@code Item} — Client không cần
 *       biết gì về logic nghiệp vụ phía Server.</li>
 *   <li>Mỗi loại Item (Art, Electronic, Vehicle) có DTO con riêng kế thừa lớp này.</li>
 * </ul>
 *
 * <p><b>Nguyên tắc SOLID được áp dụng:</b></p>
 * <ul>
 *   <li><b>SRP</b> – Lớp này chỉ chịu trách nhiệm mang dữ liệu, không chứa logic.</li>
 *   <li><b>OCP</b> – Mở rộng bằng cách tạo lớp DTO con, không sửa lớp này.</li>
 *   <li><b>LSP</b> – Mọi lớp con đều thay thế được {@code ItemDto} mà không vỡ hệ thống.</li>
 * </ul>
 */
public abstract class ItemDto implements Serializable {

    /**
     * serialVersionUID bắt buộc cho mọi lớp Serializable.
     * Phải khai báo tường minh để tránh InvalidClassException khi thêm field mới.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Giá trị mặc định khi giá chưa được thiết lập.
     */
    private static final double DEFAULT_PRICE = 0.0;

    private final String id;
    private final String itemType;
    private final String itemName;
    private final String description;
    private final double startPrice;

    /**
     * Giá hiện tại — volatile để đảm bảo visibility khi cập nhật real-time từ nhiều luồng.
     *
     * <p>Network thread cập nhật giá mới khi nhận broadcast từ Server;
     * Swing EDT đọc giá để render UI. {@code volatile} đảm bảo hai luồng
     * thấy cùng một giá trị.</p>
     */
    private volatile double currentPrice;

    private final String sellerId;
    private final String condition;
    private final String imagePath;
    private final LocalDateTime createdAt;

    /**
     * Constructor mặc định cho phép Java Serialization deserialize đối tượng.
     *
     * <p>Không dùng trực tiếp — hãy dùng static factory {@code fromItem()} ở lớp con.</p>
     */
    protected ItemDto() {
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
     * Constructor đầy đủ tham số — chỉ dùng bởi lớp con thông qua {@code super(...)}.
     *
     * @param id           ID duy nhất của sản phẩm (UUID), không được null.
     * @param itemType     Loại sản phẩm (ví dụ: "Art", "Electronic", "Vehicle").
     * @param itemName     Tên sản phẩm, không được rỗng.
     * @param description  Mô tả sản phẩm.
     * @param startPrice   Giá khởi điểm, phải >= 0.
     * @param currentPrice Giá hiện tại, phải >= startPrice.
     * @param sellerId     ID người bán, không được null.
     * @param condition    Tình trạng sản phẩm.
     * @param imagePath    Đường dẫn ảnh, có thể null nếu chưa có ảnh.
     * @param createdAt    Thời điểm tạo sản phẩm.
     *
     * @throws IllegalArgumentException nếu id, itemName, sellerId không hợp lệ
     *                                  hoặc giá trị giá âm / không nhất quán.
     */
    protected ItemDto(
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
    // Abstract method — lớp con bắt buộc implement (OCP)
    // -------------------------------------------------------------------------

    /**
     * Trả về chuỗi tóm tắt thông tin để hiển thị trên danh sách đấu giá.
     *
     * <p>Mỗi loại item tự quyết định cách hiển thị thông tin đặc thù của mình,
     * tránh client phải dùng {@code instanceof} để phân biệt loại.</p>
     *
     * @return Chuỗi mô tả ngắn gọn, không null.
     */
    public abstract String getDisplaySummary();

    // -------------------------------------------------------------------------
    // Real-time update
    // -------------------------------------------------------------------------

    /**
     * Cập nhật giá hiện tại khi nhận broadcast giá mới từ Server.
     *
     * <p>Được gọi bởi luồng nhận dữ liệu mạng khi có bid mới thắng.
     * Trong đấu giá, giá chỉ được phép tăng — không bao giờ giảm.</p>
     *
     * @param newPrice Giá mới từ server, phải {@code >= currentPrice}.
     *
     * @throws IllegalArgumentException nếu {@code newPrice < currentPrice}.
     */
    public void updateCurrentPrice(double newPrice) {
        if (newPrice < this.currentPrice) {
            throw new IllegalArgumentException(
                    String.format(
                            "Giá mới (%.2f) không được thấp hơn giá hiện tại (%.2f).",
                            newPrice, this.currentPrice));
        }
        this.currentPrice = newPrice;
    }

    // -------------------------------------------------------------------------
    // Getters
    // Checkstyle: SingleLineJavadoc cấm dùng /** @return ... */ một dòng.
    // Phải viết đúng dạng multi-line với summary sentence + dòng trống + @return.
    // -------------------------------------------------------------------------

    /**
     * Trả về ID duy nhất của sản phẩm.
     *
     * @return ID dạng UUID string, không null sau khi khởi tạo đầy đủ.
     */
    public String getId() {
        return id;
    }

    /**
     * Trả về loại sản phẩm.
     *
     * @return Chuỗi phân loại, ví dụ: {@code "Art"}, {@code "Electronic"}, {@code "Vehicle"}.
     */
    public String getItemType() {
        return itemType;
    }

    /**
     * Trả về tên sản phẩm.
     *
     * @return Tên sản phẩm, không null và không rỗng.
     */
    public String getItemName() {
        return itemName;
    }

    /**
     * Trả về mô tả chi tiết sản phẩm.
     *
     * @return Chuỗi mô tả, có thể null nếu chưa có mô tả.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Trả về giá khởi điểm — bất biến sau khi DTO được tạo.
     *
     * @return Giá khởi điểm, luôn {@code >= 0}.
     */
    public double getStartPrice() {
        return startPrice;
    }

    /**
     * Trả về giá hiện tại của sản phẩm trong phiên đấu giá.
     *
     * <p>Giá trị này có thể thay đổi theo thời gian thực khi có bid mới.
     * Dùng {@link #updateCurrentPrice(double)} để cập nhật.</p>
     *
     * @return Giá hiện tại, luôn {@code >= startPrice}.
     */
    public double getCurrentPrice() {
        return currentPrice;
    }

    /**
     * Trả về ID của người bán sản phẩm.
     *
     * @return ID người bán, không null.
     */
    public String getSellerId() {
        return sellerId;
    }

    /**
     * Trả về tình trạng sản phẩm.
     *
     * @return Chuỗi mô tả tình trạng, ví dụ: {@code "Mới"}, {@code "Đã qua sử dụng"}.
     */
    public String getCondition() {
        return condition;
    }

    /**
     * Trả về đường dẫn hình ảnh sản phẩm.
     *
     * @return Đường dẫn ảnh, có thể null nếu chưa có ảnh.
     */
    public String getImagePath() {
        return imagePath;
    }

    /**
     * Trả về thời điểm sản phẩm được tạo trong hệ thống.
     *
     * @return Thời điểm tạo dạng {@link LocalDateTime}, không null.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // -------------------------------------------------------------------------
    // equals / hashCode — theo id (identity by ID)
    // -------------------------------------------------------------------------

    /**
     * So sánh hai DTO theo ID — phù hợp dùng trong {@code Set} hoặc {@code Map}.
     *
     * @param obj Đối tượng cần so sánh.
     *
     * @return {@code true} nếu cùng ID, {@code false} nếu khác.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ItemDto)) {
            return false;
        }
        ItemDto other = (ItemDto) obj;
        return Objects.equals(this.id, other.id);
    }

    /**
     * Trả về hash code dựa trên ID của DTO.
     *
     * @return Hash code của {@code id}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // -------------------------------------------------------------------------
    // toString — dùng cho logging/debug, không dùng để hiển thị UI
    // -------------------------------------------------------------------------

    /**
     * Trả về chuỗi mô tả DTO dùng cho logging và debug.
     *
     * <p>Để hiển thị trên UI, dùng {@link #getDisplaySummary()} thay thế.</p>
     *
     * @return Chuỗi thông tin rút gọn của DTO.
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
    // Private validation helpers
    // -------------------------------------------------------------------------

    /**
     * Kiểm tra ID hợp lệ — không null và không rỗng.
     *
     * @param id ID cần kiểm tra.
     *
     * @throws IllegalArgumentException nếu id null hoặc rỗng.
     */
    private static void validateId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID sản phẩm không được để trống.");
        }
    }

    /**
     * Kiểm tra tên sản phẩm hợp lệ — không null và không rỗng.
     *
     * @param itemName Tên sản phẩm cần kiểm tra.
     *
     * @throws IllegalArgumentException nếu tên null hoặc rỗng.
     */
    private static void validateItemName(String itemName) {
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống.");
        }
    }

    /**
     * Kiểm tra ID người bán hợp lệ — không null và không rỗng.
     *
     * @param sellerId ID người bán cần kiểm tra.
     *
     * @throws IllegalArgumentException nếu sellerId null hoặc rỗng.
     */
    private static void validateSellerId(String sellerId) {
        if (sellerId == null || sellerId.trim().isEmpty()) {
            throw new IllegalArgumentException("ID người bán không được để trống.");
        }
    }

    /**
     * Kiểm tra tính nhất quán giữa startPrice và currentPrice.
     *
     * @param startPrice   Giá khởi điểm.
     * @param currentPrice Giá hiện tại.
     *
     * @throws IllegalArgumentException nếu giá âm hoặc currentPrice {@code < startPrice}.
     */
    private static void validatePrices(double startPrice, double currentPrice) {
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