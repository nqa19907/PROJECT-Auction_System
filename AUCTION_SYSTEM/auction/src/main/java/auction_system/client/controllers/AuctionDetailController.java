package auction_system.client.controllers;

import auction_system.client.services.AuctionService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller cho màn hình chi tiết phiên đấu giá.
 */
public class AuctionDetailController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionDetailController.class);

    // Các thành phần UI tương ứng với file FXML (bạn sẽ cần tạo file AuctionDetail.fxml sau)
    @FXML private Label itemNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label statusLabel;
    @FXML private Label endTimeLabel;
    @FXML private Label sellerLabel;
    @FXML private Text descriptionText;
    @FXML private Button backButton;
    @FXML private Button bidButton;

    private String currentAuctionId;

    @FXML
    public void initialize() {
        // Tạm thời vô hiệu hóa nút đặt giá cho đến khi lấy được dữ liệu từ Server
        bidButton.setDisable(true);
    }

    // TODO: Load action detail.

    @FXML
    private void handleBackAction(ActionEvent event) {
        LOGGER.info("Quay lại màn hình danh sách (Sẽ xử lý code chuyển trang sau).");
    }

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        LOGGER.info("Người dùng muốn đặt giá cho ID: " + currentAuctionId);
    }
}