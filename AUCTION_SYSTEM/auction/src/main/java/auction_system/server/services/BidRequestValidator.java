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

        double availableBalance = bidder.getBalance();

        if (isSameBidder(bidder, previousHighestBid)) {
            availableBalance += previousHighestBid.getAmount();
        }

        if (amount > availableBalance) {
            throw new InvalidBidException("Không đủ số dư để đặt giá.");
        }
    }

    void validateAuctionId(final String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            throw new InvalidBidException("Mã phiên đấu giá không được rỗng.");
        }
    }

    private boolean isSameBidder(
            final Participant bidder,
            final BidTransaction previousHighestBid) {

        return previousHighestBid != null
                && previousHighestBid.getParticipant() != null
                && previousHighestBid.getParticipant().getId().equals(bidder.getId());
    }
}
