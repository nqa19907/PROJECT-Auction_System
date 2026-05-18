package auction_system.common.dto;

import auction_system.common.models.Art;
import java.time.LocalDateTime;

/**
 * DTO đại diện cho tác phẩm nghệ thuật (Art) trong quá trình truyền dữ liệu qua mạng.
 *
 * <p><b>Vị trí trong luồng dữ liệu real-time:</b></p>
 * <pre>
 *  [Server]  Art (model) ──fromItem()──▶ ArtDTO ──Socket──▶ [Client]
 *  [Client]  ArtDTO ──toItem()──▶ Art (model, nếu Server cần tái tạo)
 * </pre>
 *
 * <p><b>Nguyên tắc thiết kế:</b></p>
 * <ul>
 *   <li><b>SRP</b> – Chỉ mang dữ liệu đặc thù của Art, không chứa logic nghiệp vụ.</li>
 *   <li><b>LSP</b> – Có thể thay thế {@link ItemDTO} ở mọi nơi nhận ItemDTO.</li>
 *   <li><b>Static factory</b> – {@code fromItem()} và {@code toItem()} tách biệt
 *       việc chuyển đổi khỏi constructor, tránh constructor phình to.</li>
 * </ul>
 */
public final class ArtDTO extends ItemDTO {

    // -------------------------------------------------------------------------
    // Checkstyle: serialVersionUID bắt buộc — phải khác với lớp cha
    // -------------------------------------------------------------------------
    private static final long serialVersionUID = 2L;

    // -------------------------------------------------------------------------
    // Checkstyle: field private final — dữ liệu đặc thù của Art không thay đổi
    //             sau khi DTO được tạo và gửi đi
    // -------------------------------------------------------------------------
    private final String artistName;
    private final String creationYear;
    private final boolean hasAuthenticityCertificate;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Constructor mặc định — cần thiết cho Java deserialization qua ObjectInputStream.
     *
     * <p>Không dùng trực tiếp; hãy dùng {@link #fromItem(Art)} hoặc
     * {@link #of(String, String, String, String, double, double,
     * String, String, LocalDateTime, String, String, boolean)}.</p>
     */
    public ArtDTO() {
        super();
        this.artistName = null;
        this.creationYear = null;
        this.hasAuthenticityCertificate = false;
    }

    /**
     * Constructor đầy đủ — dùng nội bộ bởi static factory methods.
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
     * @param artistName                 Tên nghệ sĩ.
     * @param creationYear               Năm sáng tác.
     * @param hasAuthenticityCertificate Có chứng nhận xác thực hay không.
     */
    private ArtDTO(
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

        // Checkstyle: gọi super constructor đầu tiên, truyền đủ tham số
        super(id, "Art", itemName, description,
                startPrice, currentPrice,
                sellerId, condition, imagePath, createdAt);

        // Checkstyle: validate trước khi gán — không để artistName rỗng
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
    // Static factory methods (thay thế constructor public phức tạp)
    // -------------------------------------------------------------------------

    /**
     * Tạo {@code ArtDTO} từ domain model {@link Art}.
     *
     * <p>Dùng phía <b>Server</b> trước khi gửi dữ liệu xuống Client qua Socket.</p>
     *
     * <p>Ví dụ:</p>
     * <pre>{@code
     * Art art = artRepository.findById(id);
     * ArtDTO dto = ArtDTO.fromItem(art);
     * objectOutputStream.writeObject(dto);
     * }</pre>
     *
     * @param art Domain model, không được null.
     * @return {@code ArtDTO} chứa đầy đủ thông tin của {@code art}.
     * @throws IllegalArgumentException nếu {@code art} là null.
     */
    public static ArtDTO fromItem(Art art) {
        // Checkstyle: guard clause — kiểm tra null ngay đầu method
        if (art == null) {
            throw new IllegalArgumentException("Art model không được null khi tạo ArtDTO.");
        }
        return new ArtDTO(
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
     * <p>Dùng phía <b>Server</b> nếu cần xử lý nghiệp vụ sau khi nhận DTO
     * từ một service khác, hoặc phục vụ việc test / tái tạo trạng thái.</p>
     *
     * <p><b>Lưu ý:</b> {@code Art} được tạo ra từ phương thức này sẽ có
     * {@code createdAt} và {@code id} gốc từ DTO, không phải giá trị mới.</p>
     *
     * @return {@link Art} tương ứng với DTO này.
     */
    public Art toItem() {
        // Checkstyle: không dùng constructor Art trực tiếp mà qua ArtBuilder
        // để đảm bảo tính nhất quán với phần còn lại của codebase
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
        // Cập nhật giá hiện tại nếu khác giá khởi điểm (đã có bid)
        if (getCurrentPrice() > getStartPrice()) {
            art.setCurrentPrice(getCurrentPrice());
        }
        return art;
    }

    // -------------------------------------------------------------------------
    // Implement abstract method từ ItemDTO
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Định dạng: {@code "Nghệ thuật | <Tên> | Nghệ sĩ: <Nghệ sĩ> | Năm: <Năm> | <Chứng nhận>"}
     */
    @Override
    public String getDisplaySummary() {
        // Checkstyle: dùng String.format thay vì concatenation nhiều dòng
        String certificate = hasAuthenticityCertificate ? "✓ Đã xác thực" : "✗ Chưa xác thực";
        return String.format(
                "Nghệ thuật | %s | Nghệ sĩ: %s | Năm: %s | %s",
                getItemName(), artistName, creationYear, certificate);
    }

    // -------------------------------------------------------------------------
    // Getters — không có setter (immutable sau khi tạo)
    // -------------------------------------------------------------------------

    /** @return Tên nghệ sĩ sáng tác tác phẩm. */
    public String getArtistName() {
        return artistName;
    }

    /** @return Năm sáng tác tác phẩm (dạng chuỗi, ví dụ "1889", "khoảng 1920"). */
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
    // toString — logging/debug, không dùng cho UI
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Thêm thông tin đặc thù của Art vào chuỗi từ lớp cha.</p>
     */
    @Override
    public String toString() {
        return super.toString()
                + String.format(
                " -> Art{artistName='%s', creationYear='%s', certified=%b}",
                artistName, creationYear, hasAuthenticityCertificate);
    }
}