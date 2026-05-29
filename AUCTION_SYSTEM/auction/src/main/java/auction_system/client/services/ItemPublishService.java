package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import com.fasterxml.jackson.core.JsonProcessingException;
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

    private PublishItemCallback currentCallback;

    private ItemPublishService() {
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.PUBLISH_ITEM_OK.name(),
                this::handlePublishSuccess);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.PUBLISH_ITEM_FAIL.name(),
                this::handlePublishFailure);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ERROR.name(), 
                this::handlePublishError);
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

        Objects.requireNonNull(callback, "Callback không được null.");
        this.currentCallback = callback;

        String request = buildPublishItemRequest(
                category,
                itemName,
                description,
                condition,
                startPrice,
                startTime,
                endTime);

        boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent) {
            LOGGER.warning("Không thể gửi yêu cầu đăng bán sản phẩm tới Server.");
            callback.onResult(false, "Không thể kết nối tới Server.");
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
        PublishItemCallback callback = currentCallback;
        currentCallback = null;

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
        if (JsonProtocol.isJsonObject(response)) {
            try {
                final JsonMessage message = JsonProtocol.parse(response);
                return message.message() == null || message.message().isBlank()
                        ? fallback
                        : message.message();
            } catch (IOException exception) {
                return fallback;
            }
        }

        String[] parts = response.split("\\" + Protocol.SEPARATOR, 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            return fallback;
        }
        return parts[1];
    }

    /**
     * Làm sạch dữ liệu text trước khi đưa vào text protocol.
     *
     * @param value Giá trị đầu vào.
     * @return Giá trị đã loại bỏ ký tự phân tách protocol.
     */
    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace(Protocol.SEPARATOR, " ")
                .trim();
    }

    private String buildPublishItemRequest(
            final String category,
            final String itemName,
            final String description,
            final String condition,
            final double startPrice,
            final LocalDateTime startTime,
            final LocalDateTime endTime) {
        try {
            return JsonProtocol.stringify(
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
                                    FORMATTER.format(endTime))),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warning("Không tạo được JSON request đăng bán sản phẩm: "
                    + exception.getMessage());
            return String.join(
                    Protocol.SEPARATOR,
                    Protocol.Command.PUBLISH_ITEM.name(),
                    clean(category),
                    clean(itemName),
                    clean(description),
                    clean(condition),
                    String.valueOf(startPrice),
                    FORMATTER.format(startTime),
                    FORMATTER.format(endTime));
        }
    }

    private String nullToEmpty(final String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Xử lý lỗi chung từ server trong lúc đang chờ kết quả đăng bán.
     *
     * @param response phản hồi lỗi từ server
     */
    private void handlePublishError(final String response) {
        if (currentCallback == null) {
            return;
        }

        notifyCallback(
                false,
                extractMessage(response, "Server không xử lý được yêu cầu đăng bán."));
    }
}
