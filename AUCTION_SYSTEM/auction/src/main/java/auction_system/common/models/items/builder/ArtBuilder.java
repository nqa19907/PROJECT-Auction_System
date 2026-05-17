package auction_system.common.models.items.builder;

import auction_system.common.models.items.Art;

/**
 * Lớp Builder giúp khởi tạo đối tượng Art.
 */
public class ArtBuilder implements Builder<Art> {
    private String itemName;
    private String description;
    private double startPrice;
    private double currentPrice;
    private String sellerId;

    public ArtBuilder itemName(String itemName) {
        this.itemName = itemName;
        return this;
    }

    public ArtBuilder description(String description) {
        this.description = description;
        return this;
    }

    public ArtBuilder startPrice(double startPrice) {
        this.startPrice = startPrice;
        return this;
    }

    public ArtBuilder currentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
        return this;
    }

    public ArtBuilder sellerId(String sellerId) {
        this.sellerId = sellerId;
        return this;
    }

    /**
     * Xây dựng và trả về đối tượng Art.
     *
     * @return Đối tượng Art đã được khởi tạo.
     */
    @Override
    public Art build() {
        Art art = new Art(
                itemName, description, startPrice, sellerId);
        if (this.currentPrice > 0) {
            art.setCurrentPrice(this.currentPrice);
        }
        return art;
    }

}
