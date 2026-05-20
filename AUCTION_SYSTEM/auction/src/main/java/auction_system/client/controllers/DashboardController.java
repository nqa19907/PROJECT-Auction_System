package auction_system.client.controllers;

import auction_system.client.services.AuthService;
import auction_system.client.utils.SceneManager;
import java.io.IOException;
import java.util.logging.Logger;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;


/**
 * Controller điều khiển giao diện Dashboard chính của người dùng.
 */
public class DashboardController {
    private static final Logger LOGGER =
            Logger.getLogger(DashboardController.class.getName());

    @FXML
    private Button btnSignOut;

    @FXML
    private StackPane contentArea;

    /**
     * Khởi tạo giao diện Dashboard.
     */
    @FXML
    public void initialize() {
        handleShowItems();
    }

    /**
     * Xử lý đăng xuất người dùng hiện tại.
     *
     * @param event sự kiện nhấn nút đăng xuất
     */
    @FXML
    private void handleSignOut(final ActionEvent event) {
        LOGGER.info("Thực hiện đăng xuất và quay về màn hình Đăng nhập.");

        AuthService.getInstance().logout();
        SceneManager.switchScene(btnSignOut, "Login.fxml", 900, 700);
    }

    @FXML
    private void handlePublishItem() {
        LOGGER.info("Chuyển sang giao diện đăng bán");

        try {
            final Node view = FXMLLoader.load(
                    getClass().getResource("/client/fxml/PublishItem.fxml"));
            contentArea.getChildren().setAll(view);
        } catch (IOException exception) {
            LOGGER.warning("Không thể tải giao diện đăng bán: " + exception.getMessage());
        }
    }

    @FXML
    private void handleShowItems() {
        LOGGER.info("Chuyển sang giao diện danh sách vật phẩm");

        try {
            final Node view = FXMLLoader.load(
                    getClass().getResource("/client/fxml/ItemList.fxml"));
            contentArea.getChildren().setAll(view);
        } catch (IOException exception) {
            LOGGER.warning("Không thể tải giao diện danh sách vật phẩm: " 
                    + exception.getMessage());
        }
    }

    @FXML
    private void handleShowBidHistory() {
        LOGGER.info("Chuyển sang giao diện lịch sử đấu giá");

        try {
            final Node view = FXMLLoader.load(
                    getClass().getResource("/client/fxml/BidHistoryView.fxml"));
            contentArea.getChildren().setAll(view);
        } catch (IOException exception) {
            LOGGER.warning("Không thể tải giao diện lịch sử đấu giá: " 
                    + exception.getMessage());
        }
    }
}