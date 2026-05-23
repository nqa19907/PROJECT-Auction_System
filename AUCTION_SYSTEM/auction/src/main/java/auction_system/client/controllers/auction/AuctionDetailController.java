package auction_system.client.controllers.auction;

import auction_system.client.models.AuctionDisplayContext;
import auction_system.client.models.AuctionViewModel;
import auction_system.client.services.AuctionService;
import auction_system.client.utils.CurrencyFormatter;
import auction_system.client.utils.Router;
import auction_system.client.utils.ViewConstants;
import auction_system.common.models.auctions.BidRow;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
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
    @FXML private LineChart<String, Number> bidLineChart;
    @FXML private CategoryAxis categoryXaxis;
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

    // ── ViewModel ────────────────────────────────────────────
    private AuctionViewModel viewModel;
    private final XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();

    /** Đồng hồ đếm ngược của phiên đấu giá. */
    private AuctionCountdownTimer countdownTimer;

    /** Hiệu ứng nhấp nháy cho chấm trạng thái trực tuyến. */
    private LiveIndicatorAnimation liveIndicatorAnimation;

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
        AuctionPriceChartConfigurer.updateAxis(
                numberYaxis,
                viewModel.getOpeningPriceValue(),
                priceSeries
        );
        loadBidHistory(context.auctionId());
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
                AuctionPriceChartConfigurer.updateAxis(
                        numberYaxis,
                        viewModel.getOpeningPriceValue(),
                        priceSeries
                );
            });
        });
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

        setupTable();
        setupChart();
        bindViewModel();
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
        AuctionPriceChartConfigurer.configure(bidLineChart, priceSeries);
    }

    /**
     * Khởi động các hiệu ứng realtime của màn hình.
     */
    private void startRealtimeVisuals() {
        liveIndicatorAnimation = new LiveIndicatorAnimation(liveDot);
        liveIndicatorAnimation.start();

        countdownTimer = new AuctionCountdownTimer(
            timerLabel,
            AuctionCountdownTimer.DEFAULT_SECONDS_LEFT
        );
        countdownTimer.start();
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
                    
                    // Tạm thời tự cập nhật giao diện ngay khi nhận phản hồi thành công từ Server
                    viewModel.processNewBid(amount, "Bạn", true);
                    AuctionPriceChartConfigurer.updateAxis(
                            numberYaxis, 
                            viewModel.getOpeningPriceValue(), 
                            priceSeries
                    );
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
        // TODO: đăng ký handler socket để cập nhật bảng/biểu đồ/metric.
    }
}
