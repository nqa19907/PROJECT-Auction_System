package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Service phía Client dùng để gửi yêu cầu đăng bán sản phẩm lên Server.
 *
 * <p>Lớp này giúp Controller không phụ thuộc trực tiếp vào tầng network,
 * giữ đúng nguyên tắc SRP: Controller chỉ xử lý UI, Service xử lý request.</p>
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
     * Lấy instance duy nhất của service đăng bán sản phẩm.
     *
     * @return Instance dùng chung của {@code ItemPublishService}.
     */
    public static ItemPublishService getInstance() {
        return INSTANCE;
    }

    /**
     * Callback nhận kết quả đăng bán từ Server.
     */
    @FunctionalInterface
    public interface PublishItemCallback {
        /**
         * Xử lý kết quả đăng bán sản phẩm.
         *
         * @param success {@code true} nếu đăng bán thành công.
         * @param message Thông báo trả về từ Server.
         */
        void onResult(boolean success, String message);
    }

    /**
     * Gửi yêu cầu đăng bán sản phẩm lên Server.
     *
     * @param category Loại sản phẩm.
     * @param itemName Tên sản phẩm.
     * @param description Mô tả sản phẩm.
     * @param condition Tình trạng sản phẩm.
     * @param startPrice Giá khởi điểm.
     * @param startTime Thời điểm bắt đầu đấu giá.
     * @param endTime Thời điểm kết thúc đấu giá.
     * @param callback Callback nhận kết quả.
     */
    public void publishItem(
            String category,
            String itemName,
            String description,
            String condition,
            double startPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            PublishItemCallback callback) {
        // Giữ luồng đăng sản phẩm cũ hoạt động khi chưa chọn ảnh.
        publishItem(
                category,
                itemName,
                description,
                condition,
                startPrice,
                startTime,
                endTime,
                "",
                false,
                callback);
    }

    /**
     * Gửi yêu cầu đăng bán sản phẩm kèm đường dẫn ảnh lên Server.
     *
     * @param category Loại sản phẩm.
     * @param itemName Tên sản phẩm.
     * @param description Mô tả sản phẩm.
     * @param condition Tình trạng sản phẩm.
     * @param startPrice Giá khởi điểm.
     * @param startTime Thời điểm bắt đầu đấu giá.
     * @param endTime Thời điểm kết thúc đấu giá.
     * @param imagePath Đường dẫn ảnh sản phẩm đã được lưu.
     * @param callback Callback nhận kết quả.
     */
    public void publishItem(
            String category,
            String itemName,
            String description,
            String condition,
            double startPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String imagePath,
            PublishItemCallback callback) {
        // Giữ caller cũ hoạt động với anti-sniping mặc định tắt.
        publishItem(
                category,
                itemName,
                description,
                condition,
                startPrice,
                startTime,
                endTime,
                imagePath,
                false,
                callback);
    }

    /**
     * Gửi yêu cầu đăng bán sản phẩm kèm cấu hình gia hạn phút chót.
     *
     * @param category Loại sản phẩm.
     * @param itemName Tên sản phẩm.
     * @param description Mô tả sản phẩm.
     * @param condition Tình trạng sản phẩm.
     * @param startPrice Giá khởi điểm.
     * @param startTime Thời điểm bắt đầu đấu giá.
     * @param endTime Thời điểm kết thúc đấu giá.
     * @param imagePath Đường dẫn ảnh sản phẩm đã được lưu.
     * @param antiSnipingEnabled true nếu bật tự động gia hạn phút chót.
     * @param callback Callback nhận kết quả.
     */
    public void publishItem(
            String category,
            String itemName,
            String description,
            String condition,
            double startPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String imagePath,
            boolean antiSnipingEnabled,
            PublishItemCallback callback) {

        Objects.requireNonNull(callback, "Callback không được null.");
        this.publishCallback = callback;

        // Đóng gói request đăng sản phẩm cùng metadata ảnh.
        String request = buildPublishItemRequest(
                category,
                itemName,
                description,
                condition,
                startPrice,
                startTime,
                endTime,
                imagePath,
                antiSnipingEnabled);

        boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent) {
            LOGGER.warning("Không thể gửi yêu cầu đăng bán sản phẩm tới Server.");
            callback.onResult(false, "Không thể kết nối tới Server.");
        }
    }

    /**
     * Cập nhật thông tin phiên do user hiện tại đăng.
     *
     * @param auctionId mã phiên cần cập nhật
     * @param category danh mục mới
     * @param itemName tên tài sản mới
     * @param description mô tả mới
     * @param condition tình trạng mới
     * @param endTime thời gian kết thúc mới
     * @param callback callback nhận kết quả
     */
    public void updateMyAuction(
            final String auctionId,
            final String category,
            final String itemName,
            final String description,
            final String condition,
            final LocalDateTime endTime,
            final PublishItemCallback callback) {
        // Ghi nhớ callback update riêng để không xung đột với request đăng bán.
        Objects.requireNonNull(callback, "Callback không được null.");
        updateCallback = callback;

        // Đóng gói các field chỉnh sửa theo đúng thứ tự command server đang đọc.
        final String request = JsonProtocol.stringifyRequired(
                new JsonMessage(
                        null,
                        Protocol.Command.UPDATE_MY_AUCTION.name(),
                        null,
                        JsonProtocol.payloadOf(List.of(
                                nullToEmpty(auctionId),
                                nullToEmpty(category),
                                nullToEmpty(itemName),
                                nullToEmpty(description),
                                nullToEmpty(condition),
                                FORMATTER.format(endTime))),
                        null));

        if (!NetworkClient.getInstance().sendCommand(request)) {
            LOGGER.warning("Không thể gửi yêu cầu cập nhật phiên tới Server.");
            notifyUpdateCallback(false, "Không thể kết nối tới Server.");
        }
    }

    /**
     * Xử lý phản hồi đăng bán thành công từ Server.
     *
     * @param response Phản hồi dạng text protocol từ Server.
     */
    private void handlePublishSuccess(String response) {
        notifyCallback(true, extractMessage(response, "Đăng bán sản phẩm thành công."));
    }

    /**
     * Xử lý phản hồi đăng bán thất bại từ Server.
     *
     * @param response Phản hồi dạng text protocol từ Server.
     */
    private void handlePublishFailure(String response) {
        notifyCallback(false, extractMessage(response, "Đăng bán sản phẩm thất bại."));
    }

    /**
     * Gọi callback hiện tại nếu tồn tại.
     *
     * @param success Trạng thái xử lý.
     * @param message Thông báo kết quả.
     */
    private void notifyCallback(boolean success, String message) {
        PublishItemCallback callback = publishCallback;
        publishCallback = null;

        if (callback != null) {
            callback.onResult(success, message);
        }
    }

    private void handleUpdateSuccess(final String response) {
        notifyUpdateCallback(true, extractMessage(response, "Cập nhật phiên thành công."));
    }

    private void handleUpdateFailure(final String response) {
        notifyUpdateCallback(false, extractMessage(response, "Cập nhật phiên thất bại."));
    }

    private void notifyUpdateCallback(final boolean success, final String message) {
        // Giải phóng callback update ngay sau khi trả kết quả về controller.
        final PublishItemCallback callback = updateCallback;
        updateCallback = null;
        if (callback != null) {
            callback.onResult(success, message);
        }
    }

    /**
     * Lấy thông báo từ response của Server.
     *
     * @param response Chuỗi phản hồi từ Server.
     * @param fallback Thông báo mặc định.
     * @return Thông báo đã tách được.
     */
    private String extractMessage(String response, String fallback) {
        try {
            // Chỉ đọc message từ JSON response; không hỗ trợ text protocol cũ.
            final JsonMessage message = JsonProtocol.parse(response);
            if (message.message() == null || message.message().isBlank()) {
                return fallback;
            }
            return message.message();
        } catch (IOException exception) {
            return fallback;
        }
    }

    private String buildPublishItemRequest(
            final String category,
            final String itemName,
            final String description,
            final String condition,
            final double startPrice,
            final LocalDateTime startTime,
            final LocalDateTime endTime,
            final String imagePath,
            final boolean antiSnipingEnabled) {
        // Payload array giữ nguyên thứ tự field mà PublishItemCommand yêu cầu.
        return JsonProtocol.stringifyRequired(
                new JsonMessage(
                        null,
                        Protocol.Command.PUBLISH_ITEM.name(),
                        null,
                        JsonProtocol.payloadOf(List.of(
                                nullToEmpty(category),
                                nullToEmpty(itemName),
                                nullToEmpty(description),
                                nullToEmpty(condition),
                                startPrice,
                                FORMATTER.format(startTime),
                                FORMATTER.format(endTime),
                                nullToEmpty(imagePath),
                                antiSnipingEnabled)),
                        null));
    }

    private String nullToEmpty(final String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Xử lý lỗi chung từ server trong lúc đang chờ kết quả đăng bán.
     *
     * @param response phản hồi lỗi từ server
     */
    private void handleError(final String response) {
        // ERROR chung được chuyển tới mọi thao tác publish hoặc update đang chờ.
        final String message = extractMessage(response, "Server không xử lý được yêu cầu.");
        if (publishCallback != null) {
            notifyCallback(false, message);
        }
        if (updateCallback != null) {
            notifyUpdateCallback(false, message);
        }
    }
}
