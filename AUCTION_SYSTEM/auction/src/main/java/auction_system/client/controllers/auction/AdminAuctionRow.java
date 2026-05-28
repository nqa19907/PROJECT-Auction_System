package auction_system.client.controllers.auction;

import javafx.beans.property.SimpleStringProperty;

/**
 * DTO hiển thị một dòng phiên đấu giá trên bảng quản trị.
 */
public class AdminAuctionRow {
    private final SimpleStringProperty id;
    private final SimpleStringProperty productName;
    private final SimpleStringProperty seller;
    private final SimpleStringProperty currentPrice;
    private final SimpleStringProperty status;

    /**
     * Tạo dữ liệu hiển thị cho một phiên đấu giá.
     *
     * @param id id phiên đấu giá
     * @param productName tên sản phẩm
     * @param seller tên người bán
     * @param currentPrice giá hiện tại
     * @param status trạng thái phiên
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

    public String getId() {
        return id.get();
    }

    public String getProductName() {
        return productName.get();
    }

    public String getSeller() {
        return seller.get();
    }

    public String getCurrentPrice() {
        return currentPrice.get();
    }

    public String getStatus() {
        return status.get();
    }
}
