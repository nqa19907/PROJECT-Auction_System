package auction_system.client.controllers;

import auction_system.client.utils.SceneManager;
import auction_system.server.core.AuctionManager;
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
    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());

    @FXML
    private Button btnSignOut;

    @FXML
    private StackPane contentArea;

    @FXML
    public void initialize() {
        // Tự động load giao diện trang chủ (dashboardContent) ngay khi mở ứng dụng
        handleShowItems();
    }

    @FXML
    private void handleSignOut(ActionEvent event) {
        LOGGER.info("Thực hiện đăng xuất: Đóng Dashboard và quay về màn hình Đăng nhập.");
        // Đóng màn hình hiện tại và quay về màn hình Login với kích thước mặc định (900x700)
        SceneManager.switchScene(btnSignOut, "Login.fxml", 900, 700);
        AuctionManager.getInstance().userLoggedOut(null);
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

    @FXML
    private void handleShowBidHistory() {
        LOGGER.info("Chuyển sang giao diện lịch sử đấu giá");
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource("/client/fxml/BidHistoryView.fxml")
            );
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
