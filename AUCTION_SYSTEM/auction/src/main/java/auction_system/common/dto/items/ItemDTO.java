package auction_system.common.dto.items;

import java.io.Serializable;

/**
 * DTO chứa thông tin sản phẩm đấu giá để truyền qua mạng Socket.
 * Không có hàm Setter để đảm bảo tính bất biến và an toàn đa luồng.
 */
public final class ItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String itemName;
    private final String description;
    private final double startPrice;
    private final double currentPrice;
    private final String sellerId;

    /**
     * Khởi tạo một gói dữ liệu ItemDTO bất biến.
     *
     * @param id ID sản phẩm.
     * @param itemName Tên sản phẩm.
     * @param description Mô tả sản phẩm.
     * @param startPrice Giá khởi điểm.
     * @param currentPrice Giá hiện tại.
     * @param sellerId ID người bán.
     */
    public ItemDTO(
            final String id,
            final String itemName,
            final String description,
            final double startPrice,
            final double currentPrice,
            final String sellerId) {

        this.id = id;
        this.itemName = itemName;
        this.description = description;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice;
        this.sellerId = sellerId;
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
     * Lấy giá hiện tại.
     *
     * @return giá hiện tại.
     */
    public double getCurrentPrice() {
        return this.currentPrice;
    }

    /**
     * Lấy ID người bán.
     *
     * @return ID người bán.
     */
    public String getSellerId() {
        return this.sellerId;
    }
}