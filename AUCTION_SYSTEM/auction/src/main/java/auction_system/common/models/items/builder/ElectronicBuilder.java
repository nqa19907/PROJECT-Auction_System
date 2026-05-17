package auction_system.common.models.items.builder;

import auction_system.common.models.items.Electronic;

/**
 * Lớp Builder giúp khởi tạo đối tượng Electronic.
 */
public class ElectronicBuilder implements Builder<Electronic> {

    private String itemName;
    private String description;
    private double startPrice;
    private double currentPrice;
    private String sellerId;

    public ElectronicBuilder itemName(String itemName) {
        this.itemName = itemName;
        return this;
    }

    public ElectronicBuilder description(String description) {
        this.description = description;
        return this;
    }

    public ElectronicBuilder startPrice(double startPrice) {
        this.startPrice = startPrice;
        return this;
    }

    public ElectronicBuilder currentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
        return this;
    }

    public ElectronicBuilder sellerId(String sellerId) {
        this.sellerId = sellerId;
        return this;
    }

    /**
     * Xây dựng và trả về đối tượng Electronic.
     *
     * @return Đối tượng Electronic đã được khởi tạo.
     */
    @Override
    public Electronic build() {
        Electronic electronic = new Electronic(
                itemName, description, startPrice, sellerId);
        if (this.currentPrice > 0) {
            electronic.setCurrentPrice(this.currentPrice);
        }
        return electronic;
    }
}