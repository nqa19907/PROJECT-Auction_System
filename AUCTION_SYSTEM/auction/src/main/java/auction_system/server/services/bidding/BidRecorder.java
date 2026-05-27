package auction_system.server.services.bidding;

import auction_system.common.exceptions.InvalidBidException;
import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.users.Participant;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.util.Objects;

/**
 * Ghi một lượt bid vào auction, cập nhật ví và lưu repository liên quan.
 */
final class BidRecorder {

    private final SerializedDatabase database;
    private final BidRequestValidator bidRequestValidator;
    private final BidWalletService bidWalletService;

    BidRecorder(
            final SerializedDatabase database,
            final BidRequestValidator bidRequestValidator,
            final BidWalletService bidWalletService) {

        this.database = Objects.requireNonNull(database, "database");
        this.bidRequestValidator = Objects.requireNonNull(
                bidRequestValidator,
                "bidRequestValidator");
        this.bidWalletService = Objects.requireNonNull(bidWalletService, "bidWalletService");
    }

    SavedBidResult saveBidInTransaction(
            final Auction auction,
            final Participant bidder,
            final double amount) {

        // Lấy bid dẫn đầu cũ để validate số dư và hoàn tiền nếu bị vượt.
        final BidTransaction previousHighestBid = auction.getCurrentHighestBid();

        // Chặn seller tự bid và đảm bảo bidder đủ số dư khả dụng.
        validateNotSellerBidder(auction, bidder);
        bidRequestValidator.validateAvailableBalance(
                bidder,
                previousHighestBid,
                amount);

        // Tạo bid mới và cập nhật highest bid trong auction.
        final BidTransaction bidTransaction = new BidTransaction(bidder, amount, auction);
        auction.placeBid(bidTransaction);

        // Hoàn tiền người dẫn đầu cũ và giữ tiền người dẫn đầu mới.
        final Participant previousBidder = bidWalletService.applyBidHold(
                bidder,
                previousHighestBid,
                amount);

        // Lưu lịch sử bid và trạng thái auction, caller quyết định thời điểm flush.
        database.bidTransactions().save(bidTransaction);
        database.auctions().save(auction);

        return new SavedBidResult(
                bidTransaction,
                auction,
                bidder,
                previousBidder);
    }

    private void validateNotSellerBidder(final Auction auction, final Participant bidder) {
        // Lấy sellerId từ cả auction và item vì dữ liệu có thể đến từ hai nguồn.
        final String sellerIdFromAuction = auction.getParticipant() != null
                ? auction.getParticipant().getId()
                : null;
        final String sellerIdFromItem = auction.getItem() != null
                ? auction.getItem().getSellerId()
                : null;

        // Người bán không được tự đặt giá cho sản phẩm của chính mình.
        if (bidder.getId().equals(sellerIdFromAuction)
                || bidder.getId().equals(sellerIdFromItem)) {
            throw new InvalidBidException(
                    "Người bán không được đấu giá sản phẩm của chính mình.");
        }
    }
}
