package auction_system.server.services.auction;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.server.core.AuctionManager;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.util.Objects;

/**
 * Cập nhật lifecycle của auction trước khi xử lý bid.
 */
public final class AuctionLifecycleRefresher {

    private final SerializedDatabase database;
    private final AuctionManager auctionManager;

    /**
     * Khởi tạo helper cập nhật trạng thái phiên đấu giá.
     *
     * @param database database serialization của server
     * @param auctionManager manager runtime dùng để settle và gửi realtime
     */
    public AuctionLifecycleRefresher(
            final SerializedDatabase database,
            final AuctionManager auctionManager) {

        this.database = Objects.requireNonNull(database, "database");
        this.auctionManager = auctionManager;
    }

    /**
     * Cập nhật trạng thái phiên theo thời gian hiện tại và gửi thông báo nếu cần.
     *
     * @param auction phiên đấu giá cần cập nhật lifecycle
     */
    public void refreshAuctionLifecycle(final Auction auction) {
        // Chạy transition theo thời gian thật trước khi nhận hoặc retry bid.
        final AuctionStatus oldStatus = auction.getStatus();
        auction.startAuction();
        auction.endAuction();

        // Chỉ persist và notify khi status thật sự thay đổi.
        if (oldStatus != auction.getStatus()) {
            database.auctions().save(auction);
            database.flushAll();
            // Phiên vừa kết thúc cần settle winner và gửi kết quả realtime.
            if (auctionManager != null && auction.getStatus() == AuctionStatus.FINISHED) {
                auctionManager.settleFinishedAuction(auction);
                auctionManager.notifyAuctionResult(auction);
            }
        }
    }
}
