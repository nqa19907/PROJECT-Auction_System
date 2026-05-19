package auction_system.client.controllers;

import auction_system.client.utils.SceneManager;
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
        LOGGER.info("Thực hiện đăng xuất: Đóng Dashboard và quay về màn hình Đăng nhập.");
        // Đóng màn hình hiện tại và quay về màn hình Login với kích thước mặc định (900x700)
        SceneManager.switchScene(btnSignOut, "Login.fxml", 900, 700);
    }

    @FXML
    private void handlePublishItem() {
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
    private void handleShowItems() {
        LOGGER.info("Chuyển sang giao diện danh sách vật phẩm");
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource("/client/fxml/ItemList.fxml")
            );
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}