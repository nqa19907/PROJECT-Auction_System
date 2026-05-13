package auction_system.common.models.items;

/**
 * Lớp đại diện cho sản phẩm đấu giá là thiết bị điện tử.
 */
public class Electronic extends Item {

    private String brand;
    private int warrantyMonths;

    /**
     * Khởi tạo một thiết bị điện tử.
     *
     * @param itemName Tên sản phẩm.
     * @param description Mô tả chi tiết.
     * @param startPrice Giá khởi điểm.
     * @param sellerId ID của người bán.
     * @param condition Tình trạng sản phẩm.
     * @param imagePath Đường dẫn hình ảnh.
     * @param brand Thương hiệu.
     * @param warrantyMonths Số tháng bảo hành.
     */
    public Electronic(String itemName, String description, Double startPrice, String sellerId,
            String condition, String imagePath, String brand, int warrantyMonths) {
        super(itemName, description, startPrice, sellerId, condition, imagePath);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String getDisplayDetails() {
        return String.format("Điện tử: %s | Hãng: %s | Bảo hành: %d tháng | Tình trạng: %s",
                getItemName(), this.brand, this.warrantyMonths, getCondition());
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public String toString() {
        return super.toString() + " -> Electronic{"
                + "brand='" + brand + '\''
                + ", warrantyMonths=" + warrantyMonths
                + '}';
    }
}
