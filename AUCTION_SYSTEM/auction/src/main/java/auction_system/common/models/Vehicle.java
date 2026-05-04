package auction_system.common.models;

/**
 * Lớp đại diện cho sản phẩm đấu giá là phương tiện giao thông.
 */
public class Vehicle extends Item {
    private String make;
    private String model;
    private int manufacturingYear;
    private double mileage;

    /**
     * Khởi tạo một phương tiện giao thông.
     *
     * @param itemName          Tên phương tiện.
     * @param description       Mô tả chi tiết.
     * @param startPrice        Giá khởi điểm.
     * @param sellerId          ID của người bán.
     * @param condition         Tình trạng phương tiện.
     * @param imagePath         Đường dẫn hình ảnh.
     * @param make              Hãng sản xuất.
     * @param model             Mẫu xe.
     * @param manufacturingYear Năm sản xuất.
     * @param mileage           Số dặm đã đi (ODO).
     */
    public Vehicle(String itemName, String description, Double startPrice, String sellerId,
                   String condition, String imagePath, String make, String model,
                   int manufacturingYear, double mileage) {
        super(itemName, description, startPrice, sellerId, condition, imagePath);
        this.make = make;
        this.model = model;
        this.manufacturingYear = manufacturingYear;
        this.mileage = mileage;
    }

    /**
     * Lớp Builder giúp khởi tạo đối tượng Vehicle.
     */
    public static class Builder {
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
        public Builder itemName(String itemName) {
            this.itemName = itemName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder startPrice(double startPrice) {
            this.startPrice = startPrice;
            return this;
        }

        public Builder currentPrice(double currentPrice) {
            this.currentPrice = currentPrice;
            return this;
        }

        public Builder sellerId(String sellerId) {
            this.sellerId = sellerId;
            return this;
        }

        public Builder condition(String condition) {
            this.condition = condition;
            return this;
        }

        public Builder imagePath(String imagePath) {
            this.imagePath = imagePath;
            return this;
        }

        public Builder make(String make) {
            this.make = make;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder manufacturingYear(int manufacturingYear) {
            this.manufacturingYear = manufacturingYear;
            return this;
        }

        public Builder mileage(double mileage) {
            this.mileage = mileage;
            return this;
        }

        /**
         * Xây dựng và trả về đối tượng Vehicle.
         *
         * @return Đối tượng Vehicle đã được khởi tạo.
         */
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

    @Override
    public String getDisplayDetails() {
        return String.format("Xe: %s | Hãng: %s | Model: %s | Đời: %d | ODO: %,.1f km",
                getItemName(), this.make, this.model, this.manufacturingYear, this.mileage);
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getManufacturingYear() {
        return manufacturingYear;
    }

    public void setManufacturingYear(int manufacturingYear) {
        this.manufacturingYear = manufacturingYear;
    }

    public double getMileage() {
        return mileage;
    }

    public void setMileage(double mileage) {
        this.mileage = mileage;
    }

    @Override
    public String toString() {
        return super.toString() + " -> Vehicle{"
                + "make='" + make + '\''
                + ", model='" + model + '\''
                + ", manufacturingYear=" + manufacturingYear
                + ", mileage=" + mileage
                + '}';
    }
}