package auction_system.client.services;

import auction_system.common.network.Protocol;
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
