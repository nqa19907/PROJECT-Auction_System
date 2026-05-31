package auction_system.client.utils;

import java.util.Map;
import javafx.stage.Stage;

/**
 * Tiện ích đặt tiêu đề cửa sổ theo màn hình hiện tại.
 */
public final class WindowTitleUtil {
    private static final String appName = "AuctionHub";
    private static final Map<String, String> titlesByView = Map.ofEntries(
            Map.entry(ViewConstants.LOGIN_VIEW, "Đăng nhập - " + appName),
            Map.entry(ViewConstants.REGISTER_VIEW, "Đăng ký tài khoản - " + appName),
            Map.entry(ViewConstants.DASHBOARD_VIEW, "Trang chủ - " + appName),
            Map.entry(ViewConstants.ADMIN_DASHBOARD_VIEW, "Quản trị - " + appName),
            Map.entry(ViewConstants.ITEM_LIST_VIEW, "Danh sách đấu giá - " + appName),
            Map.entry(ViewConstants.PUBLISH_ITEM_VIEW, "Đăng bán sản phẩm - " + appName),
            Map.entry(ViewConstants.AUCTION_DETAIL_VIEW, "Chi tiết phiên đấu giá - " + appName),
            Map.entry(
                    ViewConstants.MY_AUCTION_MANAGEMENT_VIEW,
                    "Quản lý phiên của tôi - " + appName));

    private WindowTitleUtil() {
        // Không cho khởi tạo utility class.
    }

    /**
     * Đặt title theo đường dẫn FXML nếu có mapping.
     *
     * @param stage cửa sổ cần cập nhật
     * @param viewPath đường dẫn FXML hiện tại
     */
    public static void applyTitle(final Stage stage, final String viewPath) {
        if (stage == null) {
            return;
        }

        stage.setTitle(resolveTitle(viewPath));
    }

    /**
     * Lấy title phù hợp cho view.
     *
     * @param viewPath đường dẫn FXML
     * @return tiêu đề cửa sổ
     */
    public static String resolveTitle(final String viewPath) {
        if (viewPath == null || viewPath.isBlank()) {
            return appName;
        }

        return titlesByView.getOrDefault(normalize(viewPath), appName);
    }

    private static String normalize(final String viewPath) {
        return viewPath.startsWith("/") ? viewPath : "/" + viewPath;
    }
}
