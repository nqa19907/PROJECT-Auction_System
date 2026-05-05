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
     * @param itemName Tên tác phẩm.
     * @param description Mô tả chi tiết.
     * @param startPrice Giá khởi điểm.
     * @param sellerId ID của người bán.
     * @param condition Tình trạng tác phẩm.
     * @param imagePath Đường dẫn hình ảnh.
     * @param artistName Tên nghệ sĩ.
     * @param creationYear Năm sáng tác.
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

    /**
     * Lớp Builder giúp khởi tạo đối tượng Art.
     */
    public static class Builder {

        private String itemName;
        private String description;
        private double startPrice;
        private double currentPrice;
        private String sellerId;
        private String condition;
        private String imagePath;
        private String artistName;
        private String creationYear;
        private boolean hasAuthenticityCertificate;

        public Builder itemName(String itemName) {
            this.itemName = itemName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder startPrice(double startPrice) {
            this.startPrice = startPrice;
            return this;
        }

        public Builder currentPrice(double currentPrice) {
            this.currentPrice = currentPrice;
            return this;
        }

        public Builder sellerId(String sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public Builder condition(String condition) {
            this.condition = condition;
            return this;
        }

        public Builder imagePath(String imagePath) {
            this.imagePath = imagePath;
            return this;
        }

        public Builder artistName(String artistName) {
            this.artistName = artistName;
            return this;
        }

        public Builder creationYear(String creationYear) {
            this.creationYear = creationYear;
            return this;
        }

        public Builder hasAuthenticityCertificate(boolean hasAuthenticityCertificate) {
            this.hasAuthenticityCertificate = hasAuthenticityCertificate;
            return this;
        }

        /**
         * Xây dựng và trả về đối tượng Art.
         *
         * @return Đối tượng Art đã được khởi tạo.
         */
        public Art build() {
            Art art = new Art(
                    itemName, description, startPrice, sellerId, condition, imagePath,
                    artistName, creationYear, hasAuthenticityCertificate);
            if (this.currentPrice > 0) {
                art.setCurrentPrice(this.currentPrice);
            }
            return art;
        }
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

    public boolean isHasAuthenticityCertificate() {
        return hasAuthenticityCertificate;
    }

    public void setHasAuthenticityCertificate(boolean hasAuthenticityCertificate) {
        this.hasAuthenticityCertificate = hasAuthenticityCertificate;
    }

    @Override
    public String toString() {
        return super.toString() + " -> Art{"
                + "artistName='" + artistName + '\''
                + ", creationYear='" + creationYear + '\''
                + '}';
    }
}
