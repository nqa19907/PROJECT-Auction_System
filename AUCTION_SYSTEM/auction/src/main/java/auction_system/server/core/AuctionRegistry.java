package auction_system.server.core;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.items.Item;
import auction_system.common.models.users.Participant;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime registry cho các phiên đấu giá.
 *
 * <p>Lớp này chỉ giữ collection runtime và đồng bộ thay đổi cơ bản xuống
 * persistence. Các nghiệp vụ bid, auth hoặc socket không nằm ở đây để tránh
 * kéo AuctionManager trở lại thành một God object.
 */
final class AuctionRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionRegistry.class);

    private final SerializedDatabase database;
    private final List<Auction> auctions = new CopyOnWriteArrayList<>();

    AuctionRegistry(final SerializedDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    void loadFromPersistence() {
        auctions.clear();
        auctions.addAll(database.auctions().findAll());
    }

    boolean isEmpty() {
        return auctions.isEmpty();
    }

    Auction createAuction(
            final Item item,
            final Participant seller,
            final LocalDateTime startTime,
            final LocalDateTime endTime) {

        final Auction newAuction = new Auction(item, seller, startTime, endTime);
        auctions.add(newAuction);
        database.items().save(item);
        database.auctions().save(newAuction);

        LOGGER.info(
                "Phiên đấu giá mới: {} | Item: {}",
                newAuction.getId(),
                item.getItemName());

        return newAuction;
    }

    Auction findById(final String auctionId) {
        if (auctionId == null) {
            return null;
        }

        return auctions.stream()
                .filter(auction -> auctionId.equals(auction.getId()))
                .findFirst()
                .orElse(null);
    }

    List<Auction> findAll() {
        return Collections.unmodifiableList(auctions);
    }

    boolean cancelById(final String auctionId) {
        final Auction auction = findById(auctionId);
        if (auction == null) {
            return false;
        }

        auction.setStatus(AuctionStatus.CANCELED);
        auction.notifyObservers("AUCTION_ENDED|" + auctionId + "|NONE");
        LOGGER.info("Huỷ phiên đấu giá: {}", auctionId);
        return true;
    }

    boolean deleteById(final String auctionId) {
        final Auction auction = findById(auctionId);
        if (auction == null) {
            return false;
        }

        auctions.remove(auction);
        database.auctions().deleteById(auctionId);
        LOGGER.info("Xóa phiên đấu giá: {}", auctionId);
        return true;
    }
}
