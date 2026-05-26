package auction_system.server.services;

import auction_system.common.exceptions.InvalidBidException;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;

/**
 * Validator cho request đặt giá và các điều kiện số dư trước khi ghi bid.
 */
final class BidRequestValidator {

    void validatePlaceBidRequest(
            final String auctionId,
            final User currentUser,
            final double amount) {

        /*
         * Nhóm validate này chỉ kiểm tra request thô: có phiên, có đăng nhập,
         * đúng role và số tiền dương. Các điều kiện phụ thuộc auction cụ thể
         * được kiểm tra sau khi transaction đã lấy được auction hiện tại.
         */
        validateAuctionId(auctionId);

        if (currentUser == null) {
            throw new InvalidBidException("Bạn cần đăng nhập trước khi đặt giá.");
        }

        if (!(currentUser instanceof Participant)) {
            throw new InvalidBidException("Chỉ người mua mới có thể đặt giá.");
        }

        if (amount <= 0) {
            throw new InvalidBidException("Số tiền đặt giá phải lớn hơn 0.");
        }
    }

    void validateAvailableBalance(
            final Participant bidder,
            final BidTransaction previousHighestBid,
            final double amount) {

        /*
         * availableBalance không đơn giản là balance hiện tại nếu bidder đang là
         * người dẫn đầu. Số tiền bid cũ đang bị giữ sẽ được hoàn trước khi giữ
         * bid mới, nên phải cộng lại để kiểm tra khả năng chi trả.
         */
        double availableBalance = bidder.getBalance();

        if (isSameBidder(bidder, previousHighestBid)) {
            // Bid trước của chính user đang bị giữ, nên tính lại như tiền khả dụng.
            availableBalance += previousHighestBid.getAmount();
        }

        if (amount > availableBalance) {
            throw new InvalidBidException("Không đủ số dư để đặt giá.");
        }
    }

    void validateAuctionId(final String auctionId) {
        // Dùng chung cho cả placeBid và getBidHistory vì hai lệnh đều cần auctionId hợp lệ.
        if (auctionId == null || auctionId.isBlank()) {
            throw new InvalidBidException("Mã phiên đấu giá không được rỗng.");
        }
    }

    private boolean isSameBidder(
            final Participant bidder,
            final BidTransaction previousHighestBid) {

        /*
         * So sánh bằng id thay vì object reference vì Participant có thể được
         * deserialize hoặc lấy từ repository ở các thời điểm khác nhau.
         */
        return previousHighestBid != null
                && previousHighestBid.getParticipant() != null
                && previousHighestBid.getParticipant().getId().equals(bidder.getId());
    }
}
