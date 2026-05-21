package auction_system.client.controllers.dashboard;

import auction_system.client.controllers.auction.ItemListController;
import auction_system.client.controllers.components.ProfileController;
import auction_system.client.controllers.components.SidebarController;
import auction_system.client.services.AuthService;
import auction_system.client.utils.Router;
import auction_system.client.utils.SceneManager;
import auction_system.client.utils.ViewConstants;
import auction_system.common.constants.AppConstants;
import java.io.IOException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller điều khiển giao diện Dashboard chính của người dùng.
 */
public class DashboardController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardController.class);

    @FXML
    private Button btnSignOut;

    @FXML
    private StackPane contentArea;

    // JavaFX sẽ tự động bind file FXML được include có fx:id="sidebar" vào biến này
    // Quy tắc đặt tên: [fx:id] + "Controller"
    @FXML
    private SidebarController sidebarController;

    @FXML
    private ProfileController profileController;

    /**
     * Khởi tạo màn hình Dashboard.
     * Mặt định phóng to tối đa cửa sổ.
     */
    @FXML
    public void initialize() {
        // Đợi giao diện được gắn vào Scene xong thì lấy Stage hiện tại và phóng to (maximize)
        Platform.runLater(() -> {
            Stage stage = (Stage) contentArea.getScene().getWindow();
            if (stage != null) {
                stage.setMaximized(true);
            }
            
            // Tự động load giao diện trang chủ (Tất cả sản phẩm) sau khi Scene đã sẵn sàng
            handleShowItems();
        });

        // Thiết lập liên kết giao tiếp với Sidebar
        if (sidebarController != null) {
            sidebarController.setOnCategorySelected(this::loadItemList);
            sidebarController.setOnPublishItemSelected(this::loadPublishItemView);
        }
    }

    @FXML
    private void handleSignOut(ActionEvent event) {
        LOGGER.info("Thực hiện đăng xuất: Gọi service để logout khỏi Server...");

        // Gọi AuthService để gửi lệnh LOGOUT lên server
        AuthService.getInstance().logout(result -> {
            Platform.runLater(() -> {
                if (result.isSuccess()) {
                    LOGGER.info(
                        "Đăng xuất thành công. Đóng Dashboard và quay về màn hình Đăng nhập.");
                } else {
                    LOGGER.warn(
                        "Đăng xuất có lỗi (hoặc không phản hồi): " + result.getErrorMessage());
                }
                // Luôn chuyển người dùng về màn hình đăng nhập dù kết quả trả về ra sao
                SceneManager.switchScene(btnSignOut, ViewConstants.LOGIN_VIEW, 900, 700);
            });
        });
    }

    private void loadPublishItemView() {
        LOGGER.info("Chuyển sang giao diện đăng bán");
        Router.navigateContent(contentArea, ViewConstants.PUBLISH_ITEM_VIEW);
    }

    @FXML
    private void handleShowItems() {
        if (sidebarController != null) {
            sidebarController.setActiveSidebarItem(AppConstants.UI_ID_CATEGORY_ALL);
        }
        loadItemList(AppConstants.CATEGORY_ALL);
    }

    private void loadItemList(String category) {
        LOGGER.info("Chuyển sang giao diện danh sách, lọc theo danh mục: " + category);
        ItemListController controller = Router.navigateContentAndGetController(
                contentArea, ViewConstants.ITEM_LIST_VIEW);
        if (controller != null) {
            controller.setFilterCategory(category);
        }
    }
}