package auction_system.client.controllers.auction;

import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bóc tách response protocol dành riêng cho màn hình Admin Dashboard.
 */
final class AdminDashboardResponseParser {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(AdminDashboardResponseParser.class);
    private static final int MIN_USER_FIELDS = 4;
    private static final int MIN_AUCTION_FIELDS = 5;

    /**
     * Parse danh sách user từ response ADMIN_USER_LIST.
     *
     * @param response response JSON chứa danh sách user trong payload
     * @return danh sách row user hợp lệ
     */
    List<AdminUserRow> parseUsers(final String response) {
        return parseJsonUsers(response);
    }

    /**
     * Parse danh sách phiên đấu giá từ response ADMIN_AUCTION_LIST.
     *
     * @param response response dạng
     *     response JSON chứa danh sách phiên trong payload
     * @return danh sách row auction hợp lệ
     */
    List<AdminAuctionRow> parseAuctions(final String response) {
        return parseJsonAuctions(response);
    }

    /**
     * Lấy id entity vừa bị xóa từ response OK.
     *
     * @param response response JSON chứa payload.id
     * @return id nếu response có đủ field
     */
    String parseDeletedId(final String response) {
        try {
            // Response delete JSON đặt id đã xóa trong payload để UI loại đúng dòng.
            final JsonMessage message = JsonProtocol.parse(response);
            final JsonNode payload = message.payload();
            if (payload == null || payload.isNull()) {
                return "";
            }

            return payload.path("id").asText("");
        } catch (IOException exception) {
            LOGGER.warn("Không thể đọc JSON id đã xóa: {}", exception.getMessage());
            return "";
        }
    }

    /**
     * Lấy message lỗi từ response FAIL.
     *
     * @param response response JSON chứa message
     * @param fallback message dùng khi server trả thiếu field
     * @return message lỗi để hiển thị
     */
    String parseFailureMessage(final String response, final String fallback) {
        try {
            // Failure JSON lấy message trực tiếp từ wrapper.
            final JsonMessage message = JsonProtocol.parse(response);
            return message.message() == null || message.message().isBlank()
                    ? fallback
                    : message.message();
        } catch (IOException exception) {
            LOGGER.warn("Không thể đọc JSON lỗi admin dashboard: {}",
                    exception.getMessage());
            return fallback;
        }
    }

    private List<AdminUserRow> parseJsonUsers(final String response) {
        final List<AdminUserRow> rows = new ArrayList<>();

        try {
            // Payload users giữ mỗi user là array theo thứ tự cột dashboard hiện tại.
            final JsonMessage message = JsonProtocol.parse(response);
            final JsonNode users = resolveArrayPayload(message.payload(), "users");
            if (users == null) {
                LOGGER.warn("Phản hồi ADMIN_USER_LIST JSON thiếu danh sách user.");
                return rows;
            }

            for (JsonNode user : users) {
                if (!user.isArray() || user.size() < MIN_USER_FIELDS) {
                    continue;
                }

                rows.add(new AdminUserRow(
                        user.get(0).asText(),
                        user.get(1).asText(),
                        user.get(2).asText(),
                        user.get(3).asText()));
            }
        } catch (IOException exception) {
            LOGGER.warn("Không thể đọc JSON ADMIN_USER_LIST: {}", exception.getMessage());
        }

        return rows;
    }

    private List<AdminAuctionRow> parseJsonAuctions(final String response) {
        final List<AdminAuctionRow> rows = new ArrayList<>();

        try {
            // Payload auctions giữ mỗi phiên là array theo thứ tự cột dashboard hiện tại.
            final JsonMessage message = JsonProtocol.parse(response);
            final JsonNode auctions = resolveArrayPayload(message.payload(), "auctions");
            if (auctions == null) {
                LOGGER.warn("Phản hồi ADMIN_AUCTION_LIST JSON thiếu danh sách phiên.");
                return rows;
            }

            for (JsonNode auction : auctions) {
                if (!auction.isArray() || auction.size() < MIN_AUCTION_FIELDS) {
                    continue;
                }

                rows.add(new AdminAuctionRow(
                        auction.get(0).asText(),
                        auction.get(1).asText(),
                        auction.get(2).asText(),
                        auction.get(3).asText(),
                        auction.get(4).asText()));
            }
        } catch (IOException exception) {
            LOGGER.warn("Không thể đọc JSON ADMIN_AUCTION_LIST: {}", exception.getMessage());
        }

        return rows;
    }

    private JsonNode resolveArrayPayload(final JsonNode payload, final String fieldName) {
        if (payload == null || payload.isNull()) {
            return null;
        }

        final JsonNode records = payload.has(fieldName) ? payload.path(fieldName) : payload;
        return records.isArray() ? records : null;
    }
}
