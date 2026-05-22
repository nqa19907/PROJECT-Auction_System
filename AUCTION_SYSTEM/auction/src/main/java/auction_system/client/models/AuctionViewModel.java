package auction_system.client.models;

import auction_system.client.services.AuctionService;
import auction_system.client.utils.CurrencyFormatter;
import auction_system.common.models.auctions.BidRow;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
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
    private final ObservableList<XYChart.Data<String, Number>> chartData =
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
        chartData.add(new XYChart.Data<>(timeStr, context.currentPrice()));
    }

    /**
     * Xử lý khi có một lượt đặt giá mới (từ người dùng hoặc từ mạng).
     *
     * @param amount Số tiền đặt.
     * @param bidderName Tên người đặt.
     * @param isCurrentUser Liệu có phải người dùng hiện tại đặt không.
     */
    public void processNewBid(long amount, String bidderName, boolean isCurrentUser) {
        if (amount <= currentPrice.get()) {
            // Logic xử lý bid không hợp lệ có thể thêm ở đây
            return;
        }

        final long change = amount - currentPrice.get();
        currentPrice.set(amount);

        bidCount.set(bidCount.get() + 1);
        bidders.add(bidderName);
        participantCount.set(bidders.size());

        String timeStr = LocalTime.now().format(timeFmt);
        String status = isCurrentUser ? "Dẫn đầu" : "Hợp lệ";

        bidHistory.add(0, new BidRow(timeStr, bidderName, amount, change, status));
        chartData.add(new XYChart.Data<>(timeStr, amount));

        priceChangeText.set("+" + CurrencyFormatter.formatVnd(change) + " so với trước");
    }

    // ── Getter cho các thuộc tính ───────────────────────────

    /**
     * Callback trả kết quả gửi yêu cầu đặt giá.
     */
    @FunctionalInterface
    public interface BidSubmissionCallback {
        void onComplete(boolean success, String message);
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
                callback.onComplete(false, "Vui lòng nhập số tiền.");
                return;
            }
            amount = Long.parseLong(sanitizedAmount);
        } catch (NumberFormatException e) {
            callback.onComplete(false, "Số tiền không hợp lệ.");
            return;
        }

        if (amount <= currentPrice.get()) {
            callback.onComplete(
                false,
                "Giá phải lớn hơn " + CurrencyFormatter.formatVnd(currentPrice.get())
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

    public ObservableList<XYChart.Data<String, Number>> getChartData() {
        return chartData;
    }

    public long getCurrentPriceValue() {
        return currentPrice.get();
    }

    public long getOpeningPriceValue() {
        return openingPrice.get();
    }

}
