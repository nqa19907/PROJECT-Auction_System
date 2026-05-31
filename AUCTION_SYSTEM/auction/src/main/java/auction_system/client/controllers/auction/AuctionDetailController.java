package auction_system.client.controllers.auction;

import auction_system.client.controllers.auction.components.AuctionAntiSnipingControl;
import auction_system.client.controllers.auction.components.AuctionAutoBidForm;
import auction_system.client.controllers.auction.components.AuctionBidForm;
import auction_system.client.controllers.auction.components.AuctionBidTableConfigurer;
import auction_system.client.controllers.auction.components.AuctionCountdownTimer;
import auction_system.client.controllers.auction.components.AuctionPriceChartConfigurer;
import auction_system.client.controllers.auction.components.AuctionRealtimeSubscription;
import auction_system.client.controllers.auction.components.LiveIndicatorAnimation;
import auction_system.client.models.AuctionDisplayContext;
import auction_system.client.models.AuctionViewModel;
import auction_system.client.services.AuctionService;
import auction_system.client.services.UserSessionService;
import auction_system.client.utils.CheckBoxIconUtil;
import auction_system.client.utils.ProductImageStyleUtil;
import auction_system.client.utils.Router;
import auction_system.client.utils.ViewConstants;
import auction_system.common.models.auctions.BidRow;
import auction_system.common.models.users.User;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller cho màn hình chi tiết phiên đấu giá.
 * Quản lý bảng lịch sử, biểu đồ giá, đặt giá và trạng thái realtime.
 *
 * <p>NEW ARCHITECTURE: This controller now uses a ViewModel (AuctionViewModel) to manage
 * the UI state and business logic. The controller's primary responsibility is to
 * bind UI components to the ViewModel and delegate user actions to it.
 */
public class AuctionDetailController implements Initializable {

