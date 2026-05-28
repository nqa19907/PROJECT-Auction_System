package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.Protocol;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Service phia Client dung de gui yeu cau dang ban/cap nhat phien len Server.
 */
public final class ItemPublishService {
    private static final Logger LOGGER = Logger.getLogger(ItemPublishService.class.getName());
    private static final ItemPublishService INSTANCE = new ItemPublishService();
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private PublishItemCallback publishCallback;
    private PublishItemCallback updateCallback;

    private ItemPublishService() {
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.PUBLISH_ITEM_OK.name(),
                this::handlePublishSuccess);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.PUBLISH_ITEM_FAIL.name(),
                this::handlePublishFailure);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.UPDATE_MY_AUCTION_OK.name(),
                this::handleUpdateSuccess);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.UPDATE_MY_AUCTION_FAIL.name(),
                this::handleUpdateFailure);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ERROR.name(),
                this::handleError);
    }

    /**
     * Lay singleton cua service.
     *
     * @return instance dung chung
     */
    public static ItemPublishService getInstance() {
        return INSTANCE;
    }

    /**
     * Callback nhan ket qua tu Server.
     */
    @FunctionalInterface
    public interface PublishItemCallback {
        /**
         * Tra ket qua cho controller goi service.
         *
         * @param success trang thai thanh cong
         * @param message thong bao tra ve
         */
        void onResult(boolean success, String message);
    }

    /**
     * Gui yeu cau dang ban phien moi.
     *
     * @param category danh muc san pham
     * @param itemName ten san pham
     * @param description mo ta san pham
     * @param condition tinh trang san pham
     * @param startPrice gia khoi diem
     * @param startTime thoi gian bat dau
     * @param endTime thoi gian ket thuc
     * @param callback callback nhan ket qua
     */
    public void publishItem(
            final String category,
            final String itemName,
            final String description,
            final String condition,
            final double startPrice,
            final LocalDateTime startTime,
            final LocalDateTime endTime,
            final PublishItemCallback callback) {
        Objects.requireNonNull(callback, "Callback khong duoc null.");
        this.publishCallback = callback;

        final String request = String.join(
                Protocol.SEPARATOR,
                Protocol.Command.PUBLISH_ITEM.name(),
                clean(category),
                clean(itemName),
                clean(description),
                clean(condition),
                String.valueOf(startPrice),
                FORMATTER.format(startTime),
                FORMATTER.format(endTime));

        final boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent) {
            LOGGER.warning("Khong the gui yeu cau dang ban san pham toi Server.");
            callback.onResult(false, "Khong the ket noi toi Server.");
        }
    }

    /**
     * Cap nhat phien cua user hien tai.
     * Khong gui gia khoi diem; cho phep gui thoi gian ket thuc moi.
     *
     * @param auctionId ma phien can cap nhat
     * @param category danh muc moi
     * @param itemName ten tai san moi
     * @param description mo ta moi
     * @param condition tinh trang moi
     * @param endTime thoi gian ket thuc moi
     * @param callback callback nhan ket qua
     */
    public void updateMyAuction(
            final String auctionId,
            final String category,
            final String itemName,
            final String description,
            final String condition,
            final LocalDateTime endTime,
            final PublishItemCallback callback) {
        Objects.requireNonNull(callback, "Callback khong duoc null.");
        this.updateCallback = callback;

        // Format request update: ...|condition|endTime(ISO-8601)
        final String request = String.join(
                Protocol.SEPARATOR,
                Protocol.Command.UPDATE_MY_AUCTION.name(),
                clean(auctionId),
                clean(category),
                clean(itemName),
                clean(description),
                clean(condition),
                FORMATTER.format(endTime));

        final boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent) {
            LOGGER.warning("Khong the gui yeu cau cap nhat phien toi Server.");
            callback.onResult(false, "Khong the ket noi toi Server.");
        }
    }

    private void handlePublishSuccess(final String response) {
        notifyPublishCallback(
                true,
                extractMessage(response, "Dang ban san pham thanh cong."));
    }

    private void handlePublishFailure(final String response) {
        notifyPublishCallback(
                false,
                extractMessage(response, "Dang ban san pham that bai."));
    }

    private void handleUpdateSuccess(final String response) {
        notifyUpdateCallback(
                true,
                extractMessage(response, "Cap nhat phien thanh cong."));
    }

    private void handleUpdateFailure(final String response) {
        notifyUpdateCallback(
                false,
                extractMessage(response, "Cap nhat phien that bai."));
    }

    private void notifyPublishCallback(final boolean success, final String message) {
        final PublishItemCallback callback = publishCallback;
        publishCallback = null;
        if (callback != null) {
            callback.onResult(success, message);
        }
    }

    private void notifyUpdateCallback(final boolean success, final String message) {
        final PublishItemCallback callback = updateCallback;
        updateCallback = null;
        if (callback != null) {
            callback.onResult(success, message);
        }
    }

    private String extractMessage(final String response, final String fallback) {
        final String[] parts = response.split("\\" + Protocol.SEPARATOR, 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            return fallback;
        }
        return parts[1];
    }

    private String clean(final String value) {
        if (value == null) {
            return "";
        }
        return value.replace(Protocol.SEPARATOR, " ").trim();
    }

    private void handleError(final String response) {
        final String message =
                extractMessage(response, "Server khong xu ly duoc yeu cau.");
        if (publishCallback != null) {
            notifyPublishCallback(false, message);
        }
        if (updateCallback != null) {
            notifyUpdateCallback(false, message);
        }
    }
}
