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

        for (int i = FIRST_AUCTION_RECORD_INDEX; i < records.length; i++) {
            final String[] parts = records[i].split(Protocol.SEPARATOR_REGEX);
            if (parts.length >= MIN_AUCTION_LIST_PARTS) {
                auctionList.add(parts);
            }
        }

        return auctionList;
    }

    static String[] parseAuctionDetail(final String response) {
        return response.split(Protocol.SEPARATOR_REGEX);
    }

    static OptionalDouble parseBidOkBalance(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX);
        if (parts.length < MIN_BID_OK_PARTS) {
            LOGGER.warn("Phản hồi BID_OK thiếu số dư mới.");
            return OptionalDouble.empty();
        }

        return parseDouble(parts[IDX_BID_NEW_BALANCE], "BID_OK");
    }

    static String parseBidFailureMessage(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX);

        return parts.length >= MIN_BID_FAIL_PARTS
                ? parts[IDX_BID_FAIL_MESSAGE]
                : "Lỗi đặt giá không xác định.";
    }

    static List<String[]> parseBidHistory(final String response) {
        final List<String[]> bidHistoryRows = new ArrayList<>();
        final String[] records = response.split(Protocol.RECORD_SEPARATOR);

        for (int i = FIRST_BID_HISTORY_RECORD_INDEX; i < records.length; i++) {
            final String[] parts = records[i].split(Protocol.SEPARATOR_REGEX);
            if (parts.length >= MIN_BID_HISTORY_PARTS) {
                bidHistoryRows.add(parts);
            }
        }

        return bidHistoryRows;
    }

    static OptionalDouble parseBalanceUpdate(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX);
        if (parts.length < MIN_BALANCE_UPDATED_PARTS) {
            LOGGER.warn("BALANCE_UPDATED không hợp lệ: {}", response);
            return OptionalDouble.empty();
        }

        return parseDouble(parts[IDX_BALANCE_UPDATED_BALANCE], "BALANCE_UPDATED");
    }

    private static OptionalDouble parseDouble(final String rawValue, final String responseName) {
        try {
            return OptionalDouble.of(Double.parseDouble(rawValue));
        } catch (NumberFormatException exception) {
            LOGGER.warn("Không thể đọc số từ {}: {}", responseName, rawValue);
            return OptionalDouble.empty();
        }
    }
}
