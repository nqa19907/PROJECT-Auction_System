package auction_system.common.dto.items;

import java.time.LocalDateTime;

/**
 * DTO đại diện cho sản phẩm đấu giá loại tác phẩm nghệ thuật.
 *
 * <p>Kế thừa {@code ItemDto} và bổ sung các thuộc tính đặc thù:
 * tên nghệ sĩ, năm sáng tác và chứng nhận xác thực.</p>
 */
public class ArtDto extends ItemDto {

    private static final long serialVersionUID = 1L;

    private final String artistName;
    private final String creationYear;
    private final boolean hasAuthenticityCertificate;

    /**
     * Constructor mặc định cho phép Java Serialization deserialize đối tượng.
     */
    protected ArtDto() {
        super();
        this.artistName = null;
        this.creationYear = null;
        this.hasAuthenticityCertificate = false;
    }

    /**
     * Constructor đầy đủ tham số.
     *
     * @param id                         ID duy nhất của sản phẩm.
     * @param itemName                   Tên tác phẩm, không được rỗng.
     * @param description                Mô tả chi tiết.
     * @param startPrice                 Giá khởi điểm, phải {@code >= 0}.
     * @param currentPrice               Giá hiện tại.
     * @param sellerId                   ID người bán.
     * @param condition                  Tình trạng tác phẩm.
     * @param imagePath                  Đường dẫn ảnh, có thể null.
     * @param createdAt                  Thời điểm tạo sản phẩm.
     * @param artistName                 Tên nghệ sĩ sáng tác.
     * @param creationYear               Năm sáng tác, ví dụ: {@code "1990"}.
     * @param hasAuthenticityCertificate {@code true} nếu có chứng nhận xác thực.
     */
    public ArtDto(
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
        super(id, "ART", itemName, description, startPrice, currentPrice,
                sellerId, condition, imagePath, createdAt);
        this.artistName = artistName;
        this.creationYear = creationYear;
        this.hasAuthenticityCertificate = hasAuthenticityCertificate;
    }

    /**
     * Trả về chuỗi tóm tắt thông tin tác phẩm để hiển thị trên giao diện danh sách.
     *
     * @return Chuỗi gồm tên nghệ sĩ, năm sáng tác và trạng thái chứng nhận.
     */
    @Override
    public String getDisplaySummary() {
        String certified = hasAuthenticityCertificate ? "Đã xác thực" : "Chưa xác thực";
        return String.format(
                "Tác phẩm: %s | Nghệ sĩ: %s | Năm: %s | Chứng nhận: %s",
                getItemName(),
                artistName,
                creationYear,
                certified);
    }

    /**
     * Trả về tên nghệ sĩ sáng tác tác phẩm.
     *
     * @return Tên nghệ sĩ, có thể null nếu không có thông tin.
     */
    public String getArtistName() {
        return artistName;
    }

    /**
     * Trả về năm sáng tác của tác phẩm.
     *
     * @return Năm sáng tác dạng chuỗi, có thể null nếu không có thông tin.
     */
    public String getCreationYear() {
        return creationYear;
    }

    /**
     * Kiểm tra tác phẩm có chứng nhận xác thực hay không.
     *
     * @return {@code true} nếu có chứng nhận, {@code false} nếu ngược lại.
     */
    public boolean isHasAuthenticityCertificate() {
        return hasAuthenticityCertificate;
    }

    /**
     * Trả về chuỗi mô tả DTO dùng cho logging và debug.
     *
     * @return Chuỗi thông tin đầy đủ bao gồm dữ liệu từ lớp cha và các trường đặc thù.
     */
    @Override
    public String toString() {
        return super.toString() + String.format(
                " -> ArtDto{artistName='%s', creationYear='%s', certified=%b}",
                artistName,
                creationYear,
                hasAuthenticityCertificate);
    }
}