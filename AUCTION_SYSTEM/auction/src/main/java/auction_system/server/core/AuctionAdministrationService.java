package auction_system.server.core;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Map;
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
        auction.notifyObservers(buildAuctionCanceledMessage(auctionId));
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

    private String buildAuctionCanceledMessage(final String auctionId) {
        // Gửi thông báo hủy phiên bằng JSON AUCTION_ENDED, fallback về string cũ nếu lỗi.
        try {
            return JsonProtocol.stringify(new JsonMessage(
                    Protocol.Response.AUCTION_ENDED.name(),
                    null,
                    "OK",
                    JsonProtocol.payloadOf(Map.of(
                            "auctionId", String.valueOf(auctionId),
                            "winnerUsername", "NONE",
                            "itemName", "")),
                    null));
        } catch (JsonProcessingException exception) {
            logger.warn("Không tạo được JSON AUCTION_ENDED khi hủy phiên: {}",
                    exception.getMessage());
            return Protocol.Response.AUCTION_ENDED.name()
                    + Protocol.SEPARATOR + auctionId
                    + Protocol.SEPARATOR + "NONE";
        }
    }
}
