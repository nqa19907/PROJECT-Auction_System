package auction_system.server.services;

import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.users.Participant;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.util.Objects;

/**
 * Xử lý giữ tiền người đặt giá mới và hoàn tiền người dẫn đầu cũ.
 */
final class BidWalletService {

    private final SerializedDatabase database;

    BidWalletService(final SerializedDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    Participant applyBidHold(
            final Participant bidder,
            final BidTransaction previousHighestBid,
            final double amount) {

        final Participant previousBidder = participantOf(previousHighestBid);

        /*
         * Mô hình ví hiện tại giữ toàn bộ giá bid mới và hoàn lại bid cũ.
         * Nếu cùng một bidder tự nâng giá, validateAvailableBalance đã cộng lại
         * số đang bị giữ để chỉ cần đủ phần chênh lệch thực tế.
         */
        refundPreviousHighestBid(previousHighestBid);
        debitBidder(bidder, amount);
        saveAffectedUsers(bidder, previousBidder);

        return previousBidder;
    }

    private Participant participantOf(final BidTransaction bidTransaction) {
        // Helper nhỏ để các nhánh không phải lặp kiểm tra null cho previousHighestBid.
        return bidTransaction == null ? null : bidTransaction.getParticipant();
    }

    private void refundPreviousHighestBid(final BidTransaction previousHighestBid) {
        /*
         * Nếu phiên chưa có bid nào thì không có tiền đang bị giữ. Khi có bid cũ,
         * trả lại đúng amount của bid đó cho người dẫn đầu cũ.
         */
        final Participant previousBidder = participantOf(previousHighestBid);
        if (previousBidder == null) {
            return;
        }

        previousBidder.setBalance(
                previousBidder.getBalance() + previousHighestBid.getAmount());
    }

    private void debitBidder(final Participant bidder, final double amount) {
        // Giữ toàn bộ số tiền bid mới trong ví bidder cho đến khi bị vượt giá hoặc thắng phiên.
        bidder.setBalance(bidder.getBalance() - amount);
    }

    private void saveAffectedUsers(
            final Participant bidder,
            final Participant previousBidder) {

        /*
         * Hai user có thể cùng là một người khi bidder tự nâng giá. Khi khác
         * người, cả bidder mới và previousBidder đều thay đổi balance cần lưu.
         */
        database.users().save(bidder);

        // Nếu bidder tự nâng giá, bidder đã được save ở trên nên không ghi trùng.
        if (previousBidder != null && !previousBidder.getId().equals(bidder.getId())) {
            database.users().save(previousBidder);
        }
    }
}
