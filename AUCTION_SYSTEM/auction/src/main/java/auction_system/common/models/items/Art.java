package auction_system.common.models.items;

/**
 * Lớp đại diện cho sản phẩm đấu giá là một tác phẩm nghệ thuật.
 */
public class Art extends Item {

    /**
     * Khởi tạo một tác phẩm nghệ thuật.
     *
     * @param itemName    Tên tác phẩm.
     * @param description Mô tả chi tiết.
     * @param startPrice  Giá khởi điểm.
     * @param sellerId    ID của người bán.
     * 
     */
    public Art(String itemName, String description, Double startPrice, String sellerId) {
        super(itemName, description, startPrice, sellerId);

    }

}
