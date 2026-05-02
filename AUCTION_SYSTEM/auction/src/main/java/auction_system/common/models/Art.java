package auction_system.common.models;

/**
 * Lớp đại diện cho sản phẩm đấu giá là một tác phẩm nghệ thuật.
 */
public class Art extends Item {
    private String artistName;
    private String creationYear;
    private boolean hasAuthenticityCertificate;

    /**
     * Khởi tạo một tác phẩm nghệ thuật.
     *
     * @param itemName                   Tên tác phẩm.
     * @param description                Mô tả chi tiết.
     * @param startPrice                 Giá khởi điểm.
     * @param sellerId                   ID của người bán.
     * @param condition                  Tình trạng tác phẩm.
     * @param imagePath                  Đường dẫn hình ảnh.
     * @param artistName                 Tên nghệ sĩ.
     * @param creationYear               Năm sáng tác.
     * @param hasAuthenticityCertificate Có chứng nhận xác thực hay không.
     */
    public Art(String itemName, String description, Double startPrice, String sellerId,
            String condition, String imagePath, String artistName, String creationYear,
            boolean hasAuthenticityCertificate) {
        super(itemName, description, startPrice, sellerId, condition, imagePath);
        this.artistName = artistName;
        this.creationYear = creationYear;
        this.hasAuthenticityCertificate = hasAuthenticityCertificate;
    }

    @Override
    public String getDisplayDetails() {
        String certificate = hasAuthenticityCertificate ? "Đã xác thực" : "Chưa xác thực";
        return String.format("Nghệ thuật: %s | Nghệ sĩ: %s | Năm sáng tác: %s | Chứng nhận: %s",
                getItemName(), this.artistName, this.creationYear, certificate);
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getCreationYear() {
        return creationYear;
    }

    public void setCreationYear(String creationYear) {
        this.creationYear = creationYear;
    }

    @Override
    public String toString() {
        return super.toString() + " -> Art{"
                + "artistName='" + artistName + '\''
                + ", creationYear='" + creationYear + '\''
                + '}';
    }
}
