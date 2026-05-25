package auction_system.client.controllers.auction;

import auction_system.client.models.AuctionDisplayContext;
import auction_system.client.models.AuctionViewModel;
import auction_system.client.services.AuctionService;
import auction_system.client.utils.Router;
import auction_system.client.utils.ViewConstants;
import auction_system.common.models.auctions.BidRow;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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

    // ── ViewModel ────────────────────────────────────────────
    private AuctionViewModel viewModel;
    private final XYChart.Series<Number, Number> priceSeries = new XYChart.Series<>();
    private AuctionBidForm bidForm;
    private AuctionRealtimeSubscription realtimeSubscription;
    private AuctionDetailVisuals visuals;

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
        AuctionPriceChartConfigurer.updateAxes(
                numberXaxis,
                numberYaxis,
                viewModel.getOpeningPriceValue(),
                priceSeries
        );
        startCountdownTimer(context.endTime(), context.status());
        loadBidHistory(context.auctionId());

        // Sau khi có auctionId, bắt đầu nhận broadcast realtime của phiên này.
        realtimeSubscription.watch(context.auctionId());
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
     * Khởi tạo các thành phần giao diện và animation realtime.
     *
     * @param url URL khởi tạo từ JavaFX
     * @param rb ResourceBundle từ JavaFX
     */
    @Override
    public void initialize(final URL url, final ResourceBundle rb) {
        viewModel = new AuctionViewModel();
        bidForm = new AuctionBidForm(bidInput, placeBidBtn, lblError, viewModel);
        realtimeSubscription = new AuctionRealtimeSubscription(
                () -> viewModel.auctionIdProperty().get(),
                this::handleCurrentAuctionUpdated);
        visuals = new AuctionDetailVisuals(timerLabel, liveDot);

        setupTable();
        setupChart();
        bindViewModel();
        bidForm.registerInputListener();
        realtimeSubscription.registerHandlers();
        visuals.start();
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
     * Khởi động đồng hồ dựa trên thời gian kết thúc thật từ server.
     *
     * @param endTime thời gian kết thúc phiên đấu giá
     * @param status trạng thái hiện tại của phiên đấu giá
     */
    private void startCountdownTimer(
            final java.time.LocalDateTime endTime,
            final String status) {
        visuals.startCountdown(endTime, this::markAuctionFinishedOnUi);

        if ("FINISHED".equals(status) || "CANCELED".equals(status)) {
            markAuctionFinishedOnUi();
        }
    }

    /**
     * Cập nhật giao diện khi phiên đã hết giờ hoặc đã bị đóng.
     */
    private void markAuctionFinishedOnUi() {
        timerLabel.setText("Kết thúc");
        placeBidBtn.setDisable(true);
        bidInput.setDisable(true);
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
        bidForm.submit();
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

        cleanupDetailResources();
        Router.navigateContent(bidInput, ViewConstants.ITEM_LIST_VIEW);
    }

    /**
     * Cộng thêm delta vào giá đang nhập.
     *
     * @param delta số tiền cộng thêm
     */
    private void adjustInput(final long delta) {
        if (bidForm != null) {
            bidForm.addToInput(delta);
        }
    }

    private void cleanupDetailResources() {
        if (realtimeSubscription != null) {
            realtimeSubscription.cleanup();
        }

        if (visuals != null) {
            visuals.stop();
        }
    }

    /**
     * Dọn dẹp hiệu ứng khi scene bị huỷ khỏi JavaFX.
     */
    private void registerLifecycleCleanup() {
        bidInput.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                cleanupDetailResources();
            }
        });
    }

    /**
     * Đồng bộ lại bảng và biểu đồ khi realtime báo phiên hiện tại có giá mới.
     *
     * <p>UPDATE_PRICE hiện chưa chứa đủ bidder/time, nên controller vẫn reload
     * lịch sử bid từ server để ViewModel dựng lại trạng thái chuẩn.
     *
     * @param updatedAuctionId mã phiên vừa được server cập nhật
     */
    private void handleCurrentAuctionUpdated(final String updatedAuctionId) {
        loadBidHistory(updatedAuctionId);
    }
}
