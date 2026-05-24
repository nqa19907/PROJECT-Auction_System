package auction_system.client.controllers.auction;

import auction_system.client.models.AuctionDisplayContext;
import auction_system.client.models.AuctionViewModel;
import auction_system.client.network.NetworkClient;
import auction_system.client.services.AuctionService;
import auction_system.client.utils.CurrencyFormatter;
import auction_system.client.utils.Router;
import auction_system.client.utils.ViewConstants;
import auction_system.common.models.auctions.BidRow;
import auction_system.common.network.Protocol;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
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

/* TODO: Lỗi ở bảng bidHistory
    - Bị lỗi bidder2 đặt giá dẫn đầu thì nếu bidder1 vào sau ko
      thấy được sự thay đổi ô "trạng thái" đổi sang dẫn đầu mà chỉ thấy
      hợp lệ.
*/

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
    private final Consumer<String> watchOkHandler = this::handleWatchAuctionSuccess;
    private final Consumer<String> watchFailHandler = this::handleWatchAuctionFailure;
    private final Consumer<String> unwatchOkHandler = this::handleUnwatchAuctionSuccess;
    private final Consumer<String> updatePriceHandler = this::handleRealtimePriceUpdate;

    /** Đồng hồ đếm ngược của phiên đấu giá. */
    private AuctionCountdownTimer countdownTimer;

    /** Hiệu ứng nhấp nháy cho chấm trạng thái trực tuyến. */
    private LiveIndicatorAnimation liveIndicatorAnimation;

    // ── Chỉ số của dữ liệu nhận về ──────────────────────────
    private static final int IDX_AUCTION_ID = 1;

    // ── Tiện ích giao diện realtime ─────────────────────────
    private boolean realtimeCleanedUp = false;

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
        // TODO: Khi AuctionDisplayContext có status, disable bidInput/placeBidBtn
        // nếu status != RUNNING.
        AuctionPriceChartConfigurer.updateAxis(
                numberYaxis,
                viewModel.getOpeningPriceValue(),
                priceSeries
        );
        loadBidHistory(context.auctionId());

        // Sau khi có auctionId, bắt đầu nhận broadcast realtime của phiên này.
        watchAuction(context.auctionId());
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
     * Đăng ký màn hình hiện tại theo dõi realtime một phiên đấu giá.
     *
     * <p>Lệnh WATCH_AUCTION chỉ dùng để server biết client đang xem phiên nào
     * và gửi broadcast realtime về đúng màn hình. Đây không phải điều kiện nghiệp
     * vụ để người dùng được đặt giá.
     *
     * @param auctionIdValue mã phiên đấu giá cần theo dõi realtime
     */
    private void watchAuction(final String auctionIdValue) {
        if (auctionIdValue == null || auctionIdValue.isBlank()) {
            return;
        }

        // Gửi yêu cầu subscribe realtime để server attach client này vào observer list.
        NetworkClient.getInstance().sendCommand(
                Protocol.Command.WATCH_AUCTION.name()
                        + Protocol.SEPARATOR
                        + auctionIdValue
        );

        LOGGER.info("Bắt đầu theo dõi realtime phiên: {}", auctionIdValue);
    }

    /**
     * Hủy đăng ký theo dõi realtime phiên đấu giá hiện tại.
     *
     * <p>Lệnh UNWATCH_AUCTION giúp server gỡ client khỏi observer list của phiên.
     * Cần gọi khi người dùng rời màn chi tiết để tránh nhận update cho màn đã đóng.
     *
     * @param auctionIdValue mã phiên đấu giá cần dừng theo dõi realtime
     */
    private void unwatchAuction(final String auctionIdValue) {
        if (auctionIdValue == null || auctionIdValue.isBlank()) {
            return;
        }

        // Gửi yêu cầu unsubscribe realtime để server detach client khỏi observer list.
        NetworkClient.getInstance().sendCommand(
                Protocol.Command.UNWATCH_AUCTION.name()
                        + Protocol.SEPARATOR
                        + auctionIdValue
        );

        LOGGER.info("Dừng theo dõi realtime phiên: {}", auctionIdValue);
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
        registerRealtimeHandlers();
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
        // TODO: Chặn submit ở client nếu phiên không còn RUNNING,
        // trước khi gửi PLACE_BID lên server.
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

                    // UI sẽ cập nhật qua UPDATE_PRICE rồi reload bid history để đồng bộ với server.
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
        handleExit();
    }

    @FXML
    private void handleExit() {
        LOGGER.info("Hủy bid history và quay lại ItemList");

        unwatchAuction(viewModel.auctionIdProperty().get());
        unregisterRealtimeHandlers();
        cleanupRealtime();
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

    private void cleanupRealtime() {
        if (realtimeCleanedUp) {
            return;
        }

        realtimeCleanedUp = true;
        unwatchAuction(viewModel.auctionIdProperty().get());
        unregisterRealtimeHandlers();
    }

    /**
     * Dọn dẹp hiệu ứng khi scene bị huỷ khỏi JavaFX.
     */
    private void registerLifecycleCleanup() {
        bidInput.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                unwatchAuction(viewModel.auctionIdProperty().get());
                unregisterRealtimeHandlers();
                cleanupRealtime();
                stopUiAnimations();
            }
        });
    }

    /**
     * Đăng ký socket handler để nhận dữ liệu realtime.
     */
    private void registerRealtimeHandlers() {
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.WATCH_OK.name(),
                watchOkHandler
        );

        NetworkClient.getInstance().registerHandler(
                Protocol.Response.WATCH_FAIL.name(),
                watchFailHandler
        );

        NetworkClient.getInstance().registerHandler(
                Protocol.Response.UNWATCH_OK.name(),
                unwatchOkHandler
        );

        NetworkClient.getInstance().registerHandler(
                Protocol.Response.UPDATE_PRICE.name(),
                updatePriceHandler
        );
    }

    /**
     * Xử lý phản hồi khi server xác nhận client đã theo dõi realtime phiên đấu giá.
     *
     * @param response phản hồi WATCH_OK từ server theo định dạng WATCH_OK|auctionId
     */
    private void handleWatchAuctionSuccess(final String response) {
        LOGGER.info("Theo dõi realtime thành công: {}", response);
    }

    /**
     * Xử lý phản hồi khi server từ chối theo dõi realtime phiên đấu giá.
     *
     * @param response phản hồi WATCH_FAIL từ server theo định dạng WATCH_FAIL|message
     */
    private void handleWatchAuctionFailure(final String response) {
        LOGGER.warn("Theo dõi realtime thất bại: {}", response);
    }

    /**
     * Xử lý phản hồi khi server xác nhận client đã dừng theo dõi realtime phiên đấu giá.
     *
     * @param response phản hồi UNWATCH_OK từ server theo định dạng UNWATCH_OK|auctionId
     */
    private void handleUnwatchAuctionSuccess(final String response) {
        LOGGER.info("Dừng theo dõi realtime thành công: {}", response);
    }

    /**
     * Xử lý broadcast realtime khi server thông báo giá mới của một phiên đấu giá.
     *
     * <p>Server hiện gửi dữ liệu theo định dạng:
     * {@code UPDATE_PRICE|auctionId|newPrice}. Controller cần lọc auctionId để
     * chỉ cập nhật màn hình đang xem đúng phiên này.
     *
     * @param response broadcast UPDATE_PRICE từ server
     */
    private void handleRealtimePriceUpdate(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX);

        if (parts.length < 3) {
            LOGGER.warn("Bỏ qua UPDATE_PRICE không hợp lệ: {}", response);
            return;
        }


        final String updatedAuctionId = parts[IDX_AUCTION_ID];
        final String currentAuctionId = viewModel.auctionIdProperty().get();

        if (!updatedAuctionId.equals(currentAuctionId)) {
            LOGGER.info(
                    "Bỏ qua UPDATE_PRICE của phiên khác: received={}, current={}",
                    updatedAuctionId,
                    currentAuctionId
            );
            return;
        }

        LOGGER.info("Nhận UPDATE_PRICE cho phiên hiện tại: {}", response);

        // UPDATE_PRICE hiện chưa chứa đủ bidder/time, nên reload lịch sử để đồng bộ bảng và chart.
        loadBidHistory(updatedAuctionId);
    }

    /**
     * Hủy đăng ký các socket handler thuộc màn hình chi tiết hiện tại.
     *
     * <p>NetworkClient là singleton dùng chung toàn app, nên controller phải gỡ
     * handler khi rời màn hình để controller cũ không tiếp tục nhận broadcast.
     */
    private void unregisterRealtimeHandlers() {
        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.WATCH_OK.name(),
                watchOkHandler
        );

        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.WATCH_FAIL.name(),
                watchFailHandler
        );

        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.UNWATCH_OK.name(),
                unwatchOkHandler
        );

        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.UPDATE_PRICE.name(),
                updatePriceHandler
        );
    }
}
