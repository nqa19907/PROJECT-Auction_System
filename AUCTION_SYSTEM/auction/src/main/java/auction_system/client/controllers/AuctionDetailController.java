package auction_system.client.controllers;

import auction_system.client.utils.CurrencyFormatter;
import auction_system.common.models.auctions.BidRow;
import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for BidHistoryView.
 */
public class AuctionDetailController implements Initializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionDetailController.class);

    @FXML
    private Label currentPrice;
    @FXML
    private Label priceChange;
    @FXML
    private Label startPrice;
    @FXML
    private Label bidCount;
    @FXML
    private Label participantCount;
    @FXML
    private Label timerLabel;
    @FXML
    private Label minBidHint;
    @FXML
    private Label auctionTitle;
    @FXML
    private Label auctionId;
    @FXML
    private Circle liveDot;
    @FXML
    private LineChart<String, Number> bidLineChart;
    @FXML
    private CategoryAxis categoryXaxis;
    @FXML
    private NumberAxis numberYaxis;
    @FXML
    private TableView<BidRow> bidTable;
    @FXML
    private TableColumn<BidRow, String> colTime;
    @FXML
    private TableColumn<BidRow, String> colBidder;
    @FXML
    private TableColumn<BidRow, Double> colPrice;
    @FXML
    private TableColumn<BidRow, Double> colChange;
    @FXML
    private TableColumn<BidRow, String> colStatus;
    @FXML
    private TextField bidInput;

    private final ObservableList<BidRow> tableData = FXCollections.observableArrayList();
    private final XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final Set<String> bidders = new HashSet<>();

    private long lastPrice = 15_500_000L;
    private long openingPrice = 3_500_000L;
    private int secondsLeft = 14 * 60 + 32;
    private int totalBids = 0;
    private int totalParticipants = 0;
    private String currentAuctionId = "AUC-2024-0518";
    private String currentAuctionTitle = "Đồng hồ Rolex Submariner Date";

    private Timeline countdownTimeline;
    private FadeTransition liveDotTransition;

    /**
     * Khởi tạo màn hình theo auctionId được truyền từ ItemList.
     *
     * @param auctionId mã phiên đấu giá
     */
    public void initAuction(final String auctionId) {
        this.currentAuctionId = auctionId;
        loadSampleData();
        joinAuctionRoom(auctionId);
    }

    public void setAuctionId(final String auctionId) {
        initAuction(auctionId);
    }

    /**
     * Khởi tạo màn hình theo đầy đủ dữ liệu item được chọn ở ItemList.
     *
     * @param context dữ liệu hiển thị của item/phiên đấu giá
     */
    public void initAuction(final AuctionDisplayContext context) {
        if (context == null) {
            return;
        }
        this.currentAuctionId = context.auctionId();
        this.currentAuctionTitle = context.itemTitle();
        this.openingPrice = context.openingPrice();
        this.lastPrice = context.currentPrice();
        applyAuctionHeader();
        loadSampleData();
        joinAuctionRoom(context.auctionId());
    }

    @Override
    public void initialize(final URL url, final ResourceBundle rb) {
        setupTable();
        setupChart();
        setupNetworkHandlers();
        joinAuctionRoom();
        loadSampleData();
        startLiveDotAnimation();
        startCountdown();
        registerLifecycleCleanup();
    }

    private void setupTable() {
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colBidder.setCellValueFactory(new PropertyValueFactory<>("bidder"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colChange.setCellValueFactory(new PropertyValueFactory<>("change"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(final Double value, final boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(CurrencyFormatter.formatVnd(value.longValue()));
                }
            }
        });

        colChange.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(final Double value, final boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                if (value > 0) {
                    setText("+" + CurrencyFormatter.formatVnd(value.longValue()));
                    setStyle("-fx-text-fill: #2E7D52; -fx-font-weight: bold;");
                } else {
                    setText("-");
                    setStyle("-fx-text-fill: #A07040;");
                }
            }
        });

        bidTable.setItems(tableData);
    }

    private void setupChart() {
        priceSeries.setName("Giá trả");
        bidLineChart.getData().add(priceSeries);
        bidLineChart.setLegendVisible(false);
        bidLineChart.setCreateSymbols(true);
        bidLineChart.setAnimated(true);
    }

    private void loadSampleData() {
        tableData.clear();
        priceSeries.getData().clear();
        bidders.clear();
        totalBids = 0;
        totalParticipants = 0;
        LocalTime time = LocalTime.of(9, 0, 0);
        final long priceRange = Math.max(100_000L, lastPrice - openingPrice);
        final long step1 = Math.max(10_000L, priceRange / 10);
        final long step2 = Math.max(10_000L, priceRange / 8);
        final long step3 = Math.max(10_000L, priceRange / 6);
        final long p2 = openingPrice + step1;
        final long p3 = p2 + step1;
        final long p4 = p3 + step2;
        final long p5 = p4 + step2;
        final long p6 = p5 + step3;
        final long p7 = p6 + step3;
        final long p8 = Math.max(p7 + step2, lastPrice);

        time = addSampleBid(time, openingPrice, 0L, "Nguyen Van A", "Hợp lệ");
        time = addSampleBid(time, p2, p2 - openingPrice, "Tran Thi B", "Hợp lệ");
        time = addSampleBid(time, p3, p3 - p2, "Cong ty XYZ", "Hợp lệ");
        time = addSampleBid(time, p4, p4 - p3, "Le Minh C", "Hợp lệ");
        time = addSampleBid(time, p5, p5 - p4, "Pham Duc D", "Hợp lệ");
        time = addSampleBid(time, p6, p6 - p5, "Nguyen Van A", "Hợp lệ");
        time = addSampleBid(time, p7, p7 - p6, "Tran Thi B", "Hợp lệ");
        addSampleBid(time, p8, p8 - p7, "Cong ty XYZ", "Dẫn đầu");

        lastPrice = p8;
        applyAuctionHeader();
        updateMetrics();
        updateChartAxis(lastPrice);
    }

    private LocalTime addSampleBid(
            final LocalTime time,
            final long price,
            final long change,
            final String bidder,
            final String status
    ) {
        final String timeStr = time.format(timeFmt);
        tableData.add(0, new BidRow(timeStr, bidder, price, change, status));
        priceSeries.getData().add(new XYChart.Data<>(timeStr, price));
        totalBids++;
        bidders.add(bidder);
        totalParticipants = bidders.size();
        if (totalBids == 1) {
            openingPrice = price;
        }
        lastPrice = price;
        return time.plusMinutes(1).plusSeconds(30);
    }

    private void updateMetrics() {
        currentPrice.setText(CurrencyFormatter.formatVnd(lastPrice));
        startPrice.setText(CurrencyFormatter.formatVnd(openingPrice));
        bidCount.setText(String.valueOf(totalBids));
        participantCount.setText(String.valueOf(totalParticipants));
        minBidHint.setText("Giá tối thiểu: " + CurrencyFormatter.formatVnd(lastPrice + 100_000L));
    }

    private void updateChartAxis(final long price) {
        final double padding = Math.max(50_000D, price * 0.1);
        final double suggestedLower = Math.max(0D, openingPrice - padding);
        final double suggestedUpper = price + padding;
        if (suggestedLower < numberYaxis.getLowerBound()) {
            numberYaxis.setLowerBound(suggestedLower);
        }
        if (suggestedUpper > numberYaxis.getUpperBound()) {
            numberYaxis.setUpperBound(suggestedUpper);
        }
    }

    private void startLiveDotAnimation() {
        liveDotTransition = new FadeTransition(Duration.millis(900), liveDot);
        liveDotTransition.setFromValue(1.0);
        liveDotTransition.setToValue(0.2);
        liveDotTransition.setCycleCount(Animation.INDEFINITE);
        liveDotTransition.setAutoReverse(true);
        liveDotTransition.play();
    }

    private void startCountdown() {
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (secondsLeft > 0) {
                secondsLeft--;
            }
            final int minutes = secondsLeft / 60;
            final int seconds = secondsLeft % 60;
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
            if (secondsLeft == 0) {
                timerLabel.setText("Kết thúc");
            }
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    @FXML
    private void placeBid() {
        final String raw = bidInput.getText().replaceAll("[^0-9]", "");
        if (raw.isEmpty()) {
            return;
        }

        final long amount = Long.parseLong(raw);
        if (amount <= lastPrice) {
            final Alert alert = new Alert(
                    Alert.AlertType.WARNING,
                    "Giá phải lớn hơn " + CurrencyFormatter.formatVnd(lastPrice),
                    ButtonType.OK
            );
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        final long change = amount - lastPrice;
        lastPrice = amount;
        totalBids++;
        bidders.add("Bạn");
        totalParticipants = bidders.size();
        final String timeStr = LocalTime.now().format(timeFmt);
        tableData.add(0, new BidRow(timeStr, "Bạn", amount, change, "Dẫn đầu"));
        priceSeries.getData().add(new XYChart.Data<>(timeStr, amount));
        updateChartAxis(amount);
        updateMetrics();
        priceChange.setText("+" + CurrencyFormatter.formatVnd(change) + " so với trước");
        bidInput.clear();
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
        LOGGER.info("Huỷ bid history và quay lại ItemList");
        try {
            stopUiAnimations();
            final Node view = FXMLLoader.load(getClass().getResource("/client/fxml/ItemList.fxml"));
            final Node contentAreaNode = bidInput.getScene().lookup("#contentArea");
            if (contentAreaNode instanceof StackPane contentArea) {
                contentArea.getChildren().setAll(view);
                return;
            }
            LOGGER.warning("Không tìm thấy #contentArea để chuyển về ItemList.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void adjustInput(final long delta) {
        final String raw = bidInput.getText().replaceAll("[^0-9]", "");
        final long base = raw.isEmpty() ? lastPrice : Long.parseLong(raw);
        bidInput.setText(String.valueOf(base + delta));
    }

    private void stopUiAnimations() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        if (liveDotTransition != null) {
            liveDotTransition.stop();
        }
    }

    private void registerLifecycleCleanup() {
        bidInput.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                stopUiAnimations();
            }
        });
    }

    private void setupNetworkHandlers() {
        // TODO: wire socket events to update table/chart/metrics on JavaFX thread.
    }

    private void joinAuctionRoom() {
        // TODO: send join-room message with current auction id.
    }

    /**
     * Tham gia room realtime theo auctionId cụ thể.
     *
     * @param targetAuctionId mã phiên đấu giá
     */
    private void joinAuctionRoom(final String targetAuctionId) {
        this.currentAuctionId = targetAuctionId;
        // TODO: send join-room message by targetAuctionId.
        LOGGER.info("Tham gia phòng đấu giá: " + targetAuctionId);
    }

    /**
     * Cập nhật thông tin tiêu đề phiên đấu giá.
     */
    private void applyAuctionHeader() {
        if (auctionTitle != null) {
            auctionTitle.setText(currentAuctionTitle);
        }
        if (auctionId != null) {
            auctionId.setText("Phiên #" + currentAuctionId);
        }
    }
}
