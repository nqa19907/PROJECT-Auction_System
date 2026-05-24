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

        refundPreviousHighestBid(previousHighestBid);
        debitBidder(bidder, amount);
        saveAffectedUsers(bidder, previousBidder);

        return previousBidder;
    }

    private Participant participantOf(final BidTransaction bidTransaction) {
        return bidTransaction == null ? null : bidTransaction.getParticipant();
    }

    private void refundPreviousHighestBid(final BidTransaction previousHighestBid) {
        final Participant previousBidder = participantOf(previousHighestBid);
        if (previousBidder == null) {
            return;
        }

        previousBidder.setBalance(
                previousBidder.getBalance() + previousHighestBid.getAmount());
    }

    private void debitBidder(final Participant bidder, final double amount) {
        bidder.setBalance(bidder.getBalance() - amount);
    }

    private void saveAffectedUsers(
            final Participant bidder,
            final Participant previousBidder) {

        database.users().save(bidder);

        if (previousBidder != null && !previousBidder.getId().equals(bidder.getId())) {
            database.users().save(previousBidder);
        }
    }
}
