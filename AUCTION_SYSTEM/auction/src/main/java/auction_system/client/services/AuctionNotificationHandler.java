package auction_system.client.services;

import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý các thông báo realtime không gắn với callback request của AuctionService.
 */
final class AuctionNotificationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionNotificationHandler.class);

    private static final int MIN_AUCTION_ENDED_PARTS = 3;
    private static final int IDX_AUCTION_WINNER_NAME = 2;
    private static final int IDX_AUCTION_ENDED_ITEM_NAME = 3;
    private static final int MIN_AUCTION_WINNER_PARTS = 3;
    private static final int IDX_AUCTION_WINNER_ITEM_NAME = 2;
    private static final int MIN_AUCTION_LOST_PARTS = 4;
    private static final int IDX_AUCTION_LOST_ITEM_NAME = 2;
    private static final int IDX_AUCTION_LOST_WINNER_NAME = 3;
    private static final int MIN_BALANCE_UPDATED_PARTS = 2;
    private static final int IDX_BALANCE_UPDATED_VALUE = 1;

    /**
     * Xử lý thông báo phiên đấu giá kết thúc cho các client đang xem phiên.
     *
     * @param response thông báo AUCTION_ENDED từ server
     */
    void handleAuctionEndedResponse(final String response) {
        final String[] parts = splitKeepingEmptyFields(response);

        // Một số phiên có thể không có winner, nên fallback NONE vẫn được hiển thị rõ.
        final String winnerName = parts.length >= MIN_AUCTION_ENDED_PARTS
                ? parts[IDX_AUCTION_WINNER_NAME]
                : "NONE";

        final String itemName = parts.length > IDX_AUCTION_ENDED_ITEM_NAME
                ? parts[IDX_AUCTION_ENDED_ITEM_NAME]
                : "";

        showInfo(
                "Kết thúc đấu giá",
                "người chiến thắng vật phẩm " + itemName + " là " + winnerName);
    }

    /**
     * Xử lý thông báo riêng gửi cho người thắng phiên đấu giá.
     *
     * @param response thông báo AUCTION_WINNER từ server
     */
    void handleAuctionWinnerResponse(final String response) {
        // Ưu tiên đọc AUCTION_WINNER JSON, fallback xuống protocol string cũ.
        if (JsonProtocol.isJsonObject(response)) {
            handleAuctionWinnerJsonResponse(response);
            return;
        }

        final String[] parts = splitKeepingEmptyFields(response);

        // Tên vật phẩm nằm sau auctionId trong response riêng cho người thắng.
        final String itemName = parts.length >= MIN_AUCTION_WINNER_PARTS
                ? parts[IDX_AUCTION_WINNER_ITEM_NAME]
                : "";

        showInfo("Thông báo", "bạn đã thắng vật phẩm " + itemName);
    }

    /**
     * Xử lý thông báo riêng gửi cho người thua phiên đấu giá.
     *
     * @param response thông báo AUCTION_LOST từ server
     */
    void handleAuctionLostResponse(final String response) {
        // Ưu tiên đọc AUCTION_LOST JSON, fallback xuống protocol string cũ.
        if (JsonProtocol.isJsonObject(response)) {
            handleAuctionLostJsonResponse(response);
            return;
        }

        final String[] parts = splitKeepingEmptyFields(response);

        // Response thua phiên cần cả tên vật phẩm và tên người thắng để hiển thị popup.
        final String itemName = parts.length >= MIN_AUCTION_LOST_PARTS
                ? parts[IDX_AUCTION_LOST_ITEM_NAME]
                : "";
        final String winnerName = parts.length >= MIN_AUCTION_LOST_PARTS
                ? parts[IDX_AUCTION_LOST_WINNER_NAME]
                : "NONE";

        showInfo(
                "Kết thúc đấu giá",
                "người chiến thắng vật phẩm " + itemName + " là " + winnerName);
    }

    /**
     * Cập nhật số dư realtime khi server hoàn/trừ tiền do bid.
     *
     * @param response phản hồi BALANCE_UPDATED từ server
     */
    void handleBalanceUpdatedResponse(final String response) {
        // Ưu tiên đọc BALANCE_UPDATED JSON, fallback xuống protocol string cũ.
        if (JsonProtocol.isJsonObject(response)) {
            handleBalanceUpdatedJsonResponse(response);
            return;
        }

        final String[] parts = splitKeepingEmptyFields(response);
        if (parts.length < MIN_BALANCE_UPDATED_PARTS) {
            return;
        }

        try {
            // Số dư realtime đến từ server là nguồn đúng sau khi hoàn/trừ tiền bid.
            UserSessionService.getInstance().updateCurrentUserBalance(
                    Double.parseDouble(parts[IDX_BALANCE_UPDATED_VALUE]));
        } catch (NumberFormatException exception) {
            LOGGER.warn("Không thể đọc số dư realtime: {}", parts[IDX_BALANCE_UPDATED_VALUE]);
        }
    }

    private void handleAuctionWinnerJsonResponse(final String response) {
        try {
            // Payload winner chứa itemName để popup không phụ thuộc thứ tự field string.
            final JsonMessage message = JsonProtocol.parse(response);
            final JsonNode payload = message.payload();
            final String itemName = payload == null ? "" : payload.path("itemName").asText("");
            showInfo("Thông báo", "bạn đã thắng vật phẩm " + itemName);
        } catch (IOException exception) {
            LOGGER.warn("Không thể đọc JSON AUCTION_WINNER: {}", exception.getMessage());
        }
    }

    private void handleAuctionLostJsonResponse(final String response) {
        try {
            // Payload loser chứa itemName và winnerUsername để popup hiển thị đủ ngữ cảnh.
            final JsonMessage message = JsonProtocol.parse(response);
            final JsonNode payload = message.payload();
            final String itemName = payload == null ? "" : payload.path("itemName").asText("");
            final String winnerName = payload == null
                    ? "NONE"
                    : payload.path("winnerUsername").asText("NONE");
            showInfo(
                    "Kết thúc đấu giá",
                    "người chiến thắng vật phẩm " + itemName + " là " + winnerName);
        } catch (IOException exception) {
            LOGGER.warn("Không thể đọc JSON AUCTION_LOST: {}", exception.getMessage());
        }
    }

    private void handleBalanceUpdatedJsonResponse(final String response) {
        try {
            // Payload số dư realtime chứa balance là nguồn đúng sau khi hoàn/trừ tiền bid.
            final JsonMessage message = JsonProtocol.parse(response);
            final JsonNode payload = message.payload();
            if (payload == null || !payload.has("balance")) {
                return;
            }

            UserSessionService.getInstance().updateCurrentUserBalance(
                    payload.path("balance").asDouble());
        } catch (IOException exception) {
            LOGGER.warn("Không thể đọc JSON BALANCE_UPDATED: {}", exception.getMessage());
        }
    }

    /**
     * Tách response nhưng giữ field rỗng ở cuối để các index protocol không bị lệch.
     *
     * @param response response text protocol
     * @return các field đã tách
     */
    private String[] splitKeepingEmptyFields(final String response) {
        return response.split(Protocol.SEPARATOR_REGEX, -1);
    }

    /**
     * Hiện pop up thông báo đơn giản trên client.
     *
     * @param title tiêu đề pop up
     * @param content nội dung pop up
     */
    private void showInfo(final String title, final String content) {
        final Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
