package auction_system.client.controllers.auction.components;

import auction_system.client.models.AuctionViewModel;
import auction_system.client.network.NetworkClient;
import auction_system.client.services.AuctionService;
import auction_system.client.services.UserSessionService;
import auction_system.common.models.users.User;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
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
        handleRealtimePriceJsonUpdate(response);
    }

    private void handleAuctionExtended(final String response) {
        handleAuctionExtendedJson(response);
    }

    private void handleRealtimePriceJsonUpdate(final String response) {
        try {
            // Payload UPDATE_PRICE chứa dữ liệu bid mới theo tên field thay vì index string.
            final JsonMessage message = JsonProtocol.parse(response);
            final JsonNode payload = message.payload();
            if (payload == null
                    || activeAuctionId == null
                    || !activeAuctionId.equals(payload.path("auctionId").asText())) {
                return;
            }

            processRealtimePriceUpdate(
                    payload.path("currentPrice").asDouble(),
                    payload.path("bidderName").asText(""),
                    payload.path("bidTime").asText(""),
                    payload.path("endTime").asText(""),
                    response);
        } catch (IOException exception) {
            LOGGER.warn("Không thể đọc JSON UPDATE_PRICE: {}", exception.getMessage());
        }
    }

    private void handleAuctionExtendedJson(final String response) {
        try {
            // Payload AUCTION_EXTENDED chứa auctionId và endTime mới cho phiên đang mở.
            final JsonMessage message = JsonProtocol.parse(response);
            final JsonNode payload = message.payload();
            if (payload == null
                    || activeAuctionId == null
                    || !activeAuctionId.equals(payload.path("auctionId").asText())) {
                return;
            }

            final String endTime = payload.path("endTime").asText("");
            if (!endTime.isBlank()) {
                endTimeUpdateHandler.accept(endTime);
            }
        } catch (IOException exception) {
            LOGGER.warn("Không thể đọc JSON AUCTION_EXTENDED: {}", exception.getMessage());
        }
    }

    private void processRealtimePriceUpdate(
            final double rawAmount,
            final String bidderName,
            final String rawBidTime,
            final String endTime,
            final String originalResponse) {
        try {
            final long amount = (long) rawAmount;
            final String bidTime = rawBidTime.isBlank()
                    ? DateTimeFormatter.ofPattern("HH:mm:ss").format(java.time.LocalTime.now())
                    : formatBidTime(rawBidTime);
            final User currentUser = UserSessionService.getInstance().getCurrentUser();
            final boolean isCurrentUser = currentUser != null
                    && bidderName.equals(currentUser.getUsername());

            viewModel.processRealtimeBid(amount, bidderName, bidTime, isCurrentUser);
            if (!endTime.isBlank()) {
                endTimeUpdateHandler.accept(endTime);
            }

            AuctionPriceChartConfigurer.updateAxes(
                    numberXaxis,
                    numberYaxis,
                    viewModel.getOpeningPriceValue(),
                    priceSeries);
        } catch (RuntimeException exception) {
            LOGGER.warn("Không thể xử lý giá realtime: {}", originalResponse);
        }
    }

    private String formatBidTime(final String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            // Nếu server không gửi timestamp, dùng giờ client làm thời điểm hiển thị.
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
