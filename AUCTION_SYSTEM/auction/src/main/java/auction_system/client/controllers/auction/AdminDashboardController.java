package auction_system.client.controllers.auction;

import auction_system.client.utils.Router;
import auction_system.client.utils.ViewConstants;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;

/**
 * Controller cho màn hình Admin Dashboard.
 * Hiện chỉ chứa hành động đơn giản: quay về màn hình chính (ItemList) khi nhấn nút.
 */
public class AdminDashboardController {

    @FXML
    private BorderPane root;

    /**
     * Xử lý khi người dùng nhấn "Quay về" ở header của Admin Dashboard.
     * Nạp lại giao diện danh sách sản phẩm vào vùng content của Dashboard.
     */
    @FXML
    private void handleBack() {
        // Dùng Router để nạp view vào vùng content (tương tự Dashboard.loadPublishItemView)
        Router.navigateContent(root, ViewConstants.ITEM_LIST_VIEW);
    }

}