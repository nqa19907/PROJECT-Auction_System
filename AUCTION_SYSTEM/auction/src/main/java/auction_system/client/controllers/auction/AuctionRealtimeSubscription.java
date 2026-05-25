package auction_system.client.controllers.auction;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.Protocol;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quản lý subscription realtime của màn hình chi tiết phiên đấu giá.
 *
 * <p>NetworkClient là singleton dùng chung toàn app, nên object này giữ các
 * handler ổn định để đăng ký/gỡ đăng ký đúng instance khi màn hình bị đóng.
 */
final class AuctionRealtimeSubscription {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuctionRealtimeSubscription.class);
    private static final int MIN_UPDATE_PRICE_PARTS = 3;
    private static final int IDX_AUCTION_ID = 1;

    private final Supplier<String> currentAuctionIdSupplier;
    private final Consumer<String> currentAuctionUpdatedHandler;
    private final Consumer<String> watchOkHandler = this::handleWatchAuctionSuccess;
    private final Consumer<String> watchFailHandler = this::handleWatchAuctionFailure;
    private final Consumer<String> unwatchOkHandler = this::handleUnwatchAuctionSuccess;
    private final Consumer<String> updatePriceHandler = this::handleRealtimePriceUpdate;

    private boolean handlersRegistered = false;
    private boolean cleanedUp = false;

    AuctionRealtimeSubscription(
            final Supplier<String> currentAuctionIdSupplier,
            final Consumer<String> currentAuctionUpdatedHandler) {
        this.currentAuctionIdSupplier = currentAuctionIdSupplier;
        this.currentAuctionUpdatedHandler = currentAuctionUpdatedHandler;
    }

    void registerHandlers() {
        if (handlersRegistered) {
            return;
        }

        NetworkClient.getInstance().registerHandler(
                Protocol.Response.WATCH_OK.name(),
                watchOkHandler);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.WATCH_FAIL.name(),
                watchFailHandler);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.UNWATCH_OK.name(),
                unwatchOkHandler);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.UPDATE_PRICE.name(),
                updatePriceHandler);

        handlersRegistered = true;
    }

    void watch(final String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return;
        }

        cleanedUp = false;
        registerHandlers();
        NetworkClient.getInstance().sendCommand(
                Protocol.Command.WATCH_AUCTION.name()
                        + Protocol.SEPARATOR
                        + auctionId);

        LOGGER.info("Bắt đầu theo dõi realtime phiên: {}", auctionId);
    }

    void cleanup() {
        if (cleanedUp) {
            return;
        }

        cleanedUp = true;
        unwatch(currentAuctionIdSupplier.get());
        unregisterHandlers();
    }

    private void unwatch(final String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return;
        }

        NetworkClient.getInstance().sendCommand(
                Protocol.Command.UNWATCH_AUCTION.name()
                        + Protocol.SEPARATOR
                        + auctionId);

        LOGGER.info("Dừng theo dõi realtime phiên: {}", auctionId);
    }

    private void unregisterHandlers() {
        if (!handlersRegistered) {
            return;
        }

        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.WATCH_OK.name(),
                watchOkHandler);
        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.WATCH_FAIL.name(),
                watchFailHandler);
        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.UNWATCH_OK.name(),
                unwatchOkHandler);
        NetworkClient.getInstance().unregisterHandler(
                Protocol.Response.UPDATE_PRICE.name(),
                updatePriceHandler);

        handlersRegistered = false;
    }

    private void handleWatchAuctionSuccess(final String response) {
        LOGGER.info("Theo dõi realtime thành công: {}", response);
    }

    private void handleWatchAuctionFailure(final String response) {
        LOGGER.warn("Theo dõi realtime thất bại: {}", response);
    }

    private void handleUnwatchAuctionSuccess(final String response) {
        LOGGER.info("Dừng theo dõi realtime thành công: {}", response);
    }

    private void handleRealtimePriceUpdate(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX);

        if (parts.length < MIN_UPDATE_PRICE_PARTS) {
            LOGGER.warn("Bỏ qua UPDATE_PRICE không hợp lệ: {}", response);
            return;
        }

        final String updatedAuctionId = parts[IDX_AUCTION_ID];
        final String currentAuctionId = currentAuctionIdSupplier.get();

        if (!updatedAuctionId.equals(currentAuctionId)) {
            LOGGER.info(
                    "Bỏ qua UPDATE_PRICE của phiên khác: received={}, current={}",
                    updatedAuctionId,
                    currentAuctionId);
            return;
        }

        LOGGER.info("Nhận UPDATE_PRICE cho phiên hiện tại: {}", response);
        currentAuctionUpdatedHandler.accept(updatedAuctionId);
    }
}
