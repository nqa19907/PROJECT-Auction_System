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

    private static final int MIN_AUCTION_LIST_PARTS = 6;

    private static final int MIN_BID_HISTORY_PARTS = 3;

    /**
     * Parse danh sách phiên đấu giá từ response nhiều record.
     *
     * @param response response AUCTION_LIST từ server
     * @return danh sách record phiên đấu giá đủ field
     */
    List<String[]> parseAuctionList(final String response) {
        return parseJsonAuctionList(response);
    }

    private List<String[]> parseJsonAuctionList(final String response) {
        List<String[]> auctionList = new ArrayList<>();

        try {
            // Payload JSON giữ mỗi phiên là một array theo đúng thứ tự field cũ.
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
        return parseJsonAuctionDetail(response);
    }

    private String[] parseJsonAuctionDetail(final String response) {
        try {
            // Thêm header AUCTION_DETAIL vào đầu để giữ contract String[] cũ cho caller.
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

    /**
     * Lấy message lỗi từ response BID_FAIL.
     *
     * @param response response BID_FAIL từ server
     * @return message lỗi hoặc thông báo mặc định
     */
    String parseBidFailureMessage(final String response) {
        try {
            final JsonMessage message = JsonProtocol.parse(response);
            return message.message() == null || message.message().isBlank()
                    ? "Lỗi đặt giá không xác định."
                    : message.message();
        } catch (IOException exception) {
            return "Lỗi đặt giá không xác định.";
        }
    }

    /**
     * Lấy message từ response thành công dạng JSON.
     *
     * @param response phản hồi từ server
     * @return message thành công nếu có, hoặc thông báo mặc định
     */
    String parseSimpleSuccessMessage(final String response) {
        return parseJsonMessage(response, "Yêu cầu thành công.");
    }

    /**
     * Lấy message từ response lỗi dạng JSON.
     *
     * @param response phản hồi từ server
     * @return message lỗi nếu có, hoặc thông báo mặc định
     */
    String parseSimpleFailureMessage(final String response) {
        return parseJsonMessage(response, "Yêu cầu không thành công.");
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

    /**
     * Parse lịch sử bid từ response nhiều record.
     *
     * @param response response BID_HISTORY từ server
     * @return danh sách dòng lịch sử bid đủ field
     */
    List<String[]> parseBidHistory(final String response) {
        return parseJsonBidHistory(response);
    }

    private List<String[]> parseJsonBidHistory(final String response) {
        List<String[]> bidHistoryRows = new ArrayList<>();

        try {
            // Payload JSON giữ mỗi bid là một array time/bidder/amount cho ViewModel cũ.
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
     * Dữ liệu trạng thái auto-bid đã parse cho callback.
     *
     * @param enabled true nếu auto-bid đang bật
     * @param maxAmount giá tối đa
     * @param stepAmount bước tăng mỗi lần bid tự động
     */
    record AutoBidStatus(boolean enabled, long maxAmount, long stepAmount) {
    }
}
