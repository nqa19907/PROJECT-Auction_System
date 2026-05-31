package auction_system.server.core;

import auction_system.common.models.auctions.Auction;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * Scheduler định kỳ cập nhật lifecycle cho tất cả phiên đấu giá.
 */
final class AuctionLifecycleScheduler {
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final ScheduledExecutorService scheduler;
    private final AuctionRegistry auctionRegistry;
    private final AuctionLifecycleService lifecycleService;
    private final Logger logger;
    private final int intervalSeconds;

    AuctionLifecycleScheduler(
            final AuctionRegistry auctionRegistry,
            final AuctionLifecycleService lifecycleService,
            final Logger logger,
            final int intervalSeconds) {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.auctionRegistry = auctionRegistry;
        this.lifecycleService = lifecycleService;
        this.logger = logger;
        this.intervalSeconds = intervalSeconds;
    }

    void start() {
        scheduler.scheduleAtFixedRate(() -> {
            for (final Auction auction : auctionRegistry.getAll()) {
                try {
                    lifecycleService.refreshAuctionLifecycle(auction);
                } catch (Exception e) {
                    logger.warn("Lỗi scheduler phiên " + auction.getId()
                            + ": " + e.getMessage());
                }
            }
        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException exception) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
