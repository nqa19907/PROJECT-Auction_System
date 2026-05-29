package auction_system.client.services;

import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bóc tách các response text protocol liên quan đến đấu giá ở phía client.
 */
final class AuctionResponseParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionResponseParser.class);

    // AUCTION_LIST có dòng đầu là header, dữ liệu phiên bắt đầu từ dòng tiếp theo.
    private static final int FIRST_AUCTION_RECORD_INDEX = 1;
    private static final int MIN_AUCTION_LIST_PARTS = 6;

    // BID_FAIL|message.
    private static final int MIN_BID_FAIL_PARTS = 2;
    private static final int IDX_BID_FAIL_MESSAGE = 1;

    // BID_OK|auctionId|amount|newBalance.
    private static final int MIN_BID_OK_PARTS = 4;
    private static final int IDX_BID_NEW_BALANCE = 3;

    // BID_HISTORY|auctionId|count~time|bidder|amount~...
    private static final int FIRST_BID_HISTORY_RECORD_INDEX = 1;
    private static final int MIN_BID_HISTORY_PARTS = 3;

    // AUTO_BID_STATUS|ENABLED|maxAmount|stepAmount hoặc AUTO_BID_STATUS|DISABLED.
    private static final int MIN_AUTO_BID_ENABLED_PARTS = 4;
    private static final int IDX_AUTO_BID_STATUS = 1;
    private static final int IDX_AUTO_BID_MAX_AMOUNT = 2;
    private static final int IDX_AUTO_BID_STEP_AMOUNT = 3;
    private static final String AUTO_BID_ENABLED = "ENABLED";

    /**
     * Parse danh sách phiên đấu giá từ response nhiều record.
     *
     * @param response response AUCTION_LIST từ server
     * @return danh sách record phiên đấu giá đủ field
     */
    List<String[]> parseAuctionList(final String response) {
        if (JsonProtocol.isJsonObject(response)) {
            return parseJsonAuctionList(response);
        }

        List<String[]> auctionList = new ArrayList<>();
        String[] lines = response.split(Protocol.RECORD_SEPARATOR);

        for (int i = FIRST_AUCTION_RECORD_INDEX; i < lines.length; ++i) {
            String[] parts = lines[i].split(Protocol.SEPARATOR_REGEX);

            // Bỏ qua record thiếu field để bảng không render dữ liệu lệch cột.
            if (parts.length >= MIN_AUCTION_LIST_PARTS) {
                auctionList.add(parts);
            }
        }

        return auctionList;
    }

    private List<String[]> parseJsonAuctionList(final String response) {
        List<String[]> auctionList = new ArrayList<>();

        try {
            final JsonMessage message = JsonProtocol.parse(response);
            final JsonNode payload = message.payload();
            final JsonNode auctions = payload != null && payload.has("auctions")
                    ? payload.path("auctions")
                    : payload;

            if (auctions == null || !auctions.isArray()) {
                LOGGER.warn("Phản hồi AUCTION_LIST JSON thiếu danh sách phiên.");
                return auctionList;
            }

            for (JsonNode auction : auctions) {
                if (!auction.isArray() || auction.size() < MIN_AUCTION_LIST_PARTS) {
                    continue;
                }

                String[] parts = new String[auction.size()];
                for (int i = 0; i < auction.size(); i++) {
                    parts[i] = auction.get(i).asText();
                }
                auctionList.add(parts);
            }
        } catch (IOException exception) {
            LOGGER.warn("Không thể đọc JSON AUCTION_LIST: {}", exception.getMessage());
        }

        return auctionList;
    }

    /**
     * Parse chi tiết phiên đấu giá từ response một record.
     *
     * @param response response AUCTION_DETAIL từ server
     * @return các field chi tiết theo thứ tự protocol
     */
    String[] parseAuctionDetail(final String response) {
        if (JsonProtocol.isJsonObject(response)) {
            return parseJsonAuctionDetail(response);
        }

        return response.split(Protocol.SEPARATOR_REGEX);
    }

    private String[] parseJsonAuctionDetail(final String response) {
        try {
            final JsonMessage message = JsonProtocol.parse(response);
            final JsonNode payload = message.payload();
            final JsonNode auction = payload != null && payload.has("auction")
                    ? payload.path("auction")
                    : payload;

            if (auction == null || !auction.isArray()) {
                LOGGER.warn("Phản hồi AUCTION_DETAIL JSON thiếu dữ liệu phiên.");
                return new String[0];
            }

            String[] parts = new String[auction.size() + 1];
            parts[0] = Protocol.Response.AUCTION_DETAIL.name();
            for (int i = 0; i < auction.size(); i++) {
                parts[i + 1] = auction.get(i).asText();
            }

            return parts;
        } catch (IOException exception) {
            LOGGER.warn("Không thể đọc JSON AUCTION_DETAIL: {}", exception.getMessage());
            return new String[0];
        }
    }

    /**
     * Đọc số dư mới từ response BID_OK.
     *
     * @param response response BID_OK từ server
     * @return số dư mới nếu parse được
     */
    OptionalDouble parseBidBalance(final String response) {
        if (JsonProtocol.isJsonObject(response)) {
            try {
                final JsonMessage message = JsonProtocol.parse(response);
                final JsonNode payload = message.payload();
                if (payload == null || payload.isNull() || !payload.has("newBalance")) {
                    LOGGER.warn("Phản hồi BID_OK JSON thiếu số dư mới.");
                    return OptionalDouble.empty();
                }

                return OptionalDouble.of(payload.path("newBalance").asDouble());
            } catch (IOException exception) {
                LOGGER.warn("Không thể đọc JSON BID_OK: {}", exception.getMessage());
                return OptionalDouble.empty();
            }
        }

        String[] parts = response.split(Protocol.SEPARATOR_REGEX);

        // Trả về rỗng khi thiếu field để tránh cập nhật sai số dư user hiện tại.
        if (parts.length < MIN_BID_OK_PARTS) {
            LOGGER.warn("Phản hồi BID_OK thiếu số dư mới.");
            return OptionalDouble.empty();
        }

        try {
            return OptionalDouble.of(Double.parseDouble(parts[IDX_BID_NEW_BALANCE]));
        } catch (NumberFormatException e) {
            LOGGER.warn(
                    "Không thể đọc số dư mới từ phản hồi BID_OK: {}",
                    parts[IDX_BID_NEW_BALANCE]);
            return OptionalDouble.empty();
        }
    }

    /**
     * Lấy message lỗi từ response BID_FAIL.
     *
     * @param response response BID_FAIL từ server
     * @return message lỗi hoặc fallback mặc định
     */
    String parseBidFailureMessage(final String response) {
        if (JsonProtocol.isJsonObject(response)) {
            try {
                final JsonMessage message = JsonProtocol.parse(response);
                return message.message() == null || message.message().isBlank()
                        ? "Lỗi đặt giá không xác định."
                        : message.message();
            } catch (IOException exception) {
                return "Lỗi đặt giá không xác định.";
            }
        }

        String[] parts = response.split(Protocol.SEPARATOR_REGEX);
        return parts.length >= MIN_BID_FAIL_PARTS
                ? parts[IDX_BID_FAIL_MESSAGE]
                : "Lỗi đặt giá không xác định.";
    }

    /**
     * Lấy message từ response thành công dạng {@code RESPONSE|message}.
     *
     * @param response phản hồi từ server
     * @return message thành công nếu có, hoặc thông báo mặc định
     */
    String parseSimpleSuccessMessage(final String response) {
        if (JsonProtocol.isJsonObject(response)) {
            return parseJsonMessage(response, "Yêu cầu thành công.");
        }

        String[] parts = response.split(Protocol.SEPARATOR_REGEX, 2);
        return parts.length >= 2 ? parts[1] : "Yêu cầu thành công.";
    }

    /**
     * Lấy message từ response lỗi dạng {@code RESPONSE|message}.
     *
     * @param response phản hồi từ server
     * @return message lỗi nếu có, hoặc thông báo mặc định
     */
    String parseSimpleFailureMessage(final String response) {
        if (JsonProtocol.isJsonObject(response)) {
            return parseJsonMessage(response, "Yêu cầu không thành công.");
        }

        String[] parts = response.split(Protocol.SEPARATOR_REGEX, 2);
        return parts.length >= 2 ? parts[1] : "Yêu cầu không thành công.";
    }

    private String parseJsonMessage(final String response, final String fallback) {
        try {
            final JsonMessage message = JsonProtocol.parse(response);
            return message.message() == null || message.message().isBlank()
                    ? fallback
                    : message.message();
        } catch (IOException exception) {
            return fallback;
        }
    }

    /**
     * Parse trạng thái auto-bid hiện tại của user trong phiên.
     *
     * @param response response AUTO_BID_STATUS từ server
     * @return trạng thái auto-bid đã chuẩn hóa cho callback
     */
    AutoBidStatus parseAutoBidStatus(final String response) {
        if (JsonProtocol.isJsonObject(response)) {
            try {
                final JsonMessage message = JsonProtocol.parse(response);
                final JsonNode payload = message.payload();
                if (payload == null || payload.isNull() || !payload.path("enabled").asBoolean()) {
                    return new AutoBidStatus(false, 0L, 0L);
                }

                return new AutoBidStatus(
                        true,
                        payload.path("maxAmount").asLong(),
                        payload.path("stepAmount").asLong());
            } catch (IOException exception) {
                return new AutoBidStatus(false, 0L, 0L);
            }
        }

        String[] parts = response.split(Protocol.SEPARATOR_REGEX);

        // Chỉ trạng thái ENABLED đủ field mới có maxAmount và stepAmount hợp lệ.
        if (parts.length >= MIN_AUTO_BID_ENABLED_PARTS
                && AUTO_BID_ENABLED.equals(parts[IDX_AUTO_BID_STATUS])) {
            return new AutoBidStatus(
                    true,
                    parseLongOrZero(parts[IDX_AUTO_BID_MAX_AMOUNT]),
                    parseLongOrZero(parts[IDX_AUTO_BID_STEP_AMOUNT]));
        }

        return new AutoBidStatus(false, 0L, 0L);
    }

    /**
     * Parse lịch sử bid từ response nhiều record.
     *
     * @param response response BID_HISTORY từ server
     * @return danh sách dòng lịch sử bid đủ field
     */
    List<String[]> parseBidHistory(final String response) {
        if (JsonProtocol.isJsonObject(response)) {
            return parseJsonBidHistory(response);
        }

        List<String[]> bidHistoryRows = new ArrayList<>();
        String[] records = response.split(Protocol.RECORD_SEPARATOR);

        for (int i = FIRST_BID_HISTORY_RECORD_INDEX; i < records.length; i++) {
            String[] parts = records[i].split(Protocol.SEPARATOR_REGEX);

            // Header đã bị bỏ qua; mỗi record còn lại cần time/bidder/amount.
            if (parts.length >= MIN_BID_HISTORY_PARTS) {
                bidHistoryRows.add(parts);
            }
        }

        return bidHistoryRows;
    }

    private List<String[]> parseJsonBidHistory(final String response) {
        List<String[]> bidHistoryRows = new ArrayList<>();

        try {
            final JsonMessage message = JsonProtocol.parse(response);
            final JsonNode payload = message.payload();
            final JsonNode bids = payload != null && payload.has("bids")
                    ? payload.path("bids")
                    : payload;

            if (bids == null || !bids.isArray()) {
                LOGGER.warn("Phản hồi BID_HISTORY JSON thiếu danh sách bid.");
                return bidHistoryRows;
            }

            for (JsonNode bid : bids) {
                if (!bid.isArray() || bid.size() < MIN_BID_HISTORY_PARTS) {
                    continue;
                }

                String[] parts = new String[bid.size()];
                for (int i = 0; i < bid.size(); i++) {
                    parts[i] = bid.get(i).asText();
                }
                bidHistoryRows.add(parts);
            }
        } catch (IOException exception) {
            LOGGER.warn("Không thể đọc JSON BID_HISTORY: {}", exception.getMessage());
        }

        return bidHistoryRows;
    }

    /**
     * Chuyển chuỗi sang long nhưng giữ fallback 0 khi server trả dữ liệu lỗi.
     *
     * @param rawValue giá trị số dạng chuỗi
     * @return giá trị long hoặc 0
     */
    private long parseLongOrZero(final String rawValue) {
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    /**
     * Dữ liệu trạng thái auto-bid đã parse cho callback.
     *
     * @param enabled true nếu auto-bid đang bật
     * @param maxAmount giá tối đa
     * @param stepAmount bước tăng mỗi lần bid tự động
     */
    record AutoBidStatus(boolean enabled, long maxAmount, long stepAmount) {
    }
}
