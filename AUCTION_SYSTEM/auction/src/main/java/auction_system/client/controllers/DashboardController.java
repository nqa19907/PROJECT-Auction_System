package auction_system.client.controllers;

import auction_system.client.services.AuthService;
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