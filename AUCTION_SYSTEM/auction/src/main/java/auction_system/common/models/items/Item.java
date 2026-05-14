package auction_system.common.models.items;

import auction_system.common.models.auctions.Entity;

/**
 * Lớp trừu tượng đại diện cho một sản phẩm đấu giá.
 */
public abstract class Item extends Entity {
    private String itemName;
    private String description;
    private double startPrice;
    private double currentPrice;
    private String sellerId;
    private String condition;
    private String imagePath;


    /**
     * Khởi tạo một sản phẩm đấu giá mới.
     *
     * @param itemName    Tên sản phẩm.
     * @param description Mô tả sản phẩm.
     * @param startPrice  Giá khởi điểm.
     * @param sellerId    ID của người bán.
     * @param condition   Tình trạng sản phẩm.
     * @param imagePath   Đường dẫn ảnh minh họa.
     */
    public Item(String itemName, String description, double startPrice, String sellerId, 
                String condition, String imagePath) {
        super();
        this.itemName = itemName;
        this.description = description;
        this.startPrice = startPrice;
        this.currentPrice = startPrice;
        this.sellerId = sellerId;
        this.condition = condition;
        this.imagePath = imagePath;
    }

    /**
     * Lấy thông tin chi tiết của sản phẩm để hiển thị lên giao diện.
     *
     * @return Chuỗi chứa thông tin chi tiết.
     */
    public abstract String getDisplayDetails();

    public double getStartPrice() {
        return startPrice;
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

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    @Override
    public String toString() {
        return super.toString() + " -> Item{"
                + "itemName='" + itemName + '\''
                + ", description='" + description + '\''
                + ", startPrice=" + startPrice
                + ", currentPrice=" + currentPrice
                + ", sellerId='" + sellerId + '\''
                + ", condition='" + condition + '\''
                + ", imagePath='" + imagePath + '\''
                + '}';
    }
}
