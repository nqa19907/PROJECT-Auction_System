package auction_system.server.services;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.users.Participant;
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

        /*
         * auction.notifyObservers() phát UPDATE_PRICE cho các client đang watch
         * phiên. BALANCE_UPDATED là message cá nhân nên gửi riêng qua manager.
         */
        auction.notifyObservers();
        notifyBalanceUpdated(bidder);

        // Người dẫn đầu cũ cần nhận số dư mới sau khi được hoàn tiền.
        if (previousBidder != null && !previousBidder.getId().equals(bidder.getId())) {
            notifyBalanceUpdated(previousBidder);
        }
    }

    private void notifyBalanceUpdated(final Participant participant) {
        /*
         * participant null nghĩa là trước đó chưa có bid cao nhất, nên không có
         * người cần hoàn tiền/cập nhật ví.
         */
        if (participant == null) {
            return;
        }

        auctionManager.notifyBalanceUpdated(participant);
    }
}
