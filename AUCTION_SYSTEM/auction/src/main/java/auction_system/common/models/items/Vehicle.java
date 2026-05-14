package auction_system.common.models.items;

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