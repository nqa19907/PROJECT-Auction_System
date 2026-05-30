package auction_system.client.models;

import auction_system.client.services.AuctionService;
import auction_system.client.utils.CurrencyFormatter;
import auction_system.common.models.auctions.BidRow;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

/**
 * ViewModel cho màn hình AuctionDetail.
 * Chứa toàn bộ trạng thái và logic của view, giúp Controller "mỏng" hơn.
 */
public class AuctionViewModel {

    private static final int IDX_BID_TIME = 0;
    private static final int IDX_PARTICIPANT_NAME = 1;
    private static final int IDX_BID_AMOUNT = 2;
    private static final int MIN_BID_HISTORY_PARTS = 3;
    private static final String VALID_BID_STATUS = "Hợp lệ";

    // ── Thuộc tính dùng cho data binding ────────────────────

    private final StringProperty auctionId = new SimpleStringProperty("AUC-2024-0000");
    private final StringProperty auctionTitle = new SimpleStringProperty("Loading...");
    private final LongProperty currentPrice = new SimpleLongProperty(0);
    private final StringProperty currentPriceFormatted = new SimpleStringProperty("0 VNĐ");
    private final LongProperty openingPrice = new SimpleLongProperty(0);
    private final StringProperty openingPriceFormatted = new SimpleStringProperty("0 VNĐ");
    private final IntegerProperty bidCount = new SimpleIntegerProperty(0);
    private final IntegerProperty participantCount = new SimpleIntegerProperty(0);
    private final StringProperty priceChangeText = new SimpleStringProperty("+0 VNĐ so với trước");

    private final ObservableList<BidRow> bidHistory = FXCollections.observableArrayList();
    private final ObservableList<XYChart.Data<Number, Number>> chartData =
        FXCollections.observableArrayList();

    // ── Trạng thái nội bộ ───────────────────────────────────

    // Dùng Set để mỗi bidder chỉ được tính một lần trong participantCount.
    private final Set<String> bidders = new HashSet<>();
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Khởi tạo ViewModel và thiết lập các listener cho việc định dạng dữ liệu.
     */
    public AuctionViewModel() {
        // Tự động cập nhật chuỗi giá tiền đã format mỗi khi giá trị số thay đổi
        currentPrice.addListener((obs, oldVal, newVal) ->
            currentPriceFormatted.set(CurrencyFormatter.formatVnd(newVal.longValue()))
        );
        openingPrice.addListener((obs, oldVal, newVal) ->
            openingPriceFormatted.set(CurrencyFormatter.formatVnd(newVal.longValue()))
        );
    }

    /**
     * Khởi tạo dữ liệu cho ViewModel từ context.
     *
     * @param context Dữ liệu ban đầu của phiên đấu giá.
     */
    public void init(AuctionDisplayContext context) {
        if (context == null) {
            return;
        }

        this.auctionId.set(context.auctionId());
        this.auctionTitle.set(context.itemTitle());
        this.openingPrice.set(context.openingPrice());
        this.currentPrice.set(context.currentPrice());

        // Điểm đầu tiên neo biểu đồ theo giá hiện tại của phiên khi mở màn chi tiết.
        String timeStr = LocalTime.now().format(timeFmt);
        addChartPoint(timeStr, context.currentPrice());
    }

    /**
     * Xử lý khi có một lượt đặt giá mới (từ người dùng hoặc từ mạng).
     *
     * @param amount Số tiền đặt.
     * @param bidderName Tên người đặt.
     * @param isCurrentUser Liệu có phải người dùng hiện tại đặt không.
     */
    public void processNewBid(long amount, String bidderName, boolean isCurrentUser) {
        processRealtimeBid(amount, bidderName, LocalTime.now().format(timeFmt), isCurrentUser);
    }

    /**
     * Xử lý bid realtime do server đẩy về kèm thời gian phát sinh thật.
     *
     * @param amount số tiền đặt giá mới
     * @param bidderName tên người đặt giá
     * @param timeStr thời gian đặt giá đã format hoặc lấy từ server
     * @param isCurrentUser true nếu bid thuộc người dùng hiện tại
     */
    public void processRealtimeBid(
            long amount,
            String bidderName,
            String timeStr,
            boolean isCurrentUser) {
        if (amount <= currentPrice.get()) {
            // Logic xử lý bid không hợp lệ có thể thêm ở đây
            return;
        }

        final long change = amount - currentPrice.get();
        currentPrice.set(amount);

        bidCount.set(bidCount.get() + 1);
        bidders.add(bidderName);
        participantCount.set(bidders.size());

        String status = isCurrentUser ? "Dẫn đầu" : "Hợp lệ";

        bidHistory.add(0, new BidRow(timeStr, bidderName, amount, change, status));
        addChartPoint(timeStr, amount);

        priceChangeText.set("+" + CurrencyFormatter.formatVnd(change) + " so với trước");
    }

    /**
     * Nạp lịch sử đặt giá đã lấy từ server vào bảng và biểu đồ.
     *
     * <p>Dữ liệu đầu vào dùng format đã parse từ BID_HISTORY:
     * {@code time}, {@code bidder}, {@code amount}. Server trả theo thứ tự thời gian tăng dần,
     * còn bảng UI hiển thị lượt mới nhất ở đầu danh sách.
     *
     * @param rows danh sách dòng lịch sử bid đã parse từ response socket
     */
    public void loadBidHistory(final List<String[]> rows) {
        // Reset dữ liệu runtime trước khi dựng lại từ lịch sử server.
        bidHistory.clear();
        chartData.clear();
        bidders.clear();

        // previousPrice dùng để tính mức tăng của từng bid so với mốc liền trước.
        long previousPrice = openingPrice.get();
        long latestPrice = currentPrice.get();
        long latestChange = 0L;
        int parsedBidCount = 0;

        if (rows != null) {
            for (String[] row : rows) {
                // Bỏ qua record lỗi để một dòng hỏng không làm mất toàn bộ lịch sử.
                if (row == null || row.length < MIN_BID_HISTORY_PARTS) {
                    continue;
                }

                final long amount = parseBidAmount(row[IDX_BID_AMOUNT]);
                if (amount <= 0) {
                    continue;
                }

                final String time = row[IDX_BID_TIME];
                final String bidder = row[IDX_PARTICIPANT_NAME];
                final long change = amount - previousPrice;

                // Cập nhật các biến lưu trữ thông tin mới nhất
                previousPrice = amount;
                latestPrice = amount;
                latestChange = change;
                parsedBidCount++;

                // Chart giữ thứ tự thời gian tăng dần, bảng thêm mới nhất lên đầu.
                bidders.add(bidder);
                addChartPoint(time, amount);
                bidHistory.add(0, new BidRow(time, bidder, amount, change, VALID_BID_STATUS));
            }
        }

        // Đồng bộ lại các property mà UI đang bind sau khi load lịch sử.
        currentPrice.set(latestPrice);
        bidCount.set(parsedBidCount);
        participantCount.set(bidders.size());
        updateHistoryFallbackChartPoint(parsedBidCount);
        updatePriceChangeText(latestChange);
    }

