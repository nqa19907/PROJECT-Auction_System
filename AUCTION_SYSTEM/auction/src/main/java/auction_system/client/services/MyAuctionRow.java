package auction_system.client.services;

/**
 * Dữ liệu 1 dòng trong bảng "Quản lý phiên của tôi".
 */
public class MyAuctionRow {
    private final String id;
    private final String productName;
    private final String currentPrice;
    private final String status;
    private final String startTime;
    private final String endTime;
    private final String category;
    private final String description;
    private final String condition;

    /**
     * Khởi tạo dữ liệu cho một dòng trong bảng quản lý phiên.
     *
     * @param id mã phiên đấu giá
     * @param productName tên sản phẩm
     * @param currentPrice giá hiện tại
     * @param status trạng thái phiên
     * @param startTime thời gian bắt đầu
     * @param endTime thời gian kết thúc
     * @param category danh mục sản phẩm
     * @param description mô tả sản phẩm
     * @param condition tình trạng sản phẩm
     */
    public MyAuctionRow(
            final String id,
            final String productName,
            final String currentPrice,
            final String status,
            final String startTime,
            final String endTime,
            final String category,
            final String description,
            final String condition) {
        this.id = id;
        this.productName = productName;
        this.currentPrice = currentPrice;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.category = category;
        this.description = description;
        this.condition = condition;
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

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getCondition() {
        return condition;
    }
}
