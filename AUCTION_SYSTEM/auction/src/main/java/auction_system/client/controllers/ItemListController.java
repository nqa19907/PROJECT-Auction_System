package auction_system.client.controllers;

import auction_system.client.services.AuctionService;
import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller điều khiển danh sách các phiên đấu giá.
 */
public class ItemListController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemListController.class);
    
    @FXML private FlowPane productsGrid;

    /**
     * Khởi tạo giao diện và lấy dữ liệu phiên đấu giá.
     */
    @FXML
    public void initialize() {
        LOGGER.info("Đang khởi tạo màn hình Danh sách đấu giá...");

        // Cài đặt khoảng cách (spacing) giữa các thẻ sản phẩm trong FlowPane
        if (productsGrid != null) {
            productsGrid.setHgap(12); // Khoảng cách ngang (24px)
            productsGrid.setVgap(12); // Khoảng cách dọc (24px)
        }

        // Gọi Service, cung cấp một hàm Callback để tự động xử lý khi có dữ liệu trả về
        AuctionService.getInstance().fetchAuctionList(auctionList -> {
            // Bọc trong Platform.runLater để đảm bảo việc vẽ giao diện chạy trên luồng của JavaFX
            Platform.runLater(() -> {
                if (productsGrid != null) {
                    productsGrid.getChildren().clear();
                }

                if (auctionList.isEmpty()) {
                    Label emptyLabel = new Label("Hiện không có phiên đấu giá nào.");
                    productsGrid.getChildren().add(emptyLabel);
                    return;
                }
                
                for (String[] parts : auctionList) {
                    createProductCard(parts);
                }
            });
        });
    }

    private void createProductCard(String[] parts) {
        try {
            // 1. Nạp file FXML của thẻ sản phẩm
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/client/fxml/components/ProductCard.fxml"));
            VBox card = loader.load();

            // 2. Lấy controller của thẻ vừa nạp
            ProductCardController controller = loader.getController();

            // 3. Bóc tách dữ liệu từ mảng ngay bên trong hàm
            String auctionId = parts[0];
            String itemName = parts[1];
            String currentPriceStr = parts[2];
            String status = parts[3];
            String endTime = parts[4];

            // 4. Chuyển đổi dữ liệu chuỗi giá tiền thành số thập phân
            double currentPrice = Double.parseDouble(currentPriceStr);

            // 5. "Bơm" dữ liệu vào thẻ và định nghĩa hành động khi nút được bấm
            controller.setCardDetails(
                auctionId,
                itemName,
                currentPrice,
                (selectedItemId) -> {
                    navigateToAuctionDetail(selectedItemId);
                }
            );

            // 5. Thêm thẻ sản phẩm hoàn chỉnh vào lưới hiển thị
            productsGrid.getChildren().add(card);

        } catch (IOException | NumberFormatException e) {
            LOGGER.error("Lỗi khi tạo thẻ sản phẩm (ProductCard): ", e);
        }
    }

    private void navigateToAuctionDetail(String selectedItemId) {
        LOGGER.info("Người dùng muốn đấu giá cho sản phẩm có ID: " + selectedItemId);
        try {
            FXMLLoader detailLoader = new FXMLLoader(
                getClass().getResource("/client/fxml/AuctionDetail.fxml"));
            Node detailView = detailLoader.load();
            
            // TODO: Lấy controller và truyền ID sang màn hình chi tiết (đã bỏ giả lập)
            // AuctionDetailController detailController = detailLoader.getController();
            // detailController.setAuctionId(selectedItemId);

            // Tìm khu vực hiển thị chính (StackPane) của Dashboard đè màn hình mới lên
            StackPane contentArea = (StackPane) productsGrid.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(detailView);
            }
        } catch (Exception e) {
            LOGGER.error(
                "Lỗi khi chuyển sang giao diện AuctionDetail cho ID: " + selectedItemId, e);
        }
    }
}
