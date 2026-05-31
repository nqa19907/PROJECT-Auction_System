package auction_system.common.models.items;

import auction_system.common.models.auctions.Entity;

/**
 * Lớp trừu tượng đại diện cho một sản phẩm đấu giá.
 */
public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;

    private String itemName;
    private String description;
    private double startPrice;
    private double currentPrice;
    private double bidStep;
    private String sellerId;
    private String imagePath;
    protected String category;

    /**
     * Khởi tạo một sản phẩm đấu giá mới.
     *
     * @param itemName    Tên sản phẩm.
     * @param description Mô tả sản phẩm.
     * @param startPrice  Giá khởi điểm.
     * @param sellerId    ID của người bán.
     */

    public Item(String itemName, String description, double startPrice, String sellerId) {
        super();
        this.itemName = itemName;
        this.description = description;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.bidStep = startPrice;
        this.sellerId = sellerId;

    }

    public abstract String getCategory();

    public void setCategory(String category) {
        this.category = category;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public double getBidStep() {
        return bidStep <= 0 ? startPrice : bidStep;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStartPrice(double startPrice) {
        this.startPrice = startPrice;
    }

    public void setBidStep(final double bidStep) {
        this.bidStep = bidStep;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    // Xử lý đường dẫn ảnh sản phẩm để client có thể hiển thị ảnh động.
    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(final String imagePath) {
        this.imagePath = imagePath == null ? "" : imagePath.trim();
    }

    @Override
    public String toString() {
        return super.toString() + " -> Item{"
                + "itemName='" + itemName + '\''
                + ", description='" + description + '\''
                + ", startPrice=" + startPrice
                + ", currentPrice=" + currentPrice
                + ", bidStep=" + bidStep
                + ", sellerId='" + sellerId + '\''
                + ", imagePath='" + imagePath + '\''
                + '}';
    }
}
