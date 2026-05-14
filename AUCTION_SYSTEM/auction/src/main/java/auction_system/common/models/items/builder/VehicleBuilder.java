package auction_system.common.models.items.builder;

import auction_system.common.models.items.Vehicle;

/**
 * Lớp Builder giúp khởi tạo đối tượng Vehicle.
 */
public class VehicleBuilder implements Builder<Vehicle> {
    // 1. Thuộc tính của Item
    private String itemName;
    private String description;
    private double startPrice;
    private double currentPrice;
    private String sellerId;
    private String condition;
    private String imagePath;

    // 2. Thuộc tính riêng của Vehicle
    private String make;
    private String model;
    private int manufacturingYear;
    private double mileage;

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

    public VehicleBuilder condition(String condition) {
        this.condition = condition;
        return this;
    }

    public VehicleBuilder imagePath(String imagePath) {
        this.imagePath = imagePath;
        return this;
    }

    public VehicleBuilder make(String make) {
        this.make = make;
        return this;
    }

    public VehicleBuilder model(String model) {
        this.model = model;
        return this;
    }

    public VehicleBuilder manufacturingYear(int manufacturingYear) {
        this.manufacturingYear = manufacturingYear;
        return this;
    }

    public VehicleBuilder mileage(double mileage) {
        this.mileage = mileage;
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
                itemName, description, startPrice, sellerId, condition, imagePath,
                make, model, manufacturingYear, mileage);
        if (this.currentPrice > 0) {
            vehicle.setCurrentPrice(this.currentPrice);
        }
        return vehicle;
    }
}