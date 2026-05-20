package auction_system.client.controllers;

import java.io.IOException;
import java.util.logging.Logger;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

/**
 * Controller cho ItemList.fxml.
 */
public class ItemListController {

    private static final Logger LOGGER =
            Logger.getLogger(ItemListController.class.getName());

    @FXML
    private Button bidBtnCard1;

    @FXML
    private Button bidBtnCard2;

    @FXML
    private Button bidBtnCard3;

    @FXML
    private Button bidBtnCard4;

    @FXML
    private void openBidHistoryCard1() {
        openBidHistory(new AuctionDisplayContext(
                "AUC-2024-0518",
                "Đồng hồ Rolex Submariner Date 2004",
                3_500_000L,
                15_500_000L
        ));
    }

    @FXML
    private void openBidHistoryCard2() {
        openBidHistory(new AuctionDisplayContext(
                "AUC-2024-0519",
                "Tượng đồng chiến binh Hy Lạp cổ đại",
                1_200_000L,
                3_200_000L
        ));
    }

    @FXML
    private void openBidHistoryCard3() {
        openBidHistory(new AuctionDisplayContext(
                "AUC-2024-0520",
                "Bộ xu bạc La Mã thế kỷ 18 nguyên bản",
                300_000L,
                850_000L
        ));
    }

    @FXML
    private void openBidHistoryCard4() {
        openBidHistory(new AuctionDisplayContext(
                "AUC-2024-0521",
                "Tranh sơn dầu trừu tượng hiện đại",
                500_000L,
                1_400_000L
        ));
    }

    private void openBidHistory(final AuctionDisplayContext context) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/client/fxml/BidHistoryView.fxml")
            );
            Node view = loader.load();
            BidHistoryController controller = loader.getController();
            controller.initAuction(context);

            Node anyBidButton = bidBtnCard1 != null ? bidBtnCard1 : bidBtnCard2;
            if (anyBidButton == null) {
                anyBidButton = bidBtnCard3 != null ? bidBtnCard3 : bidBtnCard4;
            }
            if (anyBidButton == null || anyBidButton.getScene() == null) {
                LOGGER.warning("Không thể mở BidHistory: không tìm thấy Scene từ ItemList.");
                return;
            }

            Node contentAreaNode = anyBidButton.getScene().lookup("#contentArea");
            if (contentAreaNode instanceof StackPane contentArea) {
                contentArea.getChildren().setAll(view);
                LOGGER.info("Đã mở BidHistory với auctionId = " + context.auctionId());
                return;
            }
            LOGGER.warning("Không tìm thấy #contentArea để mở BidHistory.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
