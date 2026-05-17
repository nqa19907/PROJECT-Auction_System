package auction_system.common.dto.items;

import java.io.Serializable;

/**
 * DTO chứa thông tin thiết bị điện tử để truyền qua mạng Socket.
 * Không có hàm Setter để đảm bảo tính bất biến và an toàn đa luồng.
 */
public final class ElectronicDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String itemName;
    private final String description;
    private final double startPrice;
    private final String sellerId;
    private final String category;

    /**
     * Khởi tạo một gói dữ liệu ElectronicDTO bất biến.
     *
     * @param id ID sản phẩm.
     * @param itemName Tên sản phẩm điện tử.
     * @param description Mô tả chi tiết.
     * @param startPrice Giá khởi điểm.
     * @param sellerId ID người bán.
     * @param category Danh mục sản phẩm.
     */
    public ElectronicDto(
            final String id,
            final String itemName,
            final String description,
            final double startPrice,
            final String sellerId,
            final String category) {

        this.id = id;
        this.itemName = itemName;
        this.description = description;
        this.startPrice = startPrice;
        this.sellerId = sellerId;
        this.category = category;
    }

    /**
     * Lấy ID sản phẩm.
     *
     * @return ID sản phẩm.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Lấy tên sản phẩm.
     *
     * @return tên sản phẩm.
     */
    public String getItemName() {
        return this.itemName;
    }

    /**
     * Lấy mô tả sản phẩm.
     *
     * @return mô tả.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Lấy giá khởi điểm.
     *
     * @return giá khởi điểm.
     */
    public double getStartPrice() {
        return this.startPrice;
    }

    /**
     * Lấy ID người bán.
     *
     * @return ID người bán.
     */
    public String getSellerId() {
        return this.sellerId;
    }

    /**
     * Lấy danh mục sản phẩm.
     *
     * @return danh mục.
     */
    public String getCategory() {
        return this.category;
    }
}