package auction_system.server.core;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import java.util.Objects;
import org.slf4j.Logger;

/**
 * Xử lý thao tác quản trị trực tiếp trên phiên đấu giá.
 */
final class AuctionAdministrationService {

    private final AuctionRegistry auctionRegistry;
    private final AuctionLifecycleService lifecycleService;
    private final Logger logger;

    AuctionAdministrationService(
            final AuctionRegistry auctionRegistry,
            final AuctionLifecycleService lifecycleService,
            final Logger logger) {
        this.auctionRegistry = Objects.requireNonNull(auctionRegistry, "auctionRegistry");
        this.lifecycleService = Objects.requireNonNull(lifecycleService, "lifecycleService");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    boolean cancelAuction(final String auctionId) {
        final Auction auction = findFreshAuction(auctionId);
        if (auction == null) {
            return false;
        }

        // Đánh dấu hủy và báo cho các observer đang theo dõi phiên.
        auction.setStatus(AuctionStatus.CANCELED);
        auction.notifyObservers("AUCTION_ENDED|" + auctionId + "|NONE");
        logger.info("Huỷ phiên đấu giá: " + auctionId);
        return true;
    }

    boolean deleteAuction(final String auctionId) {
        final Auction auction = findFreshAuction(auctionId);
        if (auction == null) {
            return false;
        }

        // Xóa khỏi registry runtime trước, rồi xóa bản ghi bền vững trong database.
        auctionRegistry.delete(auction);
        logger.info("Xóa phiên đấu giá: " + auctionId);
        return true;
    }

    private Auction findFreshAuction(final String auctionId) {
        final Auction auction = auctionRegistry.findById(auctionId);
        lifecycleService.refreshAuctionLifecycle(auction);
        return auction;
    }
}
