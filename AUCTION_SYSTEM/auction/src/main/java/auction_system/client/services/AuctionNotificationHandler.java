package auction_system.client.services;

import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
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

    /**
     * Xử lý thông báo phiên đấu giá kết thúc cho các client đang xem phiên.
     *
     * @param response thông báo AUCTION_ENDED từ server
     */
    void handleAuctionEndedResponse(final String response) {
        handleAuctionEndedJsonResponse(response);
    }

    private void handleAuctionEndedJsonResponse(final String response) {
        try {
            // Payload kết thúc phiên chứa itemName và winnerUsername để hiển thị popup.
            final JsonMessage message = JsonProtocol.parse(response);
            final JsonNode payload = message.payload();
            final String winnerName = payload == null
                    ? "NONE"
                    : payload.path("winnerUsername").asText("NONE");
            final String itemName = payload == null ? "" : payload.path("itemName").asText("");
            showInfo(
                    "Kết thúc đấu giá",
                    "người chiến thắng vật phẩm " + itemName + " là " + winnerName);
        } catch (IOException exception) {
            LOGGER.warn("Không thể đọc JSON AUCTION_ENDED: {}", exception.getMessage());
        }
    }

    /**
     * Xử lý thông báo riêng gửi cho người thắng phiên đấu giá.
     *
     * @param response thông báo AUCTION_WINNER từ server
     */
    void handleAuctionWinnerResponse(final String response) {
        handleAuctionWinnerJsonResponse(response);
    }

    /**
     * Xử lý thông báo riêng gửi cho người thua phiên đấu giá.
     *
     * @param response thông báo AUCTION_LOST từ server
     */
    void handleAuctionLostResponse(final String response) {
        handleAuctionLostJsonResponse(response);
    }

    /**
     * Cập nhật số dư realtime khi server hoàn/trừ tiền do bid.
     *
     * @param response phản hồi BALANCE_UPDATED từ server
     */
    void handleBalanceUpdatedResponse(final String response) {
        handleBalanceUpdatedJsonResponse(response);
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
