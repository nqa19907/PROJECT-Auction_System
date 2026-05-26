package auction_system.server.core;

import auction_system.common.models.auctions.Auction;
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
        /*
         * Chạy ngay lúc start để trạng thái dữ liệu vừa nạp từ disk được cập
         * nhật trước khi client đầu tiên lấy danh sách phiên.
         */
        executor.scheduleAtFixedRate(
                this::updateAuctionStates,
                0,
                SCHEDULER_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
    }

    void shutdown() {
        /*
         * Server shutdown cần dừng scheduler mềm trước. Nếu task đang bị treo quá
         * 2 giây thì shutdownNow để JVM không giữ thread nền.
         */
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void updateAuctionStates() {
        for (final Auction auction : auctionRegistry.findAll()) {
            try {
                // Lỗi của một phiên không được làm scheduler dừng toàn bộ vòng quét.
                auctionRegistry.refreshAuctionLifecycle(auction);
            } catch (Exception exception) {
                LOGGER.warn(
                        "Lỗi scheduler phiên {}: {}",
                        auction.getId(),
                        exception.getMessage());
            }
        }
    }
}
