package auction_system.client.controllers.auction;

import auction_system.common.network.Protocol;
import java.util.ArrayList;
import java.util.List;

/**
 * Bóc tách response protocol dành riêng cho màn hình Admin Dashboard.
 */
final class AdminDashboardResponseParser {
    private static final int FIRST_DATA_RECORD_INDEX = 1;
    private static final int MIN_USER_FIELDS = 4;
    private static final int MIN_AUCTION_FIELDS = 5;
    private static final int IDX_MESSAGE = 1;

    /**
     * Parse danh sách user từ response ADMIN_USER_LIST.
     *
     * @param response response dạng header|count~userId|username|email|status|role~...
     * @return danh sách row user hợp lệ
     */
    List<AdminUserRow> parseUsers(final String response) {
        final List<AdminUserRow> rows = new ArrayList<>();
        final String[] records = response.split(Protocol.RECORD_SEPARATOR);

        for (int i = FIRST_DATA_RECORD_INDEX; i < records.length; i++) {
            final String[] parts = splitRecord(records[i]);

            // Chỉ nhận record đủ các cột bảng đang hiển thị để tránh lệch dữ liệu.
            if (parts.length >= MIN_USER_FIELDS) {
                rows.add(new AdminUserRow(parts[0], parts[1], parts[2], parts[3]));
            }
        }

        return rows;
    }

    /**
     * Parse danh sách phiên đấu giá từ response ADMIN_AUCTION_LIST.
     *
     * @param response response dạng
     *     header|count~auctionId|productName|seller|currentPrice|status~...
     * @return danh sách row auction hợp lệ
     */
    List<AdminAuctionRow> parseAuctions(final String response) {
        final List<AdminAuctionRow> rows = new ArrayList<>();
        final String[] records = response.split(Protocol.RECORD_SEPARATOR);

        for (int i = FIRST_DATA_RECORD_INDEX; i < records.length; i++) {
            final String[] parts = splitRecord(records[i]);

            // Các field sau status nếu có không thuộc bảng dashboard nên được bỏ qua.
            if (parts.length >= MIN_AUCTION_FIELDS) {
                rows.add(new AdminAuctionRow(
                        parts[0],
                        parts[1],
                        parts[2],
                        parts[3],
                        parts[4]));
            }
        }

        return rows;
    }

    /**
     * Lấy id entity vừa bị xóa từ response OK.
     *
     * @param response response dạng RESPONSE|id
     * @return id nếu response có đủ field
     */
    String parseDeletedId(final String response) {
        final String[] parts = splitRecord(response);
        return parts.length > IDX_MESSAGE ? parts[IDX_MESSAGE] : "";
    }

    /**
     * Lấy message lỗi từ response FAIL.
     *
     * @param response response dạng RESPONSE|message
     * @param fallback message dùng khi server trả thiếu field
     * @return message lỗi để hiển thị
     */
    String parseFailureMessage(final String response, final String fallback) {
        final String[] parts = splitRecord(response);
        return parts.length > IDX_MESSAGE && !parts[IDX_MESSAGE].isBlank()
                ? parts[IDX_MESSAGE]
                : fallback;
    }

    private String[] splitRecord(final String record) {
        return record.split(Protocol.SEPARATOR_REGEX, -1);
    }
}
