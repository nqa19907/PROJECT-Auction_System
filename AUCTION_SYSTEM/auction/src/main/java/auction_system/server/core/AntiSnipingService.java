package auction_system.server.core;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.users.User;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.util.Objects;

/**
 * Cập nhật cấu hình tự động gia hạn khi có bid phút chót cho phiên đấu giá.
 */
final class AntiSnipingService {

    private final SerializedDatabase database;
    private final AuctionRegistry auctionRegistry;
    private final AuctionLifecycleService lifecycleService;
    private final AuctionRealtimeNotifier notifier;

    AntiSnipingService(
            final SerializedDatabase database,
            final AuctionRegistry auctionRegistry,
            final AuctionLifecycleService lifecycleService,
            final AuctionRealtimeNotifier notifier) {
        this.database = Objects.requireNonNull(database, "database");
        this.auctionRegistry = Objects.requireNonNull(auctionRegistry, "auctionRegistry");
        this.lifecycleService = Objects.requireNonNull(lifecycleService, "lifecycleService");
        this.notifier = Objects.requireNonNull(notifier, "notifier");
    }

    Auction updateAntiSniping(
            final String auctionId,
            final User currentUser,
            final boolean enabled) {
        return database.executeInTransaction(() -> {
            // Lấy phiên trong transaction để cấu hình và dữ liệu lưu xuống nhất quán.
            final Auction auction = auctionRegistry.findById(auctionId);
            lifecycleService.refreshAuctionLifecycle(auction);
            if (auction == null) {
                throw new IllegalArgumentException("Không tìm thấy phiên đấu giá.");
            }

            // Chỉ seller của phiên mới được bật/tắt tự động gia hạn phút chót.
            if (!isAuctionSeller(auction, currentUser)) {
                throw new IllegalArgumentException(
                        "Chỉ người đăng bán mới được bật/tắt tự động gia hạn phút chót.");
            }

            // Persist cấu hình mới rồi báo cho các client đang xem phiên.
            auction.setAntiSnipingEnabled(enabled);
            database.auctions().save(auction);
            database.flushAll();
            notifier.notifyAntiSnipingUpdated(auction);
            return auction;
        });
    }

    private boolean isAuctionSeller(final Auction auction, final User currentUser) {
        if (auction == null || currentUser == null) {
            return false;
        }

        // Một phiên có thể giữ sellerId ở participant hoặc item sau các nhánh merge.
        final String currentUserId = currentUser.getId();
        final String sellerIdFromAuction = auction.getParticipant() != null
                ? auction.getParticipant().getId()
                : null;
        final String sellerIdFromItem = auction.getItem() != null
                ? auction.getItem().getSellerId()
                : null;

        return currentUserId != null
                && (currentUserId.equals(sellerIdFromAuction)
                || currentUserId.equals(sellerIdFromItem));
    }
}
