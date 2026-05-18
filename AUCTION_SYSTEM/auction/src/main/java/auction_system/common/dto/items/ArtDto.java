package auction_system.common.dto.items;

import auction_system.common.models.Art;
import java.time.LocalDateTime;

/**
 * DTO đại diện cho tác phẩm nghệ thuật (Art) trong quá trình truyền dữ liệu qua mạng.
 *
 * <p><b>Vị trí trong luồng dữ liệu real-time:</b></p>
 * <pre>
 *  [Server]  Art (model) ──fromItem()──▶ ArtDto ──Socket──▶ [Client]
 *  [Client]  ArtDto ──toItem()──▶ Art (model, nếu Server cần tái tạo)
 * </pre>
 *
 * <p><b>Nguyên tắc thiết kế:</b></p>
 * <ul>
 *   <li><b>SRP</b> – Chỉ mang dữ liệu đặc thù của Art, không chứa logic nghiệp vụ.</li>
 *   <li><b>LSP</b> – Có thể thay thế {@link ItemDto} ở mọi nơi nhận ItemDto.</li>
 *   <li><b>Static factory</b> – {@code fromItem()} và {@code toItem()} tách biệt
 *       việc chuyển đổi khỏi constructor, tránh constructor phình to.</li>
 * </ul>
 */
public final class ArtDto extends ItemDto {

    /**
     * serialVersionUID riêng của ArtDto — khác với lớp cha để tránh nhầm lẫn
     * khi deserialize qua ObjectInputStream.
     */
    private static final long serialVersionUID = 2L;

    private final String artistName;
    private final String creationYear;
    private final boolean hasAuthenticityCertificate;

    /**
     * Constructor mặc định — cần thiết cho Java deserialization qua ObjectInputStream.
     *
     * <p>Không dùng trực tiếp; hãy dùng {@link #fromItem(Art)}.</p>
     */
    public ArtDto() {
        super();
        this.artistName = null;
        this.creationYear = null;
        this.hasAuthenticityCertificate = false;
    }

    /**
     * Constructor đầy đủ — chỉ dùng nội bộ bởi static factory {@link #fromItem(Art)}.
     *
     * @param id                         ID duy nhất (UUID).
     * @param itemName                   Tên tác phẩm.
     * @param description                Mô tả tác phẩm.
     * @param startPrice                 Giá khởi điểm.
     * @param currentPrice               Giá hiện tại.
     * @param sellerId                   ID người bán.
     * @param condition                  Tình trạng tác phẩm.
     * @param imagePath                  Đường dẫn hình ảnh.
     * @param createdAt                  Thời điểm tạo.
     * @param artistName                 Tên nghệ sĩ, không được rỗng.
     * @param creationYear               Năm sáng tác, không được rỗng.
     * @param hasAuthenticityCertificate Có chứng nhận xác thực hay không.
     *
     * @throws IllegalArgumentException nếu artistName hoặc creationYear rỗng.
     */
    private ArtDto(
            String id,
            String itemName,
            String description,
            double startPrice,
            double currentPrice,
            String sellerId,
            String condition,
            String imagePath,
            LocalDateTime createdAt,
            String artistName,
            String creationYear,
            boolean hasAuthenticityCertificate) {

        super(id, "Art", itemName, description,
                startPrice, currentPrice,
                sellerId, condition, imagePath, createdAt);

        if (artistName == null || artistName.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên nghệ sĩ không được để trống.");
        }
        if (creationYear == null || creationYear.trim().isEmpty()) {
            throw new IllegalArgumentException("Năm sáng tác không được để trống.");
        }

        this.artistName = artistName;
        this.creationYear = creationYear;
        this.hasAuthenticityCertificate = hasAuthenticityCertificate;
    }

    // -------------------------------------------------------------------------
    // Static factory methods
    // -------------------------------------------------------------------------

