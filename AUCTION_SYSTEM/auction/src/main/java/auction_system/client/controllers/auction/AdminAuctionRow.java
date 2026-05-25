package auction_system.client.controllers.auction;

import javafx.beans.property.SimpleStringProperty;

/**
 * DTO hien thi mot dong auction tren bang Admin Dashboard.
 */
public class AdminAuctionRow {

    private final SimpleStringProperty id;
    private final SimpleStringProperty productName;
    private final SimpleStringProperty seller;
    private final SimpleStringProperty currentPrice;
    private final SimpleStringProperty status;

    /**
     * Tao mot dong hien thi auction.
     *
     * @param id id phien dau gia
     * @param productName ten san pham
     * @param seller ten nguoi ban
     * @param currentPrice gia hien tai
     * @param status trang thai phien
     */
    public AdminAuctionRow(
            final String id,
            final String productName,
            final String seller,
            final String currentPrice,
            final String status) {
        this.id = new SimpleStringProperty(id);
        this.productName = new SimpleStringProperty(productName);
        this.seller = new SimpleStringProperty(seller);
        this.currentPrice = new SimpleStringProperty(currentPrice);
        this.status = new SimpleStringProperty(status);
    }

    /**
     * Lay id phien.
     *
     * @return id phien
     */
    public String getId() {
        return id.get();
    }

    /**
     * Lay ten san pham.
     *
     * @return ten san pham
     */
    public String getProductName() {
        return productName.get();
    }

    /**
     * Lay ten nguoi ban.
     *
     * @return ten nguoi ban
     */
    public String getSeller() {
        return seller.get();
    }

    /**
     * Lay gia hien tai.
     *
     * @return gia hien tai da format
     */
    public String getCurrentPrice() {
        return currentPrice.get();
    }

    /**
     * Lay trang thai phien.
     *
     * @return trang thai phien
     */
    public String getStatus() {
        return status.get();
    }
}
