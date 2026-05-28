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

    private MyAuctionService() {
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.MY_AUCTION_LIST.name(),
                this::handleMyAuctionList);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ERROR.name(),
                this::handleError);
    }

    public static MyAuctionService getInstance() {
        return INSTANCE;
    }

    /**
     * Callback trả kết quả lấy danh sách phiên của user hiện tại.
     */
    @FunctionalInterface
    public interface FetchMyAuctionsCallback {
        /**
         * Trả kết quả xử lý về controller.
         *
         * @param success true nếu lấy dữ liệu thành công
         * @param message thông báo từ server
         * @param rows dữ liệu đã parse để đổ vào bảng
         */
        void onResult(boolean success, String message, List<MyAuctionRow> rows);
    }

    /**
     * Gửi lệnh lấy danh sách phiên của user hiện tại.
     *
     * @param callback hàm xử lý kết quả
     */
    public void fetchMyAuctions(final FetchMyAuctionsCallback callback) {
        Objects.requireNonNull(callback, "callback");
        this.currentCallback = callback;

        final String request = Protocol.Command.LIST_MY_AUCTIONS.name();
        final boolean sent = NetworkClient.getInstance().sendCommand(request);

        if (!sent) {
            notifyCallback(
                    false,
                    "Không gửi được yêu cầu lấy phiên của tôi.",
                    List.of());
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
                // row: auctionId|itemName|currentPrice|status|endTime
                final String[] parts = records[i].split("\\" + Protocol.SEPARATOR, -1);
                if (parts.length < 5) {
                    continue;
                }

                rows.add(new MyAuctionRow(
                        parts[0],
                        parts[1],
                        parts[2],
                        "",
                        parts[3],
                        parts[4]));
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
            notifyCallback(
                    false,
                    "Lỗi parse MY_AUCTION_LIST: " + ex.getMessage(),
                    List.of());
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
}