    /**
     * Tạo {@code ArtDto} từ domain model {@link Art}.
     *
     * <p>Dùng phía <b>Server</b> trước khi gửi dữ liệu xuống Client qua Socket.</p>
     *
     * <p>Ví dụ sử dụng:</p>
     * <pre>{@code
     * Art art = artRepository.findById(id);
     * ArtDto dto = ArtDto.fromItem(art);
     * objectOutputStream.writeObject(dto);
     * }</pre>
     *
     * @param art Domain model nguồn, không được null.
     *
     * @return {@code ArtDto} chứa đầy đủ thông tin của {@code art}.
     *
     * @throws IllegalArgumentException nếu {@code art} là null.
     */
    public static ArtDto fromItem(Art art) {
        if (art == null) {
            throw new IllegalArgumentException("Art model không được null khi tạo ArtDto.");
        }
        return new ArtDto(
                art.getId(),
                art.getItemName(),
                art.getDescription(),
                art.getStartPrice(),
                art.getCurrentPrice(),
                art.getSellerId(),
                art.getCondition(),
                art.getImagePath(),
                art.getCreatedAt(),
                art.getArtistName(),
                art.getCreationYear(),
                art.isHasAuthenticityCertificate());
    }

    /**
     * Tái tạo domain model {@link Art} từ DTO này.
     *
     * <p>Dùng phía Server khi cần xử lý nghiệp vụ sau khi nhận DTO từ service khác,
     * hoặc phục vụ việc test và tái tạo trạng thái.</p>
     *
     * @return {@link Art} tương ứng với dữ liệu của DTO này.
     */
    public Art toItem() {
        Art art = new Art(
                getItemName(),
                getDescription(),
                getStartPrice(),
                getSellerId(),
                getCondition(),
                getImagePath(),
                this.artistName,
                this.creationYear,
                this.hasAuthenticityCertificate);
        if (getCurrentPrice() > getStartPrice()) {
            art.setCurrentPrice(getCurrentPrice());
        }
        return art;
    }

    // -------------------------------------------------------------------------
    // Implement abstract method từ ItemDto
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Định dạng hiển thị:
     * {@code "Nghệ thuật | <Tên> | Nghệ sĩ: <X> | Năm: <Y> | <Chứng nhận>"}</p>
     */
    @Override
    public String getDisplaySummary() {
        String certificate = hasAuthenticityCertificate ? "Đã xác thực" : "Chưa xác thực";
        return String.format(
                "Nghệ thuật | %s | Nghệ sĩ: %s | Năm: %s | %s",
                getItemName(), artistName, creationYear, certificate);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    /**
     * Trả về tên nghệ sĩ sáng tác tác phẩm.
     *
     * @return Tên nghệ sĩ, không null và không rỗng.
     */
    public String getArtistName() {
        return artistName;
    }

    /**
     * Trả về năm sáng tác tác phẩm.
     *
     * <p>Giá trị là chuỗi để hỗ trợ các trường hợp như {@code "khoảng 1920"}
     * hoặc các giai đoạn nghệ thuật không rõ năm cụ thể.</p>
     *
     * @return Năm sáng tác dạng chuỗi, không null.
     */
    public String getCreationYear() {
        return creationYear;
    }

    /**
     * Kiểm tra tác phẩm có chứng nhận xác thực hay không.
     *
     * @return {@code true} nếu có chứng nhận, {@code false} nếu chưa.
     */
    public boolean isHasAuthenticityCertificate() {
        return hasAuthenticityCertificate;
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    /**
     * Trả về chuỗi mô tả ArtDto dùng cho logging và debug.
     *
     * <p>Để hiển thị trên UI, dùng {@link #getDisplaySummary()} thay thế.</p>
     *
     * @return Chuỗi thông tin rút gọn bao gồm thông tin lớp cha và thông tin Art.
     */
    @Override
    public String toString() {
        return super.toString()
                + String.format(
                " -> Art{artistName='%s', creationYear='%s', certified=%b}",
                artistName, creationYear, hasAuthenticityCertificate);
    }
}