package auction_system.common.constants;

/**
 * Lớp chứa các hằng số (Constants) dùng chung cho toàn bộ ứng dụng.
 */
public final class AppConstants {

    // Các hằng số định nghĩa danh mục sản phẩm
    public static final String CATEGORY_ALL = "ALL";
    public static final String CATEGORY_ART = "Art";
    public static final String CATEGORY_ELECTRONIC = "Electronic";
    public static final String CATEGORY_VEHICLE = "Vehicle";

    // Các hằng số định nghĩa tiêu đề hiển thị trên giao diện
    public static final String TITLE_ALL = "Tất cả";
    public static final String TITLE_ART = "Nghệ thuật";
    public static final String TITLE_ELECTRONIC = "Đồ điện tử";
    public static final String TITLE_VEHICLE = "Phương tiện giao thông";

    // Các hằng số định nghĩa ID của các component UI (dùng cho JavaFX Event)
    public static final String UI_ID_CATEGORY_ALL = "categoryAll";
    public static final String UI_ID_CATEGORY_ART = "categoryArt";
    public static final String UI_ID_CATEGORY_ELECTRONIC = "categoryElectronic";
    public static final String UI_ID_CATEGORY_VEHICLE = "categoryVehicle";

    // Private constructor để ngăn chặn việc dùng từ khoá 'new' khởi tạo đối tượng
    private AppConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

}
