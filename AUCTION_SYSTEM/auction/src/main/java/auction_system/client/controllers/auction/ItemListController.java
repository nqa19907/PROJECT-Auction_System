package auction_system.client.controllers.auction;

import auction_system.client.controllers.components.ProductCardController;
import auction_system.client.models.AuctionDisplayContext;
import auction_system.client.network.NetworkClient;
import auction_system.client.services.AuctionService;
import auction_system.client.utils.CategoryUtil;
import auction_system.client.utils.Router;
import auction_system.client.utils.ViewConstants;
import auction_system.common.constants.AppConstants;
import auction_system.common.network.Protocol;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller điều khiển danh sách các phiên đấu giá.
 */
public class ItemListController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemListController.class);

    /** Vị trí mã phiên trong response danh sách. */
    private static final int IDX_ID = 0;

    /** Vị trí tên sản phẩm trong response danh sách. */
    private static final int IDX_NAME = 1;

    /** Vị trí giá hiện tại trong response danh sách. */
    private static final int IDX_PRICE = 2;

    /** Vị trí trạng thái phiên trong response danh sách. */
    private static final int IDX_STATUS = 3;

    /** Vị trí thời gian bắt đầu trong response danh sách. */
    private static final int IDX_START_TIME = 4;

    /** Vị trí thời gian kết thúc trong response danh sách. */
    private static final int IDX_END_TIME = 5;

    /** Vị trí danh mục sản phẩm trong response danh sách. */
    private static final int IDX_CATEGORY = 6;

    /** Vị trí giá khởi điểm trong response danh sách. */
    private static final int IDX_OPENING_PRICE = 7;

    /** Vị trí mã người bán trong response danh sách. */
    private static final int IDX_SELLER_ID = 8;

    /** Số trường tối thiểu của một dòng response hợp lệ. */
    private static final int MIN_PARTS_LENGTH = 8;

    @FXML private FlowPane productsGrid;
    @FXML private Label categoryTitle;

    private List<String[]> allAuctions = new ArrayList<>();
    private String filterCategory = AppConstants.CATEGORY_ALL;
    private PauseTransition nextExpiryRefresh;
    private boolean cleanupRegistered;
    private final Consumer<String> auctionCreatedHandler = this::handleAuctionCreated;

    /**
     * Khởi tạo giao diện và lấy dữ liệu phiên đấu giá.
     */
    @FXML
    public void initialize() {
        LOGGER.info("Đang khởi tạo màn hình danh sách đấu giá...");

        if (productsGrid != null) {
            productsGrid.setHgap(12);
            productsGrid.setVgap(12);
        }

        registerRealtimeHandlers();
        refreshAuctionList();
    }

    /**
     * Tải lại danh sách phiên từ server và render lên dashboard.
     */
    private void refreshAuctionList() {
        AuctionService.getInstance().fetchAuctionList(auctionList -> Platform.runLater(() -> {
            this.allAuctions = auctionList;
            renderGrid();
            registerLifecycleCleanup();
        }));
    }

    /**
     * Đăng ký nhận sự kiện tạo phiên mới để dashboard cập nhật realtime.
     */
    private void registerRealtimeHandlers() {
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.AUCTION_CREATED.name(),
                auctionCreatedHandler);
    }

    /**
     * Xử lý sự kiện có phiên đấu giá mới từ server.
     *
     * @param response thông điệp AUCTION_CREATED
     */
    private void handleAuctionCreated(final String response) {
        refreshAuctionList();
    }

    /**
     * Tạo một card sản phẩm từ dữ liệu phiên đấu giá.
     *
     * @param parts dữ liệu phiên đấu giá từ server
     */
    private void createProductCard(final String[] parts) {
        if (parts == null || parts.length < MIN_PARTS_LENGTH) {
            LOGGER.warn("Dữ liệu sản phẩm không hợp lệ: thiếu trường thông tin.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(ViewConstants.PRODUCT_CARD_VIEW));
            VBox card = loader.load();
            ProductCardController controller = loader.getController();

            String auctionId = parts[IDX_ID];
            String itemName = parts[IDX_NAME];
            double currentPrice = Double.parseDouble(parts[IDX_PRICE]);

            controller.setCardDetails(
                    auctionId,
                    itemName,
                    currentPrice,
                    selectedItemId -> navigateToAuctionDetail(parts)
            );

            productsGrid.getChildren().add(card);
        } catch (IOException | NumberFormatException e) {
            LOGGER.error("Lỗi khi tạo thẻ sản phẩm.", e);
        }
    }

    /**
     * Chuyển sang màn hình chi tiết phiên đấu giá.
     *
     * @param selectedParts dữ liệu phiên được chọn
     */
    private void navigateToAuctionDetail(final String[] selectedParts) {
        if (selectedParts == null || selectedParts.length < MIN_PARTS_LENGTH) {
            LOGGER.warn("Không thể mở AuctionDetail vì dữ liệu item không hợp lệ.");
            return;
        }

        String selectedItemId = selectedParts[IDX_ID];
        String itemName = selectedParts[IDX_NAME];
        String status = selectedParts[IDX_STATUS];
        LocalDateTime startTime;
        LocalDateTime endTime;

        long currentPrice;
        try {
            currentPrice = (long) Double.parseDouble(selectedParts[IDX_PRICE]);
        } catch (NumberFormatException e) {
            LOGGER.error("Giá hiện tại không hợp lệ cho item ID: " + selectedItemId, e);
            return;
        }

        try {
            startTime = LocalDateTime.parse(selectedParts[IDX_START_TIME]);
        } catch (DateTimeParseException e) {
            LOGGER.error(
                    "Thời gian bắt đầu không hợp lệ cho item ID: " + selectedItemId,
                    e);
            return;
        }

        try {
            endTime = LocalDateTime.parse(selectedParts[IDX_END_TIME]);
        } catch (DateTimeParseException e) {
            LOGGER.error("Thời gian kết thúc không hợp lệ cho item ID: " + selectedItemId, e);
            return;
        }

        long openingPrice = Math.max(0L, currentPrice - 1_000_000L);
        if (selectedParts.length > IDX_OPENING_PRICE) {
            try {
                String rawPrice = selectedParts[IDX_OPENING_PRICE].replaceAll("[^0-9]", "");
                openingPrice = Long.parseLong(rawPrice);
            } catch (NumberFormatException e) {
                LOGGER.warn("Giá khởi điểm không hợp lệ, tạm dùng giá hiện tại trừ 1.000.000.");
            }
        }

        LOGGER.info("Người dùng muốn đấu giá cho sản phẩm có ID: " + selectedItemId);

        AuctionDetailController auctionDetailController =
                Router.navigateContentAndGetController(
                        productsGrid, ViewConstants.AUCTION_DETAIL_VIEW);

        if (auctionDetailController != null) {
            auctionDetailController.initAuction(
                    new AuctionDisplayContext(
                            selectedItemId,
                            itemName,
                            openingPrice,
                            currentPrice,
                            status,
                            startTime,
                            endTime,
                            selectedParts.length > IDX_SELLER_ID ? selectedParts[IDX_SELLER_ID] : ""
                    )
            );
        }
    }

    /**
     * Thiết lập danh mục lọc và hiển thị lại lưới sản phẩm.
     *
     * @param category tên danh mục cần lọc
     */
    public void setFilterCategory(final String category) {
        this.filterCategory = category;

        if (categoryTitle != null) {
            categoryTitle.setText(CategoryUtil.getTitleByCategory(category));
        }

        if (!allAuctions.isEmpty()) {
            renderGrid();
        }
    }

    /**
     * Vẽ lại lưới sản phẩm, bỏ qua các phiên đã kết thúc hoặc đã bị hủy.
     */
    private void renderGrid() {
        if (productsGrid == null) {
            return;
        }

        stopNextExpiryRefresh();
        productsGrid.getChildren().clear();

        boolean hasItems = false;
        for (String[] parts : allAuctions) {
            if (!isAuctionVisible(parts)) {
                continue;
            }

            String itemCategory = (parts.length > IDX_CATEGORY)
                    ? parts[IDX_CATEGORY] : AppConstants.CATEGORY_ALL;

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

        scheduleNextExpiryRefresh();
    }

    /**
     * Kiểm tra phiên còn hiển thị được trên danh sách hay không.
     *
     * @param parts dữ liệu phiên đấu giá từ server
     * @return true nếu phiên chưa kết thúc và chưa bị hủy
     */
    private boolean isAuctionVisible(final String[] parts) {
        if (parts == null || parts.length < MIN_PARTS_LENGTH) {
            return false;
        }

        String status = parts[IDX_STATUS];
        if ("FINISHED".equals(status) || "CANCELED".equals(status)) {
            return false;
        }

        try {
            return LocalDateTime.parse(parts[IDX_END_TIME]).isAfter(LocalDateTime.now());
        } catch (DateTimeParseException e) {
            LOGGER.warn("Bỏ qua phiên có thời gian kết thúc không hợp lệ.", e);
            return false;
        }
    }

    /**
     * Hẹn lần render kế tiếp đúng lúc phiên gần nhất hết hạn.
     */
    private void scheduleNextExpiryRefresh() {
        LocalDateTime nextEndTime = null;

        for (String[] parts : allAuctions) {
            if (!isAuctionVisible(parts)) {
                continue;
            }

            LocalDateTime endTime = LocalDateTime.parse(parts[IDX_END_TIME]);
            if (nextEndTime == null || endTime.isBefore(nextEndTime)) {
                nextEndTime = endTime;
            }
        }

        if (nextEndTime == null) {
            return;
        }

        long millisUntilEnd = java.time.Duration
                .between(LocalDateTime.now(), nextEndTime)
                .toMillis();
        nextExpiryRefresh = new PauseTransition(
                Duration.millis(Math.max(1L, millisUntilEnd + 100L)));
        nextExpiryRefresh.setOnFinished(event -> renderGrid());
        nextExpiryRefresh.play();
    }

    /**
     * Dừng lịch render hết hạn cũ trước khi dựng lại danh sách.
     */
    private void stopNextExpiryRefresh() {
        if (nextExpiryRefresh != null) {
            nextExpiryRefresh.stop();
            nextExpiryRefresh = null;
        }
    }

    /**
     * Dọn timer khi màn danh sách bị thay khỏi scene.
     */
    private void registerLifecycleCleanup() {
        if (cleanupRegistered || productsGrid == null) {
            return;
        }

        cleanupRegistered = true;
        productsGrid.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                stopNextExpiryRefresh();
                NetworkClient.getInstance().unregisterHandler(
                        Protocol.Response.AUCTION_CREATED.name(),
                        auctionCreatedHandler);
            }
        });
    }
}
