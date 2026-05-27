package auction_system.client.controllers.auction.components;

import auction_system.client.models.AuctionViewModel;
import auction_system.client.network.NetworkClient;
import auction_system.client.services.AuctionService;
import auction_system.client.services.UserSessionService;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Đăng ký và xử lý các socket update realtime cho màn chi tiết đấu giá.
 */
public final class AuctionRealtimeSubscription {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuctionRealtimeSubscription.class);
    private static final int MIN_UPDATE_PRICE_PARTS = 3;
    private static final int IDX_UPDATE_AUCTION_ID = 1;
    private static final int IDX_UPDATE_AMOUNT = 2;
    private static final int IDX_UPDATE_BIDDER = 3;
    private static final int IDX_UPDATE_TIME = 4;
    private static final int IDX_UPDATE_END_TIME = 5;
    private static final int MIN_AUCTION_EXTENDED_PARTS = 3;
    private static final int IDX_EXTENDED_AUCTION_ID = 1;
    private static final int IDX_EXTENDED_END_TIME = 2;

    private final AuctionViewModel viewModel;
    private final NumberAxis numberXaxis;
    private final NumberAxis numberYaxis;
    private final XYChart.Series<Number, Number> priceSeries;
    private final Consumer<String> endTimeUpdateHandler;
    private final Consumer<String> updatePriceHandler = this::handleRealtimePriceUpdate;
    private final Consumer<String> auctionExtendedHandler = this::handleAuctionExtended;
    private String activeAuctionId;

    /**
     * Khởi tạo subscription realtime cho phiên đấu giá.
     *
     * @param viewModel ViewModel cần cập nhật khi có bid mới
     * @param numberXaxis trục X của chart giá
     * @param numberYaxis trục Y của chart giá
     * @param priceSeries series dữ liệu chart giá
     * @param endTimeUpdateHandler callback nhận endTime mới từ server
     */
    public AuctionRealtimeSubscription(
            final AuctionViewModel viewModel,
            final NumberAxis numberXaxis,
            final NumberAxis numberYaxis,
            final XYChart.Series<Number, Number> priceSeries,
            final Consumer<String> endTimeUpdateHandler) {

        this.viewModel = viewModel;
        this.numberXaxis = numberXaxis;
        this.numberYaxis = numberYaxis;
        this.priceSeries = priceSeries;
        this.endTimeUpdateHandler = endTimeUpdateHandler;
    }

    /**
     * Bắt đầu theo dõi realtime cho một phiên đấu giá.
     *
     * @param auctionId mã phiên đấu giá cần theo dõi
     */
    public void start(final String auctionId) {
        if (activeAuctionId != null) {
            // Khi đổi phiên trên cùng controller, dừng subscription cũ trước khi join phiên mới.
            stop();
        }

        activeAuctionId = auctionId;
        // Đăng ký handler trước khi join room để không bỏ lỡ broadcast ngay sau JOIN_AUCTION.
        registerHandlers();
        AuctionService.getInstance().joinAuction(activeAuctionId);
    }

    /**
     * Dừng theo dõi realtime và rời room socket của phiên hiện tại.
     */
    public void stop() {
        if (activeAuctionId == null) {
            return;
        }

        unregisterHandlers();
        AuctionService.getInstance().leaveAuction(activeAuctionId);
        activeAuctionId = null;
    }

    private void registerHandlers() {
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.UPDATE_PRICE.name(),
                updatePriceHandler);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.AUCTION_EXTENDED.name(),
                auctionExtendedHandler);
    }

    private void unregisterHandlers() {
        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.UPDATE_PRICE.name(),
                updatePriceHandler);
        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.AUCTION_EXTENDED.name(),
                auctionExtendedHandler);
    }

    private void handleRealtimePriceUpdate(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
        if (parts.length < MIN_UPDATE_PRICE_PARTS
                || activeAuctionId == null
                || !activeAuctionId.equals(parts[IDX_UPDATE_AUCTION_ID])) {
            // Bỏ qua response sai format hoặc không thuộc phiên đang mở.
            return;
        }

        try {
            // UPDATE_PRICE chứa bidder, thời gian bid và endTime hiện tại của phiên.
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

            // ViewModel thêm dòng bid mới, cập nhật giá hiện tại, thống kê và dữ liệu chart.
            viewModel.processRealtimeBid(amount, bidderName, bidTime, isCurrentUser);
            if (parts.length > IDX_UPDATE_END_TIME) {
                // Nếu bid phút chót vừa gia hạn phiên, endTime đi kèm chính là mốc mới.
                endTimeUpdateHandler.accept(parts[IDX_UPDATE_END_TIME]);
            }

            // Sau khi thêm điểm mới, scale lại trục Y để chart không bị cắt đỉnh giá.
            AuctionPriceChartConfigurer.updateAxes(
                    numberXaxis,
                    numberYaxis,
                    viewModel.getOpeningPriceValue(),
                    priceSeries);
        } catch (NumberFormatException exception) {
            LOGGER.warn("Không thể đọc giá realtime: {}", response);
        }
    }

    private void handleAuctionExtended(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
        if (parts.length < MIN_AUCTION_EXTENDED_PARTS
                || activeAuctionId == null
                || !activeAuctionId.equals(parts[IDX_EXTENDED_AUCTION_ID])) {
            // Chỉ đồng bộ gia hạn cho đúng phiên đang được người dùng xem.
            return;
        }

        endTimeUpdateHandler.accept(parts[IDX_EXTENDED_END_TIME]);
    }

    private String formatBidTime(final String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            // Server cũ có thể không gửi timestamp, khi đó dùng giờ client làm fallback hiển thị.
            return DateTimeFormatter.ofPattern("HH:mm:ss").format(java.time.LocalTime.now());
        }

        try {
            // Chuẩn hóa ISO timestamp từ server thành giờ/phút/giây cho bảng và chart.
            return LocalDateTime.parse(rawTime).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        } catch (RuntimeException exception) {
            return rawTime;
        }
    }
}
