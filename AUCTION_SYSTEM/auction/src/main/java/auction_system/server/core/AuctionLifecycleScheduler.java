package auction_system.server.core;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduler chịu trách nhiệm chuyển trạng thái phiên theo thời gian.
 */
final class AuctionLifecycleScheduler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuctionLifecycleScheduler.class);
    private static final int SCHEDULER_INTERVAL_SECONDS = 10;

    private final AuctionRegistry auctionRegistry;
    private final ScheduledExecutorService executor;

    AuctionLifecycleScheduler(final AuctionRegistry auctionRegistry) {
        this.auctionRegistry = auctionRegistry;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    void start() {
        executor.scheduleAtFixedRate(
                this::updateAuctionStates,
                0,
                SCHEDULER_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
    }

    void shutdown() {
        executor.shutdown();
    }

    private void updateAuctionStates() {
        for (final Auction auction : auctionRegistry.findAll()) {
            try {
                updateAuctionState(auction);
            } catch (Exception exception) {
                LOGGER.warn(
                        "Lỗi scheduler phiên {}: {}",
                        auction.getId(),
                        exception.getMessage());
            }
        }
    }

    private void updateAuctionState(final Auction auction) {
        final AuctionStatus status = auction.getStatus();
        if (status == AuctionStatus.OPEN) {
            auction.startAuction();
        } else if (status == AuctionStatus.RUNNING) {
            auction.endAuction();
        }
    }
}
