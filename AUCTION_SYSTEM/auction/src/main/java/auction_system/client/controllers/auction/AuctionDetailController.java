package auction_system.client.controllers.auction;

import auction_system.client.controllers.auction.components.AuctionAutoBidForm;
import auction_system.client.controllers.auction.components.AuctionBidTableConfigurer;
import auction_system.client.controllers.auction.components.AuctionCountdownTimer;
import auction_system.client.controllers.auction.components.AuctionPriceChartConfigurer;
import auction_system.client.controllers.auction.components.LiveIndicatorAnimation;
import auction_system.client.models.AuctionDisplayContext;
import auction_system.client.models.AuctionViewModel;
import auction_system.client.network.NetworkClient;
import auction_system.client.services.AuctionService;
import auction_system.client.services.UserSessionService;
import auction_system.client.utils.CurrencyFormatter;
import auction_system.client.utils.Router;
import auction_system.client.utils.ViewConstants;
import auction_system.common.models.auctions.BidRow;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.function.Consumer;
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

    private static final int MIN_UPDATE_PRICE_PARTS = 3;
    private static final int IDX_UPDATE_AUCTION_ID = 1;
    private static final int IDX_UPDATE_AMOUNT = 2;
    private static final int IDX_UPDATE_BIDDER = 3;
    private static final int IDX_UPDATE_TIME = 4;
    private static final int IDX_UPDATE_END_TIME = 5;
    private static final int MIN_AUCTION_EXTENDED_PARTS = 3;
    private static final int IDX_EXTENDED_AUCTION_ID = 1;
    private static final int IDX_EXTENDED_END_TIME = 2;
    private static final int MIN_ANTI_SNIPING_UPDATE_PARTS = 3;
    private static final int IDX_ANTI_SNIPING_AUCTION_ID = 1;
    private static final int IDX_ANTI_SNIPING_ENABLED = 2;
    private static final int MIN_ANTI_SNIPING_FAIL_PARTS = 2;
    private static final int IDX_ANTI_SNIPING_FAIL_MESSAGE = 1;

    // ── fx:id fields ─────────────────────────────────────────

    @FXML private Label currentPrice;
    @FXML private Label priceChange;
    @FXML private Label startPrice;
    @FXML private Label bidCount;
    @FXML private Label participantCount;
    @FXML private Label timerLabel;
    @FXML private Label minBidHint;
    @FXML private Label auctionTitle;
    @FXML private Label auctionId;
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
    private AuctionAutoBidForm autoBidForm;

    /** Đồng hồ đếm ngược của phiên đấu giá. */
    private AuctionCountdownTimer countdownTimer;

    /** Hiệu ứng nhấp nháy cho chấm trạng thái trực tuyến. */
    private LiveIndicatorAnimation liveIndicatorAnimation;

    /** Handler nhận cập nhật giá realtime từ socket. */
    private final Consumer<String> updatePriceHandler = this::handleRealtimePriceUpdate;
    private final Consumer<String> auctionExtendedHandler = this::handleAuctionExtended;
    private final Consumer<String> antiSnipingUpdatedHandler = this::handleAntiSnipingUpdated;
    private final Consumer<String> antiSnipingFailHandler = this::handleAntiSnipingUpdateFail;

    /** Mã phiên đang được màn hình này theo dõi. */
    private String activeAuctionId;

    /** True nếu user hiện tại là người bán và chỉ được quan sát. */
    private boolean sellerObserveOnly;
    private boolean syncingAntiSniping;
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

        viewModel.init(context);
        activeAuctionId = context.auctionId();
        activeEndTime = context.endTime();
        AuctionService.getInstance().joinAuction(activeAuctionId);
        syncAntiSnipingCheckbox(context.antiSnipingEnabled());
        applySellerObserveOnlyPolicy(context);
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
     * Nếu người đang đăng nhập là người bán của phiên thì chỉ cho quan sát.
     *
     * @param context dữ liệu phiên đang mở
     */
    private void applySellerObserveOnlyPolicy(final AuctionDisplayContext context) {
        final User currentUser = UserSessionService.getInstance().getCurrentUser();
        if (currentUser == null || context.sellerId() == null) {
            chkAntiShipping.setDisable(true);
            return;
        }

        if (context.sellerId().equals(currentUser.getId())) {
            sellerObserveOnly = true;
            placeBidBtn.setDisable(true);
            chkAntiShipping.setDisable(false);
            minBidHint.textProperty().unbind();
            minBidHint.setText("Bạn là người bán nên chỉ có thể quan sát phiên này.");
            return;
        }

        chkAntiShipping.setDisable(true);
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
        autoBidForm = new AuctionAutoBidForm(
                autoBidToggle,
                autoBidMaxInput,
                autoBidStepInput,
                enableAutoBidBtn,
                autoBidErrorLabel,
                viewModel);

        setupTable();
        setupChart();
        bindViewModel();
        autoBidForm.registerHandlers();
        setupInputListeners();
        setupNetworkHandlers();
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
     * Đăng ký sự kiện lắng nghe thay đổi nội dung ô nhập giá.
     */
    private void setupInputListeners() {
        bidInput.textProperty().addListener((obs, oldVal, newVal) -> {
            if (lblError.isVisible()) {
                lblError.setVisible(false);
                lblError.setManaged(false);
            }
        });

        chkAntiShipping.selectedProperty().addListener((obs, oldVal, newVal) ->
                handleAntiSnipingToggle(newVal));
    }

    /**
     * Gửi yêu cầu bật/tắt chống đặt giá phút chót khi seller thao tác checkbox.
     *
     * @param enabled trạng thái checkbox mới
     */
    private void handleAntiSnipingToggle(final boolean enabled) {
        if (syncingAntiSniping || !sellerObserveOnly || activeAuctionId == null) {
            return;
        }

        chkAntiShipping.setDisable(true);
        AuctionService.getInstance().setAntiSniping(activeAuctionId, enabled);
    }

    /**
     * Đồng bộ checkbox từ dữ liệu server mà không gửi ngược lại một request mới.
     *
     * @param enabled trạng thái chống đặt giá phút chót trên server
     */
    private void syncAntiSnipingCheckbox(final boolean enabled) {
        syncingAntiSniping = true;
        chkAntiShipping.setSelected(enabled);
        syncingAntiSniping = false;
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

        if ("FINISHED".equals(status) || "CANCELED".equals(status)) {
            markAuctionFinishedOnUi();
            return;
        }

        if ("OPEN".equals(status) && LocalDateTime.now().isBefore(startTime)) {
            markAuctionWaitingOnUi();
            countdownTimer = new AuctionCountdownTimer(
                    timerLabel,
                    startTime,
                    () -> markAuctionRunningOnUi(endTime));
            countdownTimer.start();
            return;
        }

        markAuctionRunningOnUi(endTime);
    }

    /**
     * Cập nhật giao diện khi phiên chưa tới thời điểm bắt đầu.
     */
    private void markAuctionWaitingOnUi() {
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
        lblError.setVisible(false);
        lblError.setManaged(false);

        final String rawAmount = bidInput.getText();
        if (rawAmount == null || rawAmount.trim().isEmpty()) {
            // Cảnh báo ô nhập đang rỗng ngay dưới TextField
            lblError.setText("Vui lòng nhập số tiền.");
            lblError.setVisible(true);
            lblError.setManaged(true);
            return;
        }

        // Tạm khóa nút để tránh người dùng bấm nhiều lần trong lúc chờ server phản hồi.
        placeBidBtn.setDisable(true);
        placeBidBtn.setText("Đang gửi...");

        // Chuyển toàn bộ logic đặt giá (kiểm tra dữ liệu, gọi service) sang ViewModel.
        viewModel.submitBid(rawAmount, (success, message, newBalance) -> {
            // Callback này có thể chạy từ luồng mạng.
            // Mọi cập nhật UI phải được đưa về JavaFX Application Thread.
            Platform.runLater(() -> {
                // Luôn mở lại nút và khôi phục nội dung sau khi có phản hồi.
                placeBidBtn.setDisable(false);
                placeBidBtn.setText("Đặt giá ngay  →");
                keepFocusInBidInput();

                if (success) {
                    final long amount = Long.parseLong(rawAmount.replaceAll("[^0-9]", ""));

                    // Khi thành công, xóa ô nhập và ẩn thông báo lỗi
                    // UI sẽ cập nhật qua broadcast realtime (UPDATE_PRICE)
                    // từ server để đảm bảo mọi client đồng bộ cùng một trạng thái.
                    bidInput.clear();
                    lblError.setVisible(false);
                    lblError.setManaged(false);
                    String formattedBidAmount = CurrencyFormatter.formatAmount(amount);
                    String formattedBalance = CurrencyFormatter.formatAmount(newBalance);
                    LOGGER.info("Đặt giá thành công với số tiền: {}", formattedBidAmount);
                    LOGGER.info("Số dư mới sau khi đặt giá: {}", formattedBalance);

                } else {
                    // Khi thất bại, hiển thị lỗi ngay dưới ô nhập thay vì Alert
                    lblError.setText(message);
                    lblError.setVisible(true);
                    lblError.setManaged(true);
                }
            });
        });
    }

    /**
     * Giữ focus trong form đặt giá sau khi nút gửi bị disable/enable.
     *
     * <p>Khi nút đặt giá bị disable trong lúc chờ server, JavaFX có thể tự
     * chuyển focus sang control kế tiếp trong scene, hiện là ô nạp tiền ở
     * sidebar. Request focus lại input đặt giá để người dùng không bị nhảy
     * khỏi ngữ cảnh đấu giá.
     */
    private void keepFocusInBidInput() {
        bidInput.requestFocus();
    }

    @FXML
    private void quickAdd100() {
        adjustInput(100_000L);
    }

    @FXML
    private void quickAdd500() {
        adjustInput(500_000L);
    }

    @FXML
    private void quickAdd1000() {
        adjustInput(1_000_000L);
    }

    @FXML
    private void goBack() {
        handleCancel();
    }

    @FXML
    private void handleCancel() {
        LOGGER.info("Hủy bid history và quay lại ItemList");

        stopUiAnimations();
        Router.navigateContent(bidInput, ViewConstants.ITEM_LIST_VIEW);
    }

    /**
     * Cộng thêm delta vào giá đang nhập.
     *
     * @param delta số tiền cộng thêm
     */
    private void adjustInput(final long delta) {
        final String raw = bidInput.getText().replaceAll("[^0-9]", "");
        final long base = raw.isEmpty() ? viewModel.getCurrentPriceValue() : Long.parseLong(raw);

        bidInput.setText(String.valueOf(base + delta));
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

        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.UPDATE_PRICE.name(),
                updatePriceHandler);
        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.AUCTION_EXTENDED.name(),
                auctionExtendedHandler);
        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.ANTI_SNIPING_UPDATED.name(),
                antiSnipingUpdatedHandler);
        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.ANTI_SNIPING_UPDATE_FAIL.name(),
                antiSnipingFailHandler);
        AuctionService.getInstance().leaveAuction(activeAuctionId);
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
     * Đăng ký socket handler để nhận dữ liệu realtime.
     */
    private void setupNetworkHandlers() {
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.UPDATE_PRICE.name(),
                updatePriceHandler);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.AUCTION_EXTENDED.name(),
                auctionExtendedHandler);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ANTI_SNIPING_UPDATED.name(),
                antiSnipingUpdatedHandler);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ANTI_SNIPING_UPDATE_FAIL.name(),
                antiSnipingFailHandler);
    }

    /**
     * Cập nhật giá, bảng bid history và biểu đồ ngay khi server broadcast bid mới.
     *
     * @param response thông điệp UPDATE_PRICE từ server
     */
    private void handleRealtimePriceUpdate(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
        if (parts.length < MIN_UPDATE_PRICE_PARTS
                || activeAuctionId == null
                || !activeAuctionId.equals(parts[IDX_UPDATE_AUCTION_ID])) {
            return;
        }

        try {
            final long amount = (long) Double.parseDouble(parts[IDX_UPDATE_AMOUNT]);
            final String bidderName = parts.length > IDX_UPDATE_BIDDER
                    ? parts[IDX_UPDATE_BIDDER]
                    : "";
            final String bidTime = parts.length > IDX_UPDATE_TIME
                    ? formatBidTime(parts[IDX_UPDATE_TIME])
                    : DateTimeFormatter.ofPattern("HH:mm:ss").format(java.time.LocalTime.now());
            final User currentUser = UserSessionService.getInstance().getCurrentUser();
            final boolean isCurrentUser = currentUser != null
                    && bidderName.equals(currentUser.getUsername());

            viewModel.processRealtimeBid(amount, bidderName, bidTime, isCurrentUser);
            if (parts.length > IDX_UPDATE_END_TIME) {
                applyServerEndTime(parts[IDX_UPDATE_END_TIME]);
            }
            AuctionPriceChartConfigurer.updateAxes(
                    numberXaxis,
                    numberYaxis,
                    viewModel.getOpeningPriceValue(),
                    priceSeries);
        } catch (NumberFormatException exception) {
            LOGGER.warn("Không thể đọc giá realtime: {}", response);
        }
    }

    /**
     * Cập nhật đồng hồ khi server thông báo phiên đã được gia hạn.
     *
     * @param response thông báo AUCTION_EXTENDED từ server
     */
    private void handleAuctionExtended(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
        if (parts.length < MIN_AUCTION_EXTENDED_PARTS
                || activeAuctionId == null
                || !activeAuctionId.equals(parts[IDX_EXTENDED_AUCTION_ID])) {
            return;
        }

        applyServerEndTime(parts[IDX_EXTENDED_END_TIME]);
    }

    /**
     * Cập nhật checkbox khi server xác nhận trạng thái chống đặt giá phút chót.
     *
     * @param response thông báo ANTI_SNIPING_UPDATED từ server
     */
    private void handleAntiSnipingUpdated(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
        if (parts.length < MIN_ANTI_SNIPING_UPDATE_PARTS
                || activeAuctionId == null
                || !activeAuctionId.equals(parts[IDX_ANTI_SNIPING_AUCTION_ID])) {
            return;
        }

        syncAntiSnipingCheckbox(Boolean.parseBoolean(parts[IDX_ANTI_SNIPING_ENABLED]));
        chkAntiShipping.setDisable(!sellerObserveOnly);
    }

    /**
     * Hiển thị lỗi khi server từ chối yêu cầu bật/tắt chống đặt giá phút chót.
     *
     * @param response thông báo ANTI_SNIPING_UPDATE_FAIL từ server
     */
    private void handleAntiSnipingUpdateFail(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
        final String message = parts.length >= MIN_ANTI_SNIPING_FAIL_PARTS
                ? parts[IDX_ANTI_SNIPING_FAIL_MESSAGE]
                : "Không thể cập nhật chống đặt giá phút chót.";

        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
        syncAntiSnipingCheckbox(!chkAntiShipping.isSelected());
        chkAntiShipping.setDisable(!sellerObserveOnly);
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
                return;
            }

            activeEndTime = serverEndTime;
            markAuctionRunningOnUi(serverEndTime);
        } catch (RuntimeException exception) {
            LOGGER.warn("Không thể đọc thời gian kết thúc mới: {}", rawEndTime);
        }
    }

    /**
     * Định dạng timestamp server gửi về thành giờ/phút/giây cho bảng và chart.
     *
     * @param rawTime timestamp ISO từ server
     * @return chuỗi thời gian ngắn dùng cho UI
     */
    private String formatBidTime(final String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return DateTimeFormatter.ofPattern("HH:mm:ss").format(java.time.LocalTime.now());
        }

        try {
            return LocalDateTime.parse(rawTime).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        } catch (RuntimeException exception) {
            return rawTime;
        }
    }
}
