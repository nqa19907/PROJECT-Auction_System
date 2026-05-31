package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service lấy danh sách và xóa phiên đấu giá của user hiện tại.
 */
public final class MyAuctionService {
    private static final MyAuctionService INSTANCE = new MyAuctionService();

    private FetchMyAuctionsCallback currentCallback;
    private DeleteAuctionCallback deleteAuctionCallback;

    private MyAuctionService() {
        // Đăng ký handler một lần để route response JSON về đúng callback đang chờ.
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.MY_AUCTION_LIST.name(), this::handleMyAuctionList);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ERROR.name(), this::handleError);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.DELETE_MY_AUCTION_OK.name(), this::handleDeleteMyAuctionOk);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.DELETE_MY_AUCTION_FAIL.name(), this::handleDeleteMyAuctionFail);
    }

    public static MyAuctionService getInstance() {
        return INSTANCE;
    }

    /**
     * Callback trả kết quả lấy danh sách phiên của user hiện tại.
     */
    @FunctionalInterface
    public interface FetchMyAuctionsCallback {
        void onResult(boolean success, String message, List<MyAuctionRow> rows);
    }

    /**
     * Callback trả kết quả xóa phiên.
     */
    @FunctionalInterface
    public interface DeleteAuctionCallback {
        void onResult(boolean success, String message, String deletedAuctionId);
    }

    /**
     * Gửi yêu cầu lấy danh sách phiên của user hiện tại.
     *
     * @param callback callback nhận kết quả
     */
    public void fetchMyAuctions(final FetchMyAuctionsCallback callback) {
        // Ghi nhớ callback trước khi gửi request để nhận danh sách bất đồng bộ.
        Objects.requireNonNull(callback, "callback");
        currentCallback = callback;

        if (!NetworkClient.getInstance().sendCommand(request(
                Protocol.Command.LIST_MY_AUCTIONS, null))) {
            notifyCallback(false, "Không gửi được yêu cầu lấy phiên của tôi.", List.of());
        }
    }

    /**
     * Gửi yêu cầu xóa phiên do user hiện tại sở hữu.
     *
     * @param auctionId mã phiên cần xóa
     * @param callback callback nhận kết quả
     */
    public void deleteMyAuction(final String auctionId, final DeleteAuctionCallback callback) {
        // Validate mã phiên tại client để tránh gửi request chắc chắn thất bại.
        Objects.requireNonNull(callback, "callback");
        deleteAuctionCallback = callback;
        if (auctionId == null || auctionId.isBlank()) {
            notifyDeleteCallback(false, "Thiếu mã phiên đấu giá.", null);
            return;
        }

        // Gửi mã phiên trong payload array để dispatcher server chuyển thành parts.
        if (!NetworkClient.getInstance().sendCommand(request(
                Protocol.Command.DELETE_MY_AUCTION,
                List.of(auctionId.trim())))) {
            notifyDeleteCallback(false, "Không gửi được yêu cầu xóa phiên.", null);
        }
    }

    private String request(final Protocol.Command command, final Object payload) {
        // Tạo request JSON chung cho các thao tác quản lý phiên.
        return JsonProtocol.stringifyRequired(new JsonMessage(
                null,
                command.name(),
                null,
                payload == null ? null : JsonProtocol.payloadOf(payload),
                null));
    }

    private void handleMyAuctionList(final String response) {
        try {
            // Parse mảng auction từ payload và ánh xạ từng JSON object sang row hiển thị.
            final JsonNode rowsNode = JsonProtocol.parse(response).payload().path("auctions");
            final List<MyAuctionRow> rows = new ArrayList<>();
            for (JsonNode row : rowsNode) {
                rows.add(new MyAuctionRow(
                        text(row, "id"),
                        text(row, "itemName"),
                        text(row, "currentPrice"),
                        text(row, "status"),
                        text(row, "startTime"),
                        text(row, "endTime"),
                        text(row, "category"),
                        text(row, "description"),
                        text(row, "condition")));
            }
            notifyCallback(true, "OK", rows);
        } catch (IOException | RuntimeException exception) {
            notifyCallback(false, "Lỗi parse MY_AUCTION_LIST: "
                    + exception.getMessage(), List.of());
        }
    }

    private String text(final JsonNode node, final String field) {
        return node.path(field).asText("");
    }

    private void handleError(final String response) {
        // ERROR chung chỉ được nhận khi màn hình đang chờ danh sách phiên.
        if (currentCallback != null) {
            notifyCallback(false, message(response, "Server trả lỗi."), List.of());
        }
    }

    private void handleDeleteMyAuctionOk(final String response) {
        try {
            // Lấy auctionId đã xóa để controller thông báo và tải lại bảng.
            final JsonMessage json = JsonProtocol.parse(response);
            notifyDeleteCallback(
                    true,
                    message(json, "Xóa phiên thành công."),
                    json.payload().path("auctionId").asText(""));
        } catch (IOException | RuntimeException exception) {
            notifyDeleteCallback(false, "Lỗi parse kết quả xóa phiên.", null);
        }
    }

    private void handleDeleteMyAuctionFail(final String response) {
        notifyDeleteCallback(false, message(response, "Xóa phiên thất bại."), null);
    }

    private String message(final String response, final String fallback) {
        try {
            // Ưu tiên message do server trả về, dùng fallback nếu JSON không hợp lệ.
            return message(JsonProtocol.parse(response), fallback);
        } catch (IOException exception) {
            return fallback;
        }
    }

    private String message(final JsonMessage response, final String fallback) {
        return response.message() == null || response.message().isBlank()
                ? fallback
                : response.message();
    }

    private void notifyCallback(
            final boolean success,
            final String message,
            final List<MyAuctionRow> rows) {
        // Giải phóng callback sau một response để tránh gọi lại từ notification khác.
        final FetchMyAuctionsCallback callback = currentCallback;
        currentCallback = null;
        if (callback != null) {
            callback.onResult(success, message, rows);
        }
    }

    private void notifyDeleteCallback(
            final boolean success,
            final String message,
            final String deletedAuctionId) {
        // Callback xóa được quản lý riêng vì có thể tồn tại song song với request danh sách.
        final DeleteAuctionCallback callback = deleteAuctionCallback;
        deleteAuctionCallback = null;
        if (callback != null) {
            callback.onResult(success, message, deletedAuctionId);
        }
    }
}
