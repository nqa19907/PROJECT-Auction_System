package auction_system.server.core;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.util.Objects;

/**
 * Cập nhật trạng thái phiên đấu giá theo thời gian và xử lý side effect khi kết thúc.
 */
final class AuctionLifecycleService {

    private final SerializedDatabase database;
    private final AuctionRegistry auctionRegistry;
    private final AuctionSettlementService settlementService;
    private final AuctionRealtimeNotifier notifier;

    AuctionLifecycleService(
            final SerializedDatabase database,
            final AuctionRegistry auctionRegistry,
            final AuctionSettlementService settlementService,
            final AuctionRealtimeNotifier notifier) {
        this.database = Objects.requireNonNull(database, "database");
        this.auctionRegistry = Objects.requireNonNull(auctionRegistry, "auctionRegistry");
        this.settlementService = Objects.requireNonNull(settlementService, "settlementService");
        this.notifier = Objects.requireNonNull(notifier, "notifier");
    }

    void refreshAllAuctionLifecycles() {
        for (final Auction auction : auctionRegistry.getAll()) {
            refreshAuctionLifecycle(auction);
        }
    }

    void refreshAuctionLifecycle(final Auction auction) {
        if (auction == null) {
            return;
        }

        // Cho model tự chuyển PENDING -> ACTIVE -> FINISHED theo mốc thời gian.
        final AuctionStatus oldStatus = auction.getStatus();
        auction.startAuction();
        auction.endAuction();

        // Chỉ ghi database và phát sự kiện khi trạng thái thật sự thay đổi.
        if (oldStatus != auction.getStatus()) {
            database.auctions().save(auction);
            database.flushAll();

            // Khi phiên vừa kết thúc, chốt tiền cho seller và gửi kết quả realtime.
            if (auction.getStatus() == AuctionStatus.FINISHED) {
                settlementService.settleFinishedAuction(auction);
                notifier.notifyAuctionResult(auction);
            }
        }
    }
}
