package auction_system.server.core;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.items.Item;
import auction_system.common.models.users.Participant;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry runtime cho các phiên đấu giá.
 */
final class AuctionRegistry {

    private final SerializedDatabase database;
    private final List<Auction> auctionList;

    AuctionRegistry(final SerializedDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
        this.auctionList = new CopyOnWriteArrayList<>();
    }

    void loadFromDatabase() {
        auctionList.addAll(database.auctions().findAll());
    }

    boolean isEmpty() {
        return auctionList.isEmpty();
    }

    Auction createAuction(
            final Item item,
            final Participant seller,
            final LocalDateTime startTime,
            final LocalDateTime endTime,
            final boolean antiSnipingEnabled) {
        final Auction newAuction = new Auction(item, seller, startTime, endTime);
        newAuction.setAntiSnipingEnabled(antiSnipingEnabled);

        // Lưu phiên mới vào cả bộ nhớ runtime và database để server/client cùng thấy.
        auctionList.add(newAuction);
        database.items().save(item);
        database.auctions().save(newAuction);
        database.flushAll();

        return newAuction;
    }

    Auction findById(final String auctionId) {
        return auctionList.stream()
                .filter(a -> a.getId().equals(auctionId))
                .findFirst()
                .orElse(null);
    }

    List<Auction> getAll() {
        return Collections.unmodifiableList(auctionList);
    }

    boolean delete(final Auction auction) {
        auctionList.remove(auction);
        return database.auctions().deleteById(auction.getId());
    }
}