    /** Logger của controller. */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuctionDetailController.class);
    private static final DateTimeFormatter AUCTION_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String CONDITION_PREFIX = "\nTình trạng: ";

    // ── fx:id fields ─────────────────────────────────────────

    @FXML private Label currentPrice;
    @FXML private Label priceChange;
    @FXML private Label startPrice;
    @FXML private Label bidCount;
    @FXML private Label participantCount;
    @FXML private Label timerCaption;
    @FXML private Label timerLabel;
    @FXML private Label auctionStartTime;
    @FXML private Label auctionEndTime;
    @FXML private Label minBidHint;
    @FXML private Label auctionTitle;
    @FXML private Label productDescription;
    @FXML private Label productCondition;
    @FXML private Label auctionId;
    @FXML private Region productImage;
    @FXML private Region productImagePlaceholderIcon;
    @FXML private Circle liveDot;
    @FXML private LineChart<Number, Number> bidLineChart;
    @FXML private NumberAxis numberXaxis;
    @FXML private NumberAxis numberYaxis;
    @FXML private TableView<BidRow> bidTable;
    @FXML private TableColumn<BidRow, String> colTime;
    @FXML private TableColumn<BidRow, String> colBidder;
    @FXML private TableColumn<BidRow, Double> colPrice;
    @FXML private TableColumn<BidRow, Double> colChange;
    @FXML private TableColumn<BidRow, String> colStatus;
    @FXML private TextField bidInput;
    @FXML private Button placeBidBtn;
    @FXML private Label lblError;
    @FXML private VBox autoBidCard;
    @FXML private VBox antiSnipingCard;
    @FXML private VBox placeBidCard;

    // Auto-bid form controls.
    @FXML private CheckBox autoBidToggle;
    @FXML private TextField autoBidMaxInput;
    @FXML private TextField autoBidStepInput;
    @FXML private Button enableAutoBidBtn;
    @FXML private Label autoBidErrorLabel;
    @FXML private CheckBox chkAntiShipping;

    // ── ViewModel ────────────────────────────────────────────
    private AuctionViewModel viewModel;
    private final XYChart.Series<Number, Number> priceSeries = new XYChart.Series<>();
    private AuctionBidForm bidForm;
    private AuctionAutoBidForm autoBidForm;
    private AuctionAntiSnipingControl antiSnipingControl;
    private AuctionRealtimeSubscription realtimeSubscription;

    /** Đồng hồ đếm ngược của phiên đấu giá. */
    private AuctionCountdownTimer countdownTimer;

    /** Hiệu ứng nhấp nháy cho chấm trạng thái trực tuyến. */
    private LiveIndicatorAnimation liveIndicatorAnimation;

    /** Mã phiên đang được màn hình này theo dõi. */
    private String activeAuctionId;

    /** True nếu user hiện tại là người bán và chỉ được quan sát. */
    private boolean sellerObserveOnly;
    private LocalDateTime activeEndTime;

    // ── Tiện ích giao diện realtime ─────────────────────────

    /**
     * Khởi tạo màn hình theo đầy đủ dữ liệu item được chọn ở ItemList.
     *
     * @param context dữ liệu hiển thị của item/phiên đấu giá
     */

    public void initAuction(final AuctionDisplayContext context) {
        if (context == null) {
            return;
        }

        // Nạp dữ liệu phiên được chọn vào ViewModel và lưu lại trạng thái theo dõi hiện tại.
        viewModel.init(context);
        activeAuctionId = context.auctionId();
        activeEndTime = context.endTime();
        applyProductDetails(context.itemDescription());
        updateAuctionTimeLabels(context.startTime(), context.endTime());
        // Áp dụng ảnh sản phẩm cho phần đầu màn chi tiết.
        applyProductImage(context.imagePath(), context.category());

        // Đăng ký socket room của phiên để chỉ nhận realtime update liên quan.
        realtimeSubscription.start(activeAuctionId);

        // Đồng bộ các control phụ thuộc quyền người bán trước khi bật timer và tải dữ liệu.
        applySellerObserveOnlyPolicy(context);
        autoBidForm.applySellerPolicy(sellerObserveOnly);
        antiSnipingControl.applyInitialState(context.antiSnipingEnabled(), sellerObserveOnly);

        // Khởi tạo lại biểu đồ, đồng hồ, lịch sử bid và trạng thái auto-bid theo phiên mới.
        AuctionPriceChartConfigurer.updateAxes(
                numberXaxis,
                numberYaxis,
                viewModel.getOpeningPriceValue(),
                priceSeries
        );
        startCountdownTimer(context.startTime(), context.endTime(), context.status());
        loadBidHistory(context.auctionId());
        autoBidForm.loadStatus(context.auctionId());
    }

    /**
     * Tải lịch sử đặt giá đã lưu của phiên hiện tại và đưa vào ViewModel.
     *
     * <p>Socket callback có thể chạy trên thread nền, nên mọi cập nhật tới
     * ObservableList/JavaFX binding được đưa về JavaFX Application Thread bằng
     * {@link Platform#runLater(Runnable)}.
     *
     * @param auctionIdValue mã phiên đấu giá cần tải lịch sử bid
     */
    private void loadBidHistory(final String auctionIdValue) {
        AuctionService.getInstance().fetchBidHistory(auctionIdValue, rows -> {
            Platform.runLater(() -> {
                // ViewModel dựng lại bảng, biểu đồ và các chỉ số từ dữ liệu server.
                viewModel.loadBidHistory(rows);

                // Sau khi chart có dữ liệu thật, cập nhật lại trục Y theo khoảng giá mới.
                AuctionPriceChartConfigurer.updateAxes(
                        numberXaxis,
                        numberYaxis,
                        viewModel.getOpeningPriceValue(),
                        priceSeries
                );
            });
        });
    }

    /**
     * Áp dụng ảnh sản phẩm vào vùng ảnh chi tiết.
     *
     * @param imagePath đường dẫn ảnh sản phẩm
     * @param category danh mục sản phẩm
     */
    private void applyProductImage(final String imagePath, final String category) {
        ProductImageStyleUtil.applyImageOrPlaceholder(
                productImage,
                productImagePlaceholderIcon,
                imagePath,
                category);
    }

    /**
     * Tách tình trạng đang lưu kèm mô tả để hiển thị nổi bật ở cuối khối thông tin.
     *
     * @param itemDescription mô tả sản phẩm có thể chứa tình trạng ở dòng cuối
     */
    private void applyProductDetails(final String itemDescription) {
        final String details = itemDescription == null ? "" : itemDescription;
        final int conditionIndex = details.lastIndexOf(CONDITION_PREFIX);
        final boolean hasCondition = conditionIndex >= 0;

        productDescription.setText(hasCondition
                ? details.substring(0, conditionIndex)
                : details);
        productCondition.setText(hasCondition
                ? "Tình trạng: " + details.substring(conditionIndex + CONDITION_PREFIX.length())
                : "");
        productCondition.setManaged(hasCondition);
        productCondition.setVisible(hasCondition);
    }

    /**
     * Nếu người đang đăng nhập là người bán của phiên thì chỉ cho quan sát.
     *
     * @param context dữ liệu phiên đang mở
     */
    private void applySellerObserveOnlyPolicy(final AuctionDisplayContext context) {
        sellerObserveOnly = false;
        final User currentUser = UserSessionService.getInstance().getCurrentUser();
        if (currentUser == null || context.sellerId() == null) {
            // Không xác định được quyền điều khiển.
            // Khóa cấu hình gia hạn để tránh gửi sai request.
            chkAntiShipping.setDisable(true);
            applyRoleVisibility(false);
            return;
        }

        if (context.sellerId().equals(currentUser.getId())) {
            // Người bán không được tự đặt giá, nhưng được chỉnh cấu hình gia hạn phút chót.
            sellerObserveOnly = true;
            placeBidBtn.setDisable(true);
            chkAntiShipping.setDisable(false);
            applyRoleVisibility(true);
            minBidHint.textProperty().unbind();
            minBidHint.setText("Bạn là người bán nên chỉ có thể quan sát phiên này.");
            return;
        }

        chkAntiShipping.setDisable(true);
        applyRoleVisibility(false);
    }

    /**
     * Chỉ hiển thị nhóm thao tác phù hợp với vai trò trong phiên.
     *
     * @param seller true nếu user hiện tại là người bán
     */
    private void applyRoleVisibility(final boolean seller) {
        setVisibleAndManaged(autoBidCard, !seller);
        setVisibleAndManaged(placeBidCard, !seller);
        setVisibleAndManaged(antiSnipingCard, seller);
    }

    private void setVisibleAndManaged(final Region region, final boolean visible) {
        region.setVisible(visible);
        region.setManaged(visible);
    }

    /**
     * Khởi tạo các thành phần giao diện và animation realtime.
     *
     * @param url URL khởi tạo từ JavaFX
     * @param rb ResourceBundle từ JavaFX
     */
    @Override
    public void initialize(final URL url, final ResourceBundle rb) {
        viewModel = new AuctionViewModel();
        bidForm = new AuctionBidForm(
                bidInput,
                placeBidBtn,
                lblError,
                viewModel);
        autoBidForm = new AuctionAutoBidForm(
                autoBidToggle,
                autoBidMaxInput,
                autoBidStepInput,
                enableAutoBidBtn,
                autoBidErrorLabel,
                viewModel);
        antiSnipingControl = new AuctionAntiSnipingControl(
                chkAntiShipping,
                lblError,
                () -> activeAuctionId);
        realtimeSubscription = new AuctionRealtimeSubscription(
                viewModel,
                numberXaxis,
                numberYaxis,
                priceSeries,
                this::applyServerEndTime);

        setupTable();
        setupChart();
        CheckBoxIconUtil.apply(autoBidToggle);
        CheckBoxIconUtil.apply(chkAntiShipping);
        ProductImageStyleUtil.applyRoundedClip(productImage, 14);
        bindViewModel();
        bidForm.registerHandlers();
        autoBidForm.registerHandlers();
        antiSnipingControl.registerHandlers();
        antiSnipingControl.registerNetworkHandlers();
        startRealtimeVisuals();
        registerLifecycleCleanup();
    }

    /**
     * Kết nối các thành phần UI với ViewModel.
     * This is a core part of the new architecture. All UI components are bound
     * to the ViewModel's properties. When the ViewModel's data changes, the UI
     * updates automatically.
     */
    private void bindViewModel() {
        // Binding một chiều giúp UI tự cập nhật khi ViewModel xử lý lịch sử hoặc realtime bid.
        auctionId.textProperty().bind(Bindings.concat("Phiên #", viewModel.auctionIdProperty()));
        auctionTitle.textProperty().bind(viewModel.auctionTitleProperty());
        currentPrice.textProperty().bind(viewModel.currentPriceFormattedProperty());
        startPrice.textProperty().bind(viewModel.openingPriceFormattedProperty());
        bidCount.textProperty().bind(viewModel.bidCountProperty().asString());
        participantCount.textProperty().bind(viewModel.participantCountProperty().asString());
        
        // Ghép chữ tĩnh với giá trị động của currentPriceFormattedProperty
        minBidHint.textProperty().bind(
            Bindings.concat("Giá phải lớn hơn: ", viewModel.currentPriceFormattedProperty())
        );
        priceChange.textProperty().bind(viewModel.priceChangeTextProperty());
        priceSeries.setData(viewModel.getChartData());
    }

    /**
     * Cấu hình các cột của bảng lịch sử đấu giá.
     */
    private void setupTable() {
        AuctionBidTableConfigurer.configure(
            bidTable,
            colTime,
            colBidder,
            colPrice,
            colChange,
            colStatus,
            viewModel.getBidHistory()
        );
    }

    /**
     * Cấu hình biểu đồ lịch sử giá.
     */
    private void setupChart() {
        AuctionPriceChartConfigurer.configure(bidLineChart, numberXaxis, priceSeries);
    }

    /**
     * Khởi động các hiệu ứng realtime của màn hình.
     */
    private void startRealtimeVisuals() {
        liveIndicatorAnimation = new LiveIndicatorAnimation(liveDot);
        liveIndicatorAnimation.start();
    }

    /**
     * Khởi động đồng hồ dựa trên thời gian kết thúc thật từ server.
     *
     * @param startTime thời gian bắt đầu phiên đấu giá
     * @param endTime thời gian kết thúc phiên đấu giá
     * @param status trạng thái hiện tại của phiên đấu giá
     */
    private void startCountdownTimer(
            final LocalDateTime startTime,
            final LocalDateTime endTime,
            final String status) {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        final LocalDateTime now = LocalDateTime.now();
        if ("FINISHED".equals(status) || "CANCELED".equals(status)) {
            // Phiên đã đóng từ server thì không tạo timeline mới.
            markAuctionFinishedOnUi();
            return;
        }

        if (startTime != null && now.isBefore(startTime)) {
            // Chưa tới giờ chạy, timer phải đếm tới thời điểm bắt đầu.
            markAuctionWaitingOnUi();
            countdownTimer = new AuctionCountdownTimer(
                    timerLabel,
                    startTime,
                    () -> markAuctionRunningOnUi(endTime));
            countdownTimer.start();
            return;
        }

        if (endTime == null || !now.isBefore(endTime)) {
            markAuctionFinishedOnUi();
            return;
        }

        markAuctionRunningOnUi(endTime);
    }

    /**
     * Cập nhật giao diện khi phiên chưa tới thời điểm bắt đầu.
     */
    private void markAuctionWaitingOnUi() {
        timerCaption.setText("Bắt đầu sau");
        placeBidBtn.setDisable(true);
        minBidHint.textProperty().unbind();
        minBidHint.setText("Phiên đấu giá chưa bắt đầu.");
    }

    /**
     * Cập nhật giao diện khi phiên đang chạy.
     * Đồng hồ đếm ngược đến thời điểm kết thúc.
     *
     * @param endTime thời gian kết thúc phiên đấu giá
     */
    private void markAuctionRunningOnUi(final LocalDateTime endTime) {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        timerCaption.setText("Thời gian còn lại");
        // Khi phiên bắt đầu, chỉ bidder được mở lại nút đặt giá; người bán vẫn quan sát.
        placeBidBtn.setDisable(sellerObserveOnly);
        minBidHint.textProperty().unbind();
        if (sellerObserveOnly) {
            minBidHint.setText("Bạn là người bán nên chỉ có thể quan sát phiên này.");
        } else {
            minBidHint.textProperty().bind(
                    Bindings.concat(
                            "Giá phải lớn hơn: ",
                            viewModel.currentPriceFormattedProperty())
            );
        }

        countdownTimer = new AuctionCountdownTimer(
                timerLabel,
                endTime,
                this::markAuctionFinishedOnUi);
        countdownTimer.start();
    }

    /**
     * Cập nhật giao diện khi phiên đã hết giờ hoặc đã bị đóng.
     */
    private void markAuctionFinishedOnUi() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        timerCaption.setText("Trạng thái");
        timerLabel.setText("Kết thúc");
        placeBidBtn.setDisable(true);
        minBidHint.textProperty().unbind();
        minBidHint.setText("Phiên đấu giá đã kết thúc.");
    }

    /**
     * Xử lý sự kiện người dùng đặt giá mới.
     * The controller now delegates the entire bidding logic to the ViewModel.
     * It passes the raw input and a callback to handle the result.
     * This keeps the controller clean and focused on UI interaction.
     */
    @FXML
    private void placeBid() {
        bidForm.submitBid();
    }

    @FXML
    private void quickAdd100() {
        bidForm.adjustInput(100_000L);
    }

    @FXML
    private void quickAdd500() {
        bidForm.adjustInput(500_000L);
    }

    @FXML
    private void quickAdd1000() {
        bidForm.adjustInput(1_000_000L);
    }

    @FXML
    private void goBack() {
        handleCancel();
    }

    /**
     * Làm mới dữ liệu phiên đấu giá từ server theo ảnh chụp mới nhất.
     *
     * <p>Luồng này chỉ kéo lại snapshot bằng GET_AUCTION + BID_HISTORY,
     * không thay thế cơ chế realtime UPDATE_PRICE đang chạy sẵn.
     */
    @FXML
    private void refreshAuction() {
        if (activeAuctionId == null || activeAuctionId.isBlank()) {
            return;
        }

        AuctionService.getInstance().fetchAuctionDetail(activeAuctionId, parts -> {
            if (parts == null || parts.length < 12) {
                return;
            }

            try {
                final AuctionDisplayContext context = new AuctionDisplayContext(
                        parts[1],
                        parts[2],
                        parts[3],
                        (long) Double.parseDouble(parts[4]),
                        (long) Double.parseDouble(parts[5]),
                        parts[6],
                        LocalDateTime.parse(parts[7]),
                        LocalDateTime.parse(parts[8]),
                        parts[10],
                        Boolean.parseBoolean(parts[11]),
                        parts.length > 12 ? parts[12] : "",
                        parts.length > 13 ? parts[13] : "");

                Platform.runLater(() -> {
                    viewModel.init(context);
                    activeAuctionId = context.auctionId();
                    activeEndTime = context.endTime();
                    applyProductDetails(context.itemDescription());
                    applyProductImage(context.imagePath(), context.category());
                    applySellerObserveOnlyPolicy(context);
                    autoBidForm.applySellerPolicy(sellerObserveOnly);
                    syncAntiSnipingCheckbox(context.antiSnipingEnabled());
                    AuctionPriceChartConfigurer.updateAxes(
                            numberXaxis,
                            numberYaxis,
                            viewModel.getOpeningPriceValue(),
                            priceSeries
                    );
                    startCountdownTimer(context.startTime(), context.endTime(), context.status());
                    loadBidHistory(context.auctionId());
                });
            } catch (RuntimeException exception) {
                LOGGER.warn("Không thể làm mới dữ liệu phiên đấu giá: {}",
                        activeAuctionId,
                        exception);
            }
        });
    }

    /**
     * Đồng bộ trạng thái checkbox anti-sniping theo snapshot mới từ server.
     *
     * @param antiSnipingEnabled true nếu anti-sniping đang bật
     */
    private void syncAntiSnipingCheckbox(final boolean antiSnipingEnabled) {
        antiSnipingControl.applyInitialState(antiSnipingEnabled, sellerObserveOnly);
    }

    @FXML
    private void handleCancel() {
        LOGGER.info("Hủy bid history và quay lại ItemList");

        stopUiAnimations();
        Router.navigateContent(bidInput, ViewConstants.ITEM_LIST_VIEW);
    }

    /**
     * Dừng toàn bộ animation/timeline đang chạy.
     */
    private void stopUiAnimations() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        if (liveIndicatorAnimation != null) {
            liveIndicatorAnimation.stop();
        }

        antiSnipingControl.unregisterNetworkHandlers();
        realtimeSubscription.stop();
    }

    /**
     * Dọn dẹp hiệu ứng khi scene bị huỷ khỏi JavaFX.
     */
    private void registerLifecycleCleanup() {
        bidInput.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                stopUiAnimations();
            }
        });
    }

    /**
     * Áp dụng thời gian kết thúc mới từ server và khởi động lại đồng hồ đếm ngược.
     *
     * @param rawEndTime thời gian kết thúc mới theo ISO-8601
     */
    private void applyServerEndTime(final String rawEndTime) {
        try {
            final LocalDateTime serverEndTime = LocalDateTime.parse(rawEndTime);
            if (activeEndTime != null && !serverEndTime.isAfter(activeEndTime)) {
                // Chỉ nhận thời gian mới hơn để tránh update cũ làm đồng hồ chạy lùi.
                return;
            }

            // Lưu mốc kết thúc mới và dựng lại timer theo thời gian server vừa gửi.
            activeEndTime = serverEndTime;
            auctionEndTime.setText("Kết thúc: " + formatAuctionTime(serverEndTime));
            markAuctionRunningOnUi(serverEndTime);
        } catch (RuntimeException exception) {
            LOGGER.warn("Không thể đọc thời gian kết thúc mới: {}", rawEndTime);
        }
    }

    /**
     * Hiển thị mốc bắt đầu và kết thúc của phiên đấu giá.
     *
     * @param startTime thời điểm bắt đầu
     * @param endTime thời điểm kết thúc
     */
    private void updateAuctionTimeLabels(
            final LocalDateTime startTime,
            final LocalDateTime endTime) {
        auctionStartTime.setText("Bắt đầu: " + formatAuctionTime(startTime));
        auctionEndTime.setText("Kết thúc: " + formatAuctionTime(endTime));
    }

    /**
     * Format thời gian đấu giá cho UI.
     *
     * @param time thời gian cần format
     * @return chuỗi hiển thị
     */
    private String formatAuctionTime(final LocalDateTime time) {
        if (time == null) {
            return "--/--/---- --:--";
        }

        return time.format(AUCTION_TIME_FORMATTER);
    }

}
