package auction_system.client.controllers;

import auction_system.client.services.AuthService;
import auction_system.client.utils.SceneManager;
import auction_system.common.constants.AppConstants;
import java.io.IOException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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

    @FXML
    private VBox categorySidebar;

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
        });

        // Tự động load giao diện trang chủ (dashboardContent) ngay khi mở ứng dụng
        handleShowItems();
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
                SceneManager.switchScene(btnSignOut, "Login.fxml", 900, 700);
            });
        });
    }

    @FXML
    private void handlePublishItem() {
        // Đánh dấu mục "Đăng bán" trên sidebar là đang active
        setActiveSidebarItem("publishItem");

        LOGGER.info("Chuyển sang giao diện đăng bán");
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource("/client/fxml/PublishItem.fxml")
            );
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCategoryFilter(MouseEvent event) {
        Node source = (Node) event.getSource();
        String id = source.getId();
        
        String category = AppConstants.CATEGORY_ALL;
        if (id != null) {
            switch (id) {
                case AppConstants.UI_ID_CATEGORY_ART:
                    category = AppConstants.CATEGORY_ART;
                    break;
                case AppConstants.UI_ID_CATEGORY_ELECTRONIC:
                    category = AppConstants.CATEGORY_ELECTRONIC;
                    break;
                case AppConstants.UI_ID_CATEGORY_VEHICLE:
                    category = AppConstants.CATEGORY_VEHICLE;
                    break;
                default:
                    break;
            }
        }

        setActiveSidebarItem(id);
        loadItemList(category);
    }

    @FXML
    private void handleShowItems() {
        setActiveSidebarItem(AppConstants.UI_ID_CATEGORY_ALL);
        loadItemList(AppConstants.CATEGORY_ALL);
    }

    private void loadItemList(String category) {
        LOGGER.info("Chuyển sang giao diện danh sách, lọc theo danh mục: " + category);
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/client/fxml/ItemList.fxml"));
            Node view = loader.load();

            ItemListController controller = loader.getController();
            controller.setFilterCategory(category);

            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            LOGGER.error("Lỗi khi tải giao diện ItemList.fxml", e);
        }
    }

    /**
     * Cập nhật trạng thái "active" cho các nút trên Sidebar.
     * Dùng để highlight mục hiện tại đang được chọn.
     *
     * @param activeId ID của component cần được highlight.
     */
    private void setActiveSidebarItem(String activeId) {
        if (categorySidebar == null || activeId == null) {
            return;
        }
        
        // Duyệt qua tất cả thẻ, xóa tất cả trạng thái active của các thẻ
        // và chỉ đặt active ở thẻ vừa chọn.
        for (Node node : categorySidebar.getChildren()) {
            if (node.getStyleClass().contains("category-option")) {
                node.getStyleClass().remove("active");
                if (activeId.equals(node.getId())) {
                    node.getStyleClass().add("active");
                }
            }
        }
    }
}