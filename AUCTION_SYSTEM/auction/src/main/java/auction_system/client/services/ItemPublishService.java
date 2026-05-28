package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.Protocol;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Service phía Client dùng để gửi yêu cầu đăng bán/cập nhật phiên lên Server.
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
     * Lấy singleton của service.
     *
     * @return instance dùng chung
     */
    public static ItemPublishService getInstance() {
        return INSTANCE;
    }

    /**
     * Callback nhận kết quả từ Server.
     */
    @FunctionalInterface
    public interface PublishItemCallback {
        /**
         * Trả kết quả cho controller gọi service.
         *
         * @param success trạng thái thành công
         * @param message thông báo trả về
         */
        void onResult(boolean success, String message);
    }

    /**
     * Gửi yêu cầu đăng bán phiên mới.
     *
     * @param category danh mục sản phẩm
     * @param itemName tên sản phẩm
     * @param description mô tả sản phẩm
     * @param condition tình trạng sản phẩm
     * @param startPrice giá khởi điểm
     * @param startTime thời gian bắt đầu
     * @param endTime thời gian kết thúc
     * @param callback callback nhận kết quả
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
        Objects.requireNonNull(callback, "Callback không được null.");
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
            LOGGER.warning("Không thể gửi yêu cầu đăng bán sản phẩm tới Server.");
            callback.onResult(false, "Không thể kết nối tới Server.");
        }
    }

    /**
     * Cập nhật phiên của user hiện tại.
     * Không gửi thời gian và giá khởi điểm theo yêu cầu.
     *
     * @param auctionId mã phiên cần cập nhật
     * @param category danh mục mới
     * @param itemName tên tài sản mới
     * @param description mô tả mới
     * @param condition tình trạng mới
     * @param callback callback nhận kết quả
     */
    public void updateMyAuction(
            final String auctionId,
            final String category,
            final String itemName,
            final String description,
            final String condition,
            final PublishItemCallback callback) {
        Objects.requireNonNull(callback, "Callback không được null.");
        this.updateCallback = callback;

        final String request = String.join(
                Protocol.SEPARATOR,
                Protocol.Command.UPDATE_MY_AUCTION.name(),
                clean(auctionId),
                clean(category),
                clean(itemName),
                clean(description),
                clean(condition));

        final boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent) {
            LOGGER.warning("Không thể gửi yêu cầu cập nhật phiên tới Server.");
            callback.onResult(false, "Không thể kết nối tới Server.");
        }
    }

    private void handlePublishSuccess(final String response) {
        notifyPublishCallback(
                true,
                extractMessage(response, "Đăng bán sản phẩm thành công."));
    }

    private void handlePublishFailure(final String response) {
        notifyPublishCallback(
                false,
                extractMessage(response, "Đăng bán sản phẩm thất bại."));
    }

    private void handleUpdateSuccess(final String response) {
        notifyUpdateCallback(
                true,
                extractMessage(response, "Cập nhật phiên thành công."));
    }

    private void handleUpdateFailure(final String response) {
        notifyUpdateCallback(
                false,
                extractMessage(response, "Cập nhật phiên thất bại."));
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
                extractMessage(response, "Server không xử lý được yêu cầu.");
        if (publishCallback != null) {
            notifyPublishCallback(false, message);
        }
        if (updateCallback != null) {
            notifyUpdateCallback(false, message);
        }
    }
}
