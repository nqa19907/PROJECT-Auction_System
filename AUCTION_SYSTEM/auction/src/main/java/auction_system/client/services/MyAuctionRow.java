package auction_system.client.services;

/**
 * Dữ liệu 1 dòng trong bảng "Quản lý phiên của tôi".
 */
public class MyAuctionRow {
    private final String id;
    private final String productName;
    private final String currentPrice;
    private final String status;
    private final String endTime;

    /**
     * Khởi tạo dữ liệu cho một dòng trong bảng quản lý phiên.
     *
     * @param id mã phiên đấu giá
     * @param productName tên sản phẩm
     * @param currentPrice giá hiện tại
     * @param status trạng thái phiên
     * @param endTime thời gian kết thúc
     */
    public MyAuctionRow(
            final String id,
            final String productName,
            final String currentPrice,
            final String status,
            final String endTime) {
        this.id = id;
        this.productName = productName;
        this.currentPrice = currentPrice;
        this.status = status;
        this.endTime = endTime;
    }

    public String getId() {
        return id;
    }

    public String getProductName() {
        return productName;
    }

    public String getCurrentPrice() {
        return currentPrice;
    }

    public String getStatus() {
        return status;
    }

    public String getEndTime() {
        return endTime;
    }
}
