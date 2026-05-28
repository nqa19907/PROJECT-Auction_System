package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.Protocol;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service lấy danh sách phiên đấu giá của user hiện tại.
 */
public final class MyAuctionService {
    private static final MyAuctionService INSTANCE = new MyAuctionService();

    private FetchMyAuctionsCallback currentCallback;
    private DeleteAuctionCallback deleteAuctionCallback;

    private MyAuctionService() {
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.MY_AUCTION_LIST.name(),
                this::handleMyAuctionList);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ERROR.name(),
                this::handleError);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.DELETE_MY_AUCTION_OK.name(),
                this::handleDeleteMyAuctionOk);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.DELETE_MY_AUCTION_FAIL.name(),
                this::handleDeleteMyAuctionFail);
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
     * Callback trả kết quả thao tác xóa phiên.
     */
    @FunctionalInterface
    public interface DeleteAuctionCallback {
        /**
         * Trả kết quả xóa phiên về controller.
         *
         * @param success trạng thái thành công/thất bại
         * @param message thông báo từ server
         * @param deletedAuctionId mã phiên đã xóa nếu thành công
         */
        void onResult(boolean success, String message, String deletedAuctionId);
    }

    /**
     * Gửi yêu cầu lấy danh sách phiên của user hiện tại.
     *
     * @param callback callback nhận kết quả
     */
    public void fetchMyAuctions(final FetchMyAuctionsCallback callback) {
        Objects.requireNonNull(callback, "callback");
        this.currentCallback = callback;

        final String request = Protocol.Command.LIST_MY_AUCTIONS.name();
        final boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent) {
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
        Objects.requireNonNull(callback, "callback");
        this.deleteAuctionCallback = callback;

        if (auctionId == null || auctionId.isBlank()) {
            notifyDeleteCallback(false, "Thiếu mã phiên đấu giá.", null);
            return;
        }

        final String request = Protocol.Command.DELETE_MY_AUCTION.name()
                + Protocol.SEPARATOR + auctionId.trim();

        final boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent) {
            notifyDeleteCallback(false, "Không gửi được yêu cầu xóa phiên.", null);
        }
    }

    private void handleMyAuctionList(final String response) {
        try {
            // Format: MY_AUCTION_LIST|count~row1~row2...
            final String[] records = response.split(Protocol.RECORD_SEPARATOR, -1);
            if (records.length == 0) {
                notifyCallback(false, "Response rỗng.", List.of());
                return;
            }

            final String[] header = records[0].split("\\" + Protocol.SEPARATOR, 2);
            final int count = header.length > 1 ? Integer.parseInt(header[1]) : 0;

            final List<MyAuctionRow> rows = new ArrayList<>();
            for (int i = 1; i < records.length; i++) {
                // row: id|itemName|currentPrice|status|endTime|category|description|condition
                final String[] parts = records[i].split("\\" + Protocol.SEPARATOR, -1);
                if (parts.length < 8) {
                    continue;
                }

                rows.add(new MyAuctionRow(
                        parts[0],
                        parts[1],
                        parts[2],
                        parts[3],
                        parts[4],
                        parts[5],
                        parts[6],
                        parts[7]));
            }

            if (rows.size() != count) {
                notifyCallback(
                        true,
                        "Số lượng parse lệch header, nhưng vẫn hiển thị dữ liệu.",
                        rows);
                return;
            }

            notifyCallback(true, "OK", rows);
        } catch (Exception ex) {
            notifyCallback(false, "Lỗi parse MY_AUCTION_LIST: " + ex.getMessage(), List.of());
        }
    }

    private void handleError(final String response) {
        if (currentCallback == null) {
            return;
        }
        final String[] parts = response.split("\\" + Protocol.SEPARATOR, 2);
        final String message = parts.length > 1 ? parts[1] : "Server trả lỗi.";
        notifyCallback(false, message, List.of());
    }

    private void handleDeleteMyAuctionOk(final String response) {
        final String[] parts = response.split("\\" + Protocol.SEPARATOR, 2);
        final String deletedId = parts.length > 1 ? parts[1] : "";
        notifyDeleteCallback(true, "OK", deletedId);
    }

    private void handleDeleteMyAuctionFail(final String response) {
        final String[] parts = response.split("\\" + Protocol.SEPARATOR, 2);
        final String message = parts.length > 1 ? parts[1] : "Xóa phiên thất bại.";
        notifyDeleteCallback(false, message, null);
    }

    private void notifyCallback(
            final boolean success,
            final String message,
            final List<MyAuctionRow> rows) {
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
        final DeleteAuctionCallback callback = deleteAuctionCallback;
        deleteAuctionCallback = null;
        if (callback != null) {
            callback.onResult(success, message, deletedAuctionId);
        }
    }
}
