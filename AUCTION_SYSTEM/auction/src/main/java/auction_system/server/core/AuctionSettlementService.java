package auction_system.server.core;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.users.Participant;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.util.Objects;

/**
 * Chốt thanh toán khi một phiên đấu giá kết thúc.
 */
final class AuctionSettlementService {

    private final SerializedDatabase database;
    private final AuctionRealtimeNotifier notifier;

    AuctionSettlementService(
            final SerializedDatabase database,
            final AuctionRealtimeNotifier notifier) {
        this.database = Objects.requireNonNull(database, "database");
        this.notifier = Objects.requireNonNull(notifier, "notifier");
    }

    void settleFinishedAuction(final Auction auction) {
        // Tránh cộng tiền seller nhiều lần nếu scheduler chạy lại cùng phiên.
        if (auction.isSellerPaid()) {
            return;
        }

        // Phiên không có bid hợp lệ thì không có winner để thanh toán.
        final BidTransaction highestBid = auction.getCurrentHighestBid();
        if (highestBid == null || highestBid.getParticipant() == null) {
            return;
        }

        // Cần seller hợp lệ để chuyển số tiền đang giữ từ bid thắng.
        final Participant seller = auction.getParticipant();
        if (seller == null) {
            return;
        }

        // Cộng tiền cho seller, đánh dấu đã settle, rồi persist trạng thái cuối.
        seller.setBalance(seller.getBalance() + highestBid.getAmount());
        auction.setSellerPaid(true);
        database.users().save(seller);
        database.auctions().save(auction);
        database.flushAll();
        notifier.notifyBalanceUpdated(seller);
    }
}
