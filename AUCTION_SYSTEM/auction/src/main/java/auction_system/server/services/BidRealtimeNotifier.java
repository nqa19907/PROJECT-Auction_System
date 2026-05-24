package auction_system.server.services;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.users.Participant;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import java.util.Objects;

/**
 * Phát realtime sau khi bid đã được lưu thành công.
 */
final class BidRealtimeNotifier {

    private final AuctionManager auctionManager;

    BidRealtimeNotifier(final AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    void notifyBidSaved(
            final Auction auction,
            final Participant bidder,
            final Participant previousBidder) {

        auction.notifyObservers();
        notifyBalanceUpdated(bidder);

        if (previousBidder != null && !previousBidder.getId().equals(bidder.getId())) {
            notifyBalanceUpdated(previousBidder);
        }
    }

    private void notifyBalanceUpdated(final Participant participant) {
        if (participant == null) {
            return;
        }

        final String message = Protocol.Response.BALANCE_UPDATED.name()
                + Protocol.SEPARATOR
                + participant.getId()
                + Protocol.SEPARATOR
                + participant.getBalance();

        auctionManager.notifyUser(participant.getId(), message);
    }
}
