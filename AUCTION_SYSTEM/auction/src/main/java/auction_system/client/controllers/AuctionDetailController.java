package auction_system.client.controllers;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
 * Controller cho BidHistoryView.fxml.
 * Quản lý biểu đồ và bảng lịch sử đấu giá realtime.
 */
public class AuctionDetailController implements Initializable {

    /** Logger của controller. */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuctionDetailController.class);

    // ── Formatter VNĐ ────────────────────────────────────────────────────────

    /**
     * Dùng Locale vi_VN để tự động thêm dấu . phân cách nghìn (15.500.000).
     */
    private static final NumberFormat VND_FORMAT =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    /**
     * Format số tiền sang chuỗi VNĐ.
     * Ví dụ: 15500000 ra "15.500.000 VNĐ".
     *
     * @param amount số tiền cần format (đơn vị đồng).
     * @return chuỗi đã format kèm hậu tố VNĐ.
     */
    private static String formatVnd(long amount) {
        return VND_FORMAT.format(amount) + " VNĐ";
    }

    // ── fx:id fields ─────────────────────────────────────────────────────────

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

    // --- THÊM CÁC THÀNH PHẦN HIỂN THỊ CHI TIẾT SẢN PHẨM ---
    @FXML private Label itemNameLabel; // Hiển thị tên sản phẩm
    @FXML private Label sellerLabel;   // Hiển thị người bán
    @FXML private Label statusLabel;   // Hiển thị trạng thái phiên

    /** Chấm tròn nhấp nháy trạng thái live. */
    @FXML
    private Circle liveDot;

    /** Biểu đồ đường lịch sử giá. */
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

    /** Cột mức thay đổi. */
    @FXML
    private TableColumn<BidRow, Double> colChange;

    /** Cột trạng thái. */
    @FXML
    private TableColumn<BidRow, String> colStatus;

    /** Ô nhập giá. */
    @FXML
    private TextField bidInput;

    /** Dữ liệu bảng lịch sử. */
    private final ObservableList<BidRow> tableData =
            FXCollections.observableArrayList();

    /** Series dữ liệu biểu đồ. */
    private final XYChart.Series<String, Number> priceSeries =
            new XYChart.Series<>();

    /** Định dạng thời gian. */
    private final DateTimeFormatter timeFmt =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Giá trả cao nhất hiện tại. */
    private double lastPrice = 15500000;

    /** Số giây còn lại của phiên. */
    private int secondsLeft = 14 * 60 + 32;

    /** Tổng số lần trả giá. */
    private int totalBids = 6;

    /** ID của phiên đấu giá hiện tại đang xem. */
    private String auctionId;

    @Override
    public void initialize(final URL url, final ResourceBundle rb) {
        setupTable();
        setupChart();

        setupNetworkHandlers();
        startLiveDotAnimation();
        startCountdown();
    }

    /**
     * Nhận ID phiên đấu giá từ ItemList chuyển sang.
     * Mở đường kết nối lấy chi tiết và đăng ký phòng live.
     *
     * @param auctionId ID của phòng đấu giá (cụ thể vật phẩm nào).
     */
    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
        LOGGER.info("Khởi tạo phòng đấu giá cho ID: " + auctionId);
        
        // Tạm thời vẫn nạp dữ liệu giả để test giao diện. 
        // Khi nào nối Socket xong, bạn có thể xóa dòng dưới đây đi.
        loadSampleData();
    }

    /** Cấu hình các cột của TableView. */
    private void setupTable() {
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        colBidder.setCellValueFactory(new PropertyValueFactory<>("bidder"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colChange.setCellValueFactory(new PropertyValueFactory<>("change"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colPrice.setCellFactory(col -> new TableCell<BidRow, Double>() {
            @Override
            protected void updateItem(final Double value, final boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(formatVnd(value.longValue()));
                }
            }
        });

        colChange.setCellFactory(col -> new TableCell<BidRow, Double>() {
            @Override
            protected void updateItem(final Double value, final boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                if (value > 0) {
                    setText("+" + formatVnd(value.longValue()));
                    setStyle("-fx-text-fill: #2E7D52; -fx-font-weight: bold;");
                } else {
                    setText("—");
                    setStyle("-fx-text-fill: #A07040;");
                }
            }
        });

        bidTable.setItems(tableData);
    }

    /** Cấu hình LineChart. */
    private void setupChart() {
        priceSeries.setName("Giá trả");
        bidLineChart.getData().add(priceSeries);
        bidLineChart.setLegendVisible(false);
        bidLineChart.setCreateSymbols(true);
        bidLineChart.setAnimated(true);
    }

    /** Nạp dữ liệu mẫu vào biểu đồ và bảng. */
    private void loadSampleData() {
        LocalTime time = LocalTime.of(9, 0, 0);

        time = addSampleBid(time, 3500000, 0, "Nguyễn Văn A", "Hợp lệ");
        time = addSampleBid(time, 3600000, 100000, "Trần Thị B", "Hợp lệ");
        time = addSampleBid(time, 3750000, 150000, "Công ty XYZ", "Hợp lệ");
        time = addSampleBid(time, 3900000, 150000, "Lê Minh C", "Hợp lệ");
        time = addSampleBid(time, 4050000, 150000, "Phạm Đức D", "Hợp lệ");
        time = addSampleBid(time, 4200000, 150000, "Nguyễn Văn A", "Hợp lệ");
        time = addSampleBid(time, 4500000, 300000, "Trần Thị B", "Hợp lệ");
        time = addSampleBid(time, 5000000, 500000, "Công ty XYZ", "Hợp lệ");
        time = addSampleBid(time, 6200000, 1200000, "Lê Minh C", "Hợp lệ");
        time = addSampleBid(time, 8000000, 1800000, "Nguyễn Văn A", "Hợp lệ");
        time = addSampleBid(time, 11000000, 3000000, "Trần Thị B", "Hợp lệ");
        addSampleBid(time, 15500000, 4500000, "Công ty XYZ", "Dẫn đầu");

        updateMetrics();
    }

    /**
     * Thêm một dòng dữ liệu mẫu.
     *
     * @param time thời gian hiện tại
     * @param price giá trả
     * @param change mức tăng
     * @param bidder người đấu giá
     * @param status trạng thái
     * @return thời gian kế tiếp
     */
    private LocalTime addSampleBid(
            final LocalTime time,
            final double price,
            final double change,
            final String bidder,
            final String status
    ) {
        String timeStr = time.format(timeFmt);
        tableData.add(0, new BidRow(timeStr, bidder, price, change, status));
        priceSeries.getData().add(new XYChart.Data<>(timeStr, price));
        return time.plusMinutes(1).plusSeconds(30);
    }

    /** Cập nhật các nhãn metric. */
    private void updateMetrics() {
        currentPrice.setText(formatVnd((long) lastPrice));
        bidCount.setText(String.valueOf(totalBids));
        minBidHint.setText("Giá tối thiểu: " + formatVnd((long) lastPrice + 100000));
    }

    /**
     * Cập nhật giới hạn trục Y của biểu đồ nếu giá vượt mức hiện tại.
     *
     * @param price giá mới cần kiểm tra
     */
    private void updateChartAxis(final double price) {
        double padding = price * 0.1;

        if (price > numberYaxis.getUpperBound()) {
            numberYaxis.setUpperBound(price + padding);
        }
    }

    /** Khởi động animation nhấp nháy cho live dot. */
    private void startLiveDotAnimation() {
        FadeTransition fadeTransition =
                new FadeTransition(Duration.millis(900), liveDot);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.2);
        fadeTransition.setCycleCount(Animation.INDEFINITE);
        fadeTransition.setAutoReverse(true);
        fadeTransition.play();
    }

    /** Khởi động đồng hồ đếm ngược. */
    private void startCountdown() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (secondsLeft > 0) {
                secondsLeft--;
            }
            int minutes = secondsLeft / 60;
            int seconds = secondsLeft % 60;
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
            if (secondsLeft == 0) {
                timerLabel.setText("Kết thúc");
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    /** Xử lý sự kiện đặt giá. */
    @FXML
    private void placeBid() {
        String raw = bidInput.getText().replaceAll("[^0-9]", "");
        if (raw.isEmpty()) {
            return;
        }

        double amount = Double.parseDouble(raw);
        if (amount <= lastPrice) {
            Alert alert = new Alert(
                    Alert.AlertType.WARNING,
                    "Giá phải lớn hơn " + formatVnd((long) lastPrice),
                    ButtonType.OK
            );
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        double change = amount - lastPrice;
        lastPrice = amount;
        totalBids++;
        String timeStr = LocalTime.now().format(timeFmt);
        tableData.add(0, new BidRow(timeStr, "Bạn", amount, change, "Dẫn đầu"));
        priceSeries.getData().add(new XYChart.Data<>(timeStr, amount));
        updateChartAxis(amount);
        updateMetrics();
        priceChange.setText("+" + formatVnd((long) change) + " so với trước");
        bidInput.clear();
    }

    /** Thêm nhanh 100.000 VNĐ vào ô giá. */
    @FXML
    private void quickAdd100() {
        adjustInput(100000);
    }

    /** Thêm nhanh 500.000 VNĐ vào ô giá. */
    @FXML
    private void quickAdd500() {
        adjustInput(500000);
    }

    /** Thêm nhanh 1.000.000 VNĐ vào ô giá. */
    @FXML
    private void quickAdd1000() {
        adjustInput(1000000);
    }

    /** Quay lại màn hình chính. */
    @FXML
    private void goBack() {
        handleCancel();
    }

    /** Huỷ và load lại ItemList vào contentArea của Dashboard. */
    @FXML
    private void handleCancel() {
        LOGGER.info("Huỷ, quay về Dashboard");
        try {
            Node view = FXMLLoader.load(
                    getClass().getResource("/client/fxml/ItemList.fxml")
            );
            ((StackPane) bidInput.getScene().lookup("#contentArea"))
                    .getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cộng thêm delta vào giá trị đang nhập.
     *
     * @param delta số tiền cần cộng thêm
     */
    private void adjustInput(final double delta) {
        String raw = bidInput.getText().replaceAll("[^0-9]", "");
        double base = raw.isEmpty() ? lastPrice : Double.parseDouble(raw);
        bidInput.setText(String.format("%.0f", base + delta));
    }

    /**
     * Model dữ liệu cho một hàng trong bảng lịch sử đấu giá.
     */
    public static class BidRow {

        /** Thời gian đặt giá. */
        private final StringProperty time;

        /** Tên người đặt giá. */
        private final StringProperty bidder;

        /** Mức giá đặt. */
        private final DoubleProperty price;

        /** Mức thay đổi so với lần trước. */
        private final DoubleProperty change;

        /** Trạng thái của lần đặt giá. */
        private final StringProperty status;

        /**
         * Khởi tạo một hàng dữ liệu đấu giá.
         *
         * @param timeVal thời gian
         * @param bidderVal tên người đấu
         * @param priceVal giá trả
         * @param changeVal mức thay đổi
         * @param statusVal trạng thái
         */
        public BidRow(
                final String timeVal,
                final String bidderVal,
                final double priceVal,
                final double changeVal,
                final String statusVal
        ) {
            this.time = new SimpleStringProperty(timeVal);
            this.bidder = new SimpleStringProperty(bidderVal);
            this.price = new SimpleDoubleProperty(priceVal);
            this.change = new SimpleDoubleProperty(changeVal);
            this.status = new SimpleStringProperty(statusVal);
        }

        /**
         * Lấy thời gian.
         *
         * @return chuỗi thời gian
         */
        public String getTime() {
            return time.get();
        }

        /**
         * Lấy tên người đấu giá.
         *
         * @return tên người đấu
         */
        public String getBidder() {
            return bidder.get();
        }

        /**
         * Lấy mức giá.
         *
         * @return giá trả
         */
        public double getPrice() {
            return price.get();
        }

        /**
         * Lấy mức thay đổi.
         *
         * @return delta so với lần trước
         */
        public double getChange() {
            return change.get();
        }

        /**
         * Lấy trạng thái.
         *
         * @return trạng thái
         */
        public String getStatus() {
            return status.get();
        }
    }

    /**
     * Đăng ký các handler mạng để nhận dữ liệu realtime
     * từ server thông qua socket.
     *
     * <p>Các handler này dùng để xử lý những message như:
     * UPDATE_PRICE, BID_OK, BID_FAIL...
     * Khi server gửi dữ liệu, controller sẽ tự động cập nhật
     * giao diện như bảng lịch sử và biểu đồ giá.</p>
     */
    private void setupNetworkHandlers() {

    }

    /**
     * Gửi yêu cầu tham gia phòng đấu giá hiện tại tới server.
     *
     * <p>Sau khi tham gia phòng đấu giá, client sẽ nhận được
     * các cập nhật realtime liên quan đến phiên đấu giá này,
     * ví dụ như thay đổi giá hoặc trạng thái phiên.</p>
     */
    private void joinAuctionRoom() {

    }

}