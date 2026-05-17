package auction_system.common.dto.items;

import java.io.Serializable;

/**
 * Lớp Dto đại diện cho dữ liệu chuyển đổi của sản phẩm đấu giá (Item).
 */
public abstract class ItemDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String itemName;
    private String description;
    private double startPrice;
    private double currentPrice;
    private String sellerId;

    /**
     * Khởi tạo một đối tượng ItemDto trống.
     */
    public ItemDto() {
    }

    /**
     * Khởi tạo một đối tượng ItemDto với thông tin cơ bản.
     *
     * @param id          ID của sản phẩm.
     * @param itemName    Tên sản phẩm.
     * @param description Mô tả chi tiết.
     * @param startPrice  Giá khởi điểm.
     * @param sellerId    ID của người bán.
     */
    public ItemDto(String id, String itemName, String description, double startPrice,
            String sellerId) {
        this.id = id;
        this.itemName = itemName;
        this.description = description;
        this.startPrice = startPrice;
        this.currentPrice = startPrice; // Khởi tạo giá hiện tại bằng giá khởi điểm
        this.sellerId = sellerId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public double getStartPrice() {
        return startPrice;
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

    @Override
    public String toString() {
        return "ItemDto{"
                + "id='" + id + '\''
                + ", itemName='" + itemName + '\''
                + ", description='" + description + '\''
                + ", startPrice=" + startPrice
                + ", currentPrice=" + currentPrice
                + ", sellerId='" + sellerId + '\''
                + '}';
    }
}
