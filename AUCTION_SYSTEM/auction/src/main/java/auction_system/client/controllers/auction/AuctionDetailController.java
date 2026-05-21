package auction_system.client.controllers.auction;

import auction_system.client.models.AuctionDisplayContext;
import auction_system.client.utils.CurrencyFormatter;
import auction_system.client.utils.Router;
import auction_system.client.utils.ViewConstants;
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
 * Controller cho màn hình chi tiết phiên đấu giá.
 * Quản lý bảng lịch sử, biểu đồ giá, đặt giá và trạng thái realtime.
 */
public class AuctionDetailController implements Initializable {

    /** Logger của controller. */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuctionDetailController.class);

    // ── fx:id fields ─────────────────────────────────────────

    /** Nhãn giá hiện tại. */
    @FXML
    private Label currentPrice;

    /** Nhãn mức thay đổi giá. */
    @FXML
    private Label priceChange;

    /** Nhãn giá khởi điểm. */
    @FXML
    private Label startPrice;

    /** Nhãn số lần trả giá. */
    @FXML
    private Label bidCount;

    /** Nhãn số người tham gia. */
    @FXML
    private Label participantCount;

    /** Nhãn đồng hồ đếm ngược. */
    @FXML
    private Label timerLabel;

    /** Nhãn gợi ý giá tối thiểu. */
    @FXML
    private Label minBidHint;

    /** Tiêu đề phiên đấu giá. */
    @FXML
    private Label auctionTitle;

    /** Mã phiên đấu giá. */
    @FXML
    private Label auctionId;

    /** Chấm trạng thái live realtime. */
    @FXML
    private Circle liveDot;

    /** Biểu đồ lịch sử giá đấu. */
    @FXML
    private LineChart<String, Number> bidLineChart;

    /** Trục X của biểu đồ. */
    @FXML
    private CategoryAxis categoryXaxis;

    /** Trục Y của biểu đồ. */
    @FXML
    private NumberAxis numberYaxis;

    /** Bảng lịch sử đấu giá. */
    @FXML
    private TableView<BidRow> bidTable;

    /** Cột thời gian. */
    @FXML
    private TableColumn<BidRow, String> colTime;

    /** Cột người đấu giá. */
    @FXML
    private TableColumn<BidRow, String> colBidder;

    /** Cột giá trả. */
    @FXML
    private TableColumn<BidRow, Double> colPrice;

    /** Cột chênh lệch giá. */
    @FXML
    private TableColumn<BidRow, Double> colChange;

    /** Cột trạng thái bid. */
    @FXML
    private TableColumn<BidRow, String> colStatus;

    /** Ô nhập giá đấu. */
    @FXML
    private TextField bidInput;

    // ── Data structures ──────────────────────────────────────

    /** Dữ liệu bảng lịch sử đấu giá. */
    private final ObservableList<BidRow> tableData =
            FXCollections.observableArrayList();

    /** Series dữ liệu cho biểu đồ giá. */
    private final XYChart.Series<String, Number> priceSeries =
            new XYChart.Series<>();

    /** Formatter hiển thị thời gian. */
    private final DateTimeFormatter timeFmt =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Danh sách người đã tham gia đấu giá. */
    private final Set<String> bidders = new HashSet<>();

    // ── Auction state ────────────────────────────────────────

    /** Giá hiện tại cao nhất của phiên đấu giá. */
    private long lastPrice = 15_500_000L;

    /** Giá khởi điểm của phiên đấu giá. */
    private long openingPrice = 3_500_000L;

    /** Số giây còn lại của phiên đấu giá. */
    private int secondsLeft = 14 * 60 + 32;

    /** Tổng số lần trả giá. */
    private int totalBids = 0;

    /** Tổng số người tham gia đấu giá. */
    private int totalParticipants = 0;

    /** ID phiên đấu giá hiện tại. */
    private String currentAuctionId = "AUC-2024-0518";

    /** Tên phiên đấu giá hiện tại. */
    private String currentAuctionTitle =
            "Đồng hồ Rolex Submariner Date";

    // ── Animation / Timeline ────────────────────────────────

    /** Timeline đồng hồ đếm ngược realtime. */
    private Timeline countdownTimeline;

    /** Animation nhấp nháy cho live dot. */
    private FadeTransition liveDotTransition;

    /**
     * Khởi tạo màn hình theo auctionId được truyền từ ItemList.
     *
     * @param auctionId mã phiên đấu giá
     */
    public void initAuction(final String auctionId) {
        this.currentAuctionId = auctionId;
        applyAuctionHeader();
        loadSampleData();
        joinAuctionRoom(auctionId);
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

    /**
     * Hàm tương thích cho màn hình gọi theo kiểu setAuctionId().
     *
     * @param auctionId mã phiên đấu giá
     */
    public void setAuctionId(final String auctionId) {
        initAuction(auctionId);
    }

    /**
     * Khởi tạo các thành phần giao diện và animation realtime.
     *
     * @param url URL khởi tạo từ JavaFX
     * @param rb ResourceBundle từ JavaFX
     */
    @Override
    public void initialize(final URL url, final ResourceBundle rb) {
        setupTable();
        setupChart();
        setupNetworkHandlers();
        startLiveDotAnimation();
        startCountdown();
        registerLifecycleCleanup();
    }

    /**
     * Cấu hình các cột của bảng lịch sử đấu giá.
     */
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

    /**
     * Cấu hình biểu đồ lịch sử giá.
     */
    private void setupChart() {
        priceSeries.setName("Giá trả");
        bidLineChart.getData().add(priceSeries);
        bidLineChart.setLegendVisible(false);
        bidLineChart.setCreateSymbols(true);
        bidLineChart.setAnimated(true);
    }

    /**
     * Nạp dữ liệu mẫu để test giao diện bảng và biểu đồ.
     */
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
        final long p8 = lastPrice;

        time = addSampleBid(time, openingPrice, 0L, "Nguyễn Đức Mạnh", "Hợp lệ");
        time = addSampleBid(time, p2, p2 - openingPrice, "Bùi Nguyễn Phương", "Hợp lệ");
        time = addSampleBid(time, p3, p3 - p2, "Công ty XYZ", "Hợp lệ");
        time = addSampleBid(time, p4, p4 - p3, "Nguyễn Quốc Anh", "Hợp lệ");
        time = addSampleBid(time, p5, p5 - p4, "Hoàng Nguyễn", "Hợp lệ");
        time = addSampleBid(time, p6, p6 - p5, "Nguyễn Văn A", "Hợp lệ");
        time = addSampleBid(time, p7, p7 - p6, "Trần Thị B", "Hợp lệ");
        addSampleBid(time, p8, p8 - p7, "Công ty XYZ", "Dẫn đầu");

        lastPrice = p8;

        applyAuctionHeader();
        updateMetrics();
        updateChartAxis(lastPrice);
    }

    /**
     * Thêm một dòng dữ liệu đấu giá vào bảng và biểu đồ.
     *
     * @param time thời gian trả giá
     * @param price giá trả
     * @param change mức chênh lệch
     * @param bidder người đấu giá
     * @param status trạng thái lượt đấu giá
     * @return thời gian kế tiếp
     */
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

    /**
     * Cập nhật các metric hiển thị trên giao diện.
     */
    private void updateMetrics() {
        currentPrice.setText(CurrencyFormatter.formatVnd(lastPrice));
        startPrice.setText(CurrencyFormatter.formatVnd(openingPrice));
        bidCount.setText(String.valueOf(totalBids));
        participantCount.setText(String.valueOf(totalParticipants));
        minBidHint.setText(
                "Giá tối thiểu: "
                        + CurrencyFormatter.formatVnd(lastPrice + 100_000L)
        );
    }

    /**
     * Cập nhật giới hạn trục Y của biểu đồ.
     *
     * @param price giá mới cần hiển thị
     */
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

    /**
     * Khởi động hiệu ứng nhấp nháy cho live dot.
     */
    private void startLiveDotAnimation() {
        liveDotTransition = new FadeTransition(Duration.millis(900), liveDot);
        liveDotTransition.setFromValue(1.0);
        liveDotTransition.setToValue(0.2);
        liveDotTransition.setCycleCount(Animation.INDEFINITE);
        liveDotTransition.setAutoReverse(true);
        liveDotTransition.play();
    }

    /**
     * Khởi động đồng hồ đếm ngược realtime.
     */
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

    /**
     * Xử lý sự kiện người dùng đặt giá mới.
     */
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

        priceChange.setText(
                "+" + CurrencyFormatter.formatVnd(change) + " so với trước"
        );

        bidInput.clear();
    }

    /**
     * Cộng nhanh 100.000 VNĐ vào ô nhập giá.
     */
    @FXML
    private void quickAdd100() {
        adjustInput(100_000L);
    }

    /**
     * Cộng nhanh 500.000 VNĐ vào ô nhập giá.
     */
    @FXML
    private void quickAdd500() {
        adjustInput(500_000L);
    }

    /**
     * Cộng nhanh 1.000.000 VNĐ vào ô nhập giá.
     */
    @FXML
    private void quickAdd1000() {
        adjustInput(1_000_000L);
    }

    /**
     * Quay lại màn hình danh sách sản phẩm.
     */
    @FXML
    private void goBack() {
        handleCancel();
    }

    /**
     * Huỷ màn hình hiện tại và quay về ItemList.
     */
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
        final long base = raw.isEmpty() ? lastPrice : Long.parseLong(raw);

        bidInput.setText(String.valueOf(base + delta));
    }

    /**
     * Dừng toàn bộ animation/timeline đang chạy.
     */
    private void stopUiAnimations() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        if (liveDotTransition != null) {
            liveDotTransition.stop();
        }
    }

    /**
     * Cleanup animation khi scene bị huỷ khỏi JavaFX.
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

    /**
     * Gửi yêu cầu tham gia room realtime mặc định.
     */
    private void joinAuctionRoom() {
        // TODO: gửi lệnh join-room với auction id hiện tại.
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
     * Cập nhật tiêu đề và mã phiên đấu giá lên giao diện.
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