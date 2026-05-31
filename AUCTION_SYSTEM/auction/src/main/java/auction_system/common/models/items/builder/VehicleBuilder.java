package auction_system.common.models.items.builder;

import auction_system.common.models.items.Vehicle;

/**
 * Lớp Builder giúp khởi tạo đối tượng Vehicle.
 */
public class VehicleBuilder implements Builder<Vehicle> {

    private String itemName;
    private String description;
    private double startPrice;
    private double currentPrice;
    private String sellerId;
    private String imagePath;

    // Các hàm builder
    public VehicleBuilder itemName(String itemName) {
        this.itemName = itemName;
        return this;
    }

    public VehicleBuilder description(String description) {
        this.description = description;
        return this;
    }

    public VehicleBuilder startPrice(double startPrice) {
        this.startPrice = startPrice;
        return this;
    }

    public VehicleBuilder currentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
        return this;
    }

    public VehicleBuilder sellerId(String sellerId) {
        this.sellerId = sellerId;
        return this;
    }

    // Gắn đường dẫn ảnh vào item đang được build.
    public VehicleBuilder imagePath(final String imagePath) {
        this.imagePath = imagePath;
        return this;
    }

    /**
     * Xây dựng và trả về đối tượng Vehicle.
     *
     * @return Đối tượng Vehicle đã được khởi tạo.
     */
    @Override
    public Vehicle build() {
        Vehicle vehicle = new Vehicle(
                itemName, description, startPrice, sellerId);
        if (this.currentPrice > 0) {
            vehicle.setCurrentPrice(this.currentPrice);
        }
        // Áp dụng ảnh sản phẩm sau khi tạo domain object.
        vehicle.setImagePath(this.imagePath);
        return vehicle;
    }
}