    /**
     * Chuyển chuỗi amount từ protocol thành số nguyên dùng cho UI.
     *
     * @param rawAmount chuỗi giá đặt từ server
     * @return số tiền đã parse, hoặc 0 nếu dữ liệu không hợp lệ
     */
    private long parseBidAmount(final String rawAmount) {
        try {
            return (long) Double.parseDouble(rawAmount);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Thêm điểm neo cho chart khi phiên chưa có lịch sử bid hợp lệ.
     *
     * @param parsedBidCount số bid hợp lệ đã nạp được
     */
    private void updateHistoryFallbackChartPoint(final int parsedBidCount) {
        if (parsedBidCount > 0) {
            return;
        }

        addChartPoint(LocalTime.now().format(timeFmt), currentPrice.get());
    }

    /**
     * Thêm một điểm lên chart bằng số thứ tự bid làm trục X.
     *
     * <p>Không dùng trực tiếp chuỗi thời gian làm X vì {@code CategoryAxis}
     * sẽ gộp các điểm có cùng timestamp, làm mất điểm khi nhiều bid cùng giây.
     *
     * @param time nhãn thời gian hiển thị trên trục X
     * @param amount giá tại thời điểm đó
     */
    private void addChartPoint(final String time, final long amount) {
        final XYChart.Data<Number, Number> point =
                new XYChart.Data<>(chartData.size() + 1, amount);
        point.setExtraValue(time);
        chartData.add(point);
    }

    /**
     * Cập nhật dòng mô tả mức tăng giá mới nhất.
     *
     * @param latestChange mức thay đổi của bid mới nhất so với bid trước đó
     */
    private void updatePriceChangeText(final long latestChange) {
        priceChangeText.set(
                "+" + CurrencyFormatter.formatVnd(latestChange) + " so với trước"
        );
    }



    // ── Getter cho các thuộc tính ───────────────────────────

    /**
     * Callback trả kết quả gửi yêu cầu đặt giá.
     */
    @FunctionalInterface
    public interface BidSubmissionCallback {
        void onComplete(boolean success, String message, double newBalance);
    }

    /**
     * Kiểm tra dữ liệu nhập và gửi yêu cầu đặt giá tới AuctionService.
     * Phương thức này gom logic đặt giá để Controller chỉ điều phối giao diện.
     *
     * @param rawAmount chuỗi số tiền lấy trực tiếp từ ô nhập
     * @param callback callback được gọi khi xử lý xong yêu cầu đặt giá
     */
    public void submitBid(String rawAmount, BidSubmissionCallback callback) {
        long amount;
        try {
            // Cho phép nhập kèm dấu phân cách/đơn vị, chỉ giữ lại chữ số để gửi server.
            String sanitizedAmount = rawAmount.replaceAll("[^0-9]", "");
            if (sanitizedAmount.isEmpty()) {
                callback.onComplete(false, "Vui lòng nhập số tiền.", 0);
                return;
            }
            amount = Long.parseLong(sanitizedAmount);
        } catch (NumberFormatException e) {
            callback.onComplete(false, "Số tiền không hợp lệ.", 0);
            return;
        }

        if (amount <= currentPrice.get()) {
            callback.onComplete(
                false,
                "Giá phải lớn hơn " + CurrencyFormatter.formatVnd(currentPrice.get()),
                0
            );
            return;
        }

        // Gọi service với auction ID hiện tại và chuyển kết quả về callback.
        AuctionService.getInstance().placeBid(auctionId.get(), amount, callback::onComplete);
    }

    public StringProperty auctionIdProperty() {
        return auctionId;
    }

    public String getAuctionTitle() {
        return auctionTitle.get();
    }

    public StringProperty auctionTitleProperty() {
        return auctionTitle;
    }

    public StringProperty currentPriceFormattedProperty() {
        return currentPriceFormatted;
    }

    public StringProperty openingPriceFormattedProperty() {
        return openingPriceFormatted;
    }

    public IntegerProperty bidCountProperty() {
        return bidCount;
    }

    public IntegerProperty participantCountProperty() {
        return participantCount;
    }

    public StringProperty priceChangeTextProperty() {
        return priceChangeText;
    }

    public ObservableList<BidRow> getBidHistory() {
        return bidHistory;
    }

    public ObservableList<XYChart.Data<Number, Number>> getChartData() {
        return chartData;
    }

    public long getCurrentPriceValue() {
        return currentPrice.get();
    }

    public long getOpeningPriceValue() {
        return openingPrice.get();
    }

}
