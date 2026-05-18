package auction_system.client.controllers;

import java.io.IOException;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.logging.Logger;
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

/**
 * Controller cho BidHistoryView.fxml.
 * Quản lý biểu đồ và bảng lịch sử đấu giá realtime.
 */
public class BidHistoryController implements Initializable {

    /** Logger của controller. */
    private static final Logger LOGGER =
            Logger.getLogger(BidHistoryController.class.getName());

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
    private double lastPrice = 15500;

    /** Số giây còn lại của phiên. */
    private int secondsLeft = 14 * 60 + 32;

    /** Tổng số lần trả giá. */
    private int totalBids = 6;

    @Override
    public void initialize(final URL url, final ResourceBundle rb) {
        setupTable();
        setupChart();
        loadSampleData();
        startLiveDotAnimation();
        startCountdown();
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
                    setText(String.format("€%,.0f", value));
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
                    setText(String.format("+€%.0f", value));
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

        time = addSampleBid(time, 3500, 0, "Nguyễn Văn A", "Hợp lệ");
        time = addSampleBid(time, 3600, 100, "Trần Thị B", "Hợp lệ");
        time = addSampleBid(time, 3750, 150, "Công ty XYZ", "Hợp lệ");
        time = addSampleBid(time, 3900, 150, "Lê Minh C", "Hợp lệ");
        time = addSampleBid(time, 4050, 150, "Phạm Đức D", "Hợp lệ");
        time = addSampleBid(time, 4200, 150, "Nguyễn Văn A", "Hợp lệ");
        time = addSampleBid(time, 4500, 300, "Trần Thị B", "Hợp lệ");
        time = addSampleBid(time, 5000, 500, "Công ty XYZ", "Hợp lệ");
        time = addSampleBid(time, 6200, 1200, "Lê Minh C", "Hợp lệ");
        time = addSampleBid(time, 8000, 1800, "Nguyễn Văn A", "Hợp lệ");
        time = addSampleBid(time, 11000, 3000, "Trần Thị B", "Hợp lệ");
        addSampleBid(time, 15500, 4500, "Công ty XYZ", "Dẫn đầu");

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
        currentPrice.setText(String.format("€%,.0f", lastPrice));
        bidCount.setText(String.valueOf(totalBids));
        minBidHint.setText(String.format("Giá tối thiểu: €%,.0f", lastPrice + 100));
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
        String raw = bidInput.getText().replaceAll("[^0-9.]", "");
        if (raw.isEmpty()) {
            return;
        }

        double amount = Double.parseDouble(raw);
        if (amount <= lastPrice) {
            Alert alert = new Alert(
                    Alert.AlertType.WARNING,
                    "Giá phải lớn hơn " + String.format("€%,.0f", lastPrice),
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
        updateMetrics();
        priceChange.setText(String.format("+€%.0f so với trước", change));
        bidInput.clear();
    }

    /** Thêm nhanh 100 vào ô giá. */
    @FXML
    private void quickAdd100() {
        adjustInput(100);
    }

    /** Thêm nhanh 500 vào ô giá. */
    @FXML
    private void quickAdd500() {
        adjustInput(500);
    }

    /** Thêm nhanh 1000 vào ô giá. */
    @FXML
    private void quickAdd1000() {
        adjustInput(1000);
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
        String raw = bidInput.getText().replaceAll("[^0-9.]", "");
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
}
