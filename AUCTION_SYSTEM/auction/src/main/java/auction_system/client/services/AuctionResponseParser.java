package auction_system.client.services;

import auction_system.common.network.Protocol;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser cho các response liên quan đến phiên đấu giá ở phía client.
 *
 * <p>Lớp này gom hiểu biết về định dạng protocol để AuctionService không phải
 * vừa điều phối request/callback vừa tự bóc tách chuỗi response.
 */
final class AuctionResponseParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionResponseParser.class);

    // AUCTION_LIST|count~auctionId|itemName|currentPrice|status|endTime|...
    private static final int FIRST_AUCTION_RECORD_INDEX = 1;
    private static final int MIN_AUCTION_LIST_PARTS = 5;

    // BID_FAIL|message.
    private static final int MIN_BID_FAIL_PARTS = 2;
    private static final int IDX_BID_FAIL_MESSAGE = 1;

    // BID_OK|auctionId|amount|newBalance.
    private static final int MIN_BID_OK_PARTS = 4;
    private static final int IDX_BID_NEW_BALANCE = 3;

    // BID_HISTORY|auctionId|count~time|bidder|amount~...
    private static final int FIRST_BID_HISTORY_RECORD_INDEX = 1;
    private static final int MIN_BID_HISTORY_PARTS = 3;

    // BALANCE_UPDATED|userId|newBalance.
    private static final int MIN_BALANCE_UPDATED_PARTS = 3;
    private static final int IDX_BALANCE_UPDATED_BALANCE = 2;

    private AuctionResponseParser() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    static List<String[]> parseAuctionList(final String response) {
        final List<String[]> auctionList = new ArrayList<>();
        final String[] records = response.split(Protocol.RECORD_SEPARATOR);

        /*
         * Record đầu tiên là header AUCTION_LIST|count. Các record sau giữ nguyên
         * dạng String[] để lớp ViewModel/controller hiện tại tiếp tục dùng index
         * field cũ mà không cần đổi toàn bộ contract UI.
         */
        for (int i = FIRST_AUCTION_RECORD_INDEX; i < records.length; i++) {
            final String[] parts = records[i].split(Protocol.SEPARATOR_REGEX);
            if (parts.length >= MIN_AUCTION_LIST_PARTS) {
                auctionList.add(parts);
            }
        }

        return auctionList;
    }

    static String[] parseAuctionDetail(final String response) {
        /*
         * Detail response đang được ViewModel đọc theo vị trí field. Parser chỉ
         * tách chuỗi, chưa đổi sang record riêng để tránh làm rộng phạm vi refactor.
         */
        return response.split(Protocol.SEPARATOR_REGEX);
    }

    static OptionalDouble parseBidOkBalance(final String response) {
        /*
         * BID_OK trả số dư mới để client cập nhật UserSession ngay sau bid.
         * OptionalDouble giúp service phía trên phân biệt lỗi parse với số dư 0.
         */
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX);
        if (parts.length < MIN_BID_OK_PARTS) {
            LOGGER.warn("Phản hồi BID_OK thiếu số dư mới.");
            return OptionalDouble.empty();
        }

        return parseDouble(parts[IDX_BID_NEW_BALANCE], "BID_OK");
    }

    static String parseBidFailureMessage(final String response) {
        /*
         * Server gửi message lỗi nghiệp vụ ở field thứ hai. Nếu response bị thiếu
         * field, client vẫn cần một câu lỗi an toàn để hiển thị dưới form bid.
         */
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX);

        return parts.length >= MIN_BID_FAIL_PARTS
                ? parts[IDX_BID_FAIL_MESSAGE]
                : "Lỗi đặt giá không xác định.";
    }

    static List<String[]> parseBidHistory(final String response) {
        final List<String[]> bidHistoryRows = new ArrayList<>();
        final String[] records = response.split(Protocol.RECORD_SEPARATOR);

        /*
         * BID_HISTORY gồm header và nhiều dòng time|bidder|amount. Bỏ qua dòng
         * thiếu field để bảng lịch sử vẫn render được các dòng hợp lệ còn lại.
         */
        for (int i = FIRST_BID_HISTORY_RECORD_INDEX; i < records.length; i++) {
            final String[] parts = records[i].split(Protocol.SEPARATOR_REGEX);
            if (parts.length >= MIN_BID_HISTORY_PARTS) {
                bidHistoryRows.add(parts);
            }
        }

        return bidHistoryRows;
    }

    static OptionalDouble parseBalanceUpdate(final String response) {
        /*
         * BALANCE_UPDATED là realtime cá nhân theo user. Chỉ số dư mới cần được
         * đưa vào UserSessionService, userId đã được server route đúng socket.
         */
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX);
        if (parts.length < MIN_BALANCE_UPDATED_PARTS) {
            LOGGER.warn("BALANCE_UPDATED không hợp lệ: {}", response);
            return OptionalDouble.empty();
        }

        return parseDouble(parts[IDX_BALANCE_UPDATED_BALANCE], "BALANCE_UPDATED");
    }

    private static OptionalDouble parseDouble(final String rawValue, final String responseName) {
        /*
         * Không ném exception lên tầng UI vì response mạng lỗi không nên làm
         * hỏng handler. Service sẽ quyết định fallback khi OptionalDouble rỗng.
         */
        try {
            return OptionalDouble.of(Double.parseDouble(rawValue));
        } catch (NumberFormatException exception) {
            LOGGER.warn("Không thể đọc số từ {}: {}", responseName, rawValue);
            return OptionalDouble.empty();
        }
    }
}
