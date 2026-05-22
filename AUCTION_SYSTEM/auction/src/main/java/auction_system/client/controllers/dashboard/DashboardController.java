package auction_system.client.controllers.dashboard;

import auction_system.client.controllers.auction.ItemListController;
import auction_system.client.controllers.components.SidebarController;
import auction_system.client.services.AuthService;
import auction_system.client.utils.Router;
import auction_system.client.utils.SceneManager;
import auction_system.client.utils.ViewConstants;
import auction_system.common.constants.AppConstants;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller điều khiển giao diện Dashboard chính của người dùng.
 */
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private static final double loginWindowWidth = 900.0;
    private static final double loginWindowHeight = 700.0;

    @FXML
    private Button btnSignOut;

    @FXML
    private StackPane contentArea;

    @FXML
    private SidebarController sidebarController;

    /**
     * Khởi tạo màn hình Dashboard.
     */
    @FXML
    public void initialize() {
        Platform.runLater(this::maximizeAndLoadDefaultContent);
        setupSidebarActions();
    }

    /**
     * Phóng to cửa sổ và nạp danh sách sản phẩm mặc định.
     */
    private void maximizeAndLoadDefaultContent() {
        final Stage stage = (Stage) contentArea.getScene().getWindow();

        if (stage != null) {
            stage.setMaximized(true);
        }

        handleShowItems();
    }

    /**
     * Gắn các hành động điều hướng cho Sidebar.
     */
    private void setupSidebarActions() {
        if (sidebarController == null) {
            return;
        }

        sidebarController.setOnCategorySelected(this::loadItemList);
        sidebarController.setOnPublishItemSelected(this::loadPublishItemView);
    }

    /**
     * Xử lý sự kiện đăng xuất khỏi hệ thống.
     */
    @FXML
    private void handleSignOut() {
        logger.info("Thực hiện đăng xuất: gửi lệnh LOGOUT tới server.");

        AuthService.getInstance().logout();
        SceneManager.switchScene(
                btnSignOut,
                ViewConstants.LOGIN_VIEW,
                loginWindowWidth,
                loginWindowHeight);
    }

    /**
     * Chuyển sang giao diện đăng bán sản phẩm.
     */
    private void loadPublishItemView() {
        logger.info("Chuyển sang giao diện đăng bán.");
        Router.navigateContent(contentArea, ViewConstants.PUBLISH_ITEM_VIEW);
    }

    /**
     * Hiển thị toàn bộ sản phẩm trên Dashboard.
     */
    @FXML
    private void handleShowItems() {
        if (sidebarController != null) {
            sidebarController.setActiveSidebarItem(AppConstants.UI_ID_CATEGORY_ALL);
        }

        loadItemList(AppConstants.CATEGORY_ALL);
    }

    /**
     * Nạp danh sách sản phẩm theo danh mục.
     *
     * @param category danh mục sản phẩm cần hiển thị
     */
    private void loadItemList(final String category) {
        logger.info("Chuyển sang danh sách sản phẩm, danh mục: " + category);

        final ItemListController controller = Router.navigateContentAndGetController(
                contentArea,
                ViewConstants.ITEM_LIST_VIEW);

        if (controller != null) {
            controller.setFilterCategory(category);
        }
    }
}