package auction_system.client.controllers;

import auction_system.client.services.AuctionService;
import auction_system.common.constants.AppConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    @FXML private Label categoryTitle;

    private List<String[]> allAuctions = new ArrayList<>();
    private String filterCategory = AppConstants.CATEGORY_ALL;

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
                this.allAuctions = auctionList;
                renderGrid();
            });
        });
    }

    private void createProductCard(String[] parts) {
        // Kiểm tra an toàn: Đảm bảo mảng có đủ ít nhất 5 trường dữ liệu cơ bản
        if (parts == null || parts.length < 5) {
            LOGGER.warn("Dữ liệu sản phẩm không hợp lệ: thiếu trường thông tin.");
            return;
        }

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
                    selectedItemId -> navigateToAuctionDetail(parts)
            );

            // 6. Thêm thẻ sản phẩm hoàn chỉnh vào lưới hiển thị
            productsGrid.getChildren().add(card);

        } catch (IOException | NumberFormatException e) {
            LOGGER.error("Lỗi khi tạo thẻ sản phẩm (ProductCard): ", e);
        }
    }

    private void navigateToAuctionDetail(String[] selectedParts) {
        if (selectedParts == null || selectedParts.length < 5) {
            LOGGER.warn("Không thể mở AuctionDetail vì dữ liệu item không hợp lệ.");
            return;
        }

        String selectedItemId = selectedParts[0];
        String itemName = selectedParts[1];

        long currentPrice;
        try {
            currentPrice = Long.parseLong(selectedParts[2].replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            LOGGER.error("Giá hiện tại không hợp lệ cho item ID: " + selectedItemId, e);
            return;
        }

        long openingPrice = Math.max(0L, currentPrice - 1_000_000L);

        if (selectedParts.length > 6) {
            try {
                openingPrice = Long.parseLong(selectedParts[6].replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                LOGGER.warn("Giá khởi điểm không hợp lệ, tạm dùng giá hiện tại trừ 1.000.000.");
            }
        }

        LOGGER.info("Người dùng muốn đấu giá cho sản phẩm có ID: " + selectedItemId);

        try {
            FXMLLoader detailLoader = new FXMLLoader(
                    getClass().getResource("/client/fxml/AuctionDetail.fxml"));
            Node detailView = detailLoader.load();

            AuctionDetailController auctionDetailController = detailLoader.getController();
            auctionDetailController.initAuction(
                    new AuctionDisplayContext(
                            selectedItemId,
                            itemName,
                            openingPrice,
                            currentPrice
                    )
            );

            StackPane contentArea = (StackPane) productsGrid.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(detailView);
            }
        } catch (Exception e) {
            LOGGER.error(
                    "Lỗi khi chuyển sang giao diện AuctionDetail cho ID: " + selectedItemId, e);
        }
    }

    /**
     * Thiết lập danh mục lọc và hiển thị lại lưới sản phẩm.
     *
     * @param category Tên danh mục cần lọc (VD: Art, Electronic...).
     */
    public void setFilterCategory(String category) {
        this.filterCategory = category;

        // Cập nhật dòng text tiêu đề phía trên danh sách sản phẩm
        if (categoryTitle != null) {
            switch (category) {
                case AppConstants.CATEGORY_ART:
                    categoryTitle.setText(AppConstants.TITLE_ART);
                    break;
                case AppConstants.CATEGORY_ELECTRONIC:
                    categoryTitle.setText(AppConstants.TITLE_ELECTRONIC);
                    break;
                case AppConstants.CATEGORY_VEHICLE:
                    categoryTitle.setText(AppConstants.TITLE_VEHICLE);
                    break;
                default:
                    categoryTitle.setText(AppConstants.TITLE_ALL);
                    break;
            }
        }

        // Nếu dữ liệu đã tải về xong mới vẽ lại
        if (!allAuctions.isEmpty()) {
            renderGrid();
        }
    }

    private void renderGrid() {
        if (productsGrid == null) {
            return;
        }

        productsGrid.getChildren().clear(); // Dọn dẹp lưới hiển thị cũ

        boolean hasItems = false;
        for (String[] parts : allAuctions) {
            // Tên danh mục nãy ta nối trên Server giờ nó nằm ở vị trí số 5 (index = 5)
            String itemCategory = (parts.length > 5) ? parts[5] : AppConstants.CATEGORY_ALL;

            // So sánh, nếu chữ khớp (hoặc đang chọn tất cả) thì mới vẽ
            if (AppConstants.CATEGORY_ALL.equals(filterCategory)
                    || filterCategory.equalsIgnoreCase(itemCategory)) {

                createProductCard(parts);
                hasItems = true;
            }
        }

        if (!hasItems) {
            productsGrid.getChildren().add(
                    new Label("Không có phiên đấu giá nào thuộc danh mục này."));
        }
    }
}