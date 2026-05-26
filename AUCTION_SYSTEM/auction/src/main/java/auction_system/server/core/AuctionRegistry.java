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
        /*
         * Registry runtime luôn được rebuild từ repository khi server start.
         * CopyOnWriteArrayList cho phép scheduler đọc trong lúc command khác duyệt.
         */
        auctions.clear();
        auctions.addAll(database.auctions().findAll());
    }

    boolean isEmpty() {
        // Dùng cho luồng seed data mẫu sau khi nạp persistence.
        return auctions.isEmpty();
    }

    Auction createAuction(
            final Item item,
            final Participant seller,
            final LocalDateTime startTime,
            final LocalDateTime endTime) {

        /*
         * Tạo auction mới cần lưu cả item và auction để sau khi restart server
         * vẫn khôi phục được quan hệ item/phiên trong database serialization.
         */
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
        /*
         * Mỗi lần lấy một phiên cụ thể đều refresh lifecycle trước khi trả ra,
         * nhờ vậy command đặt giá hoặc xem chi tiết không dùng trạng thái cũ.
         */
        if (auctionId == null) {
            return null;
        }

        final Auction auction = auctions.stream()
                .filter(candidate -> auctionId.equals(candidate.getId()))
                .findFirst()
                .orElse(null);
        refreshAuctionLifecycle(auction);
        return auction;
    }

    List<Auction> findAll() {
        // Làm mới lifecycle trước khi trả dữ liệu để LIST_AUCTIONS không hiển thị trạng thái cũ.
        refreshAllAuctionLifecycles();
        return Collections.unmodifiableList(auctions);
    }

    void refreshAllAuctionLifecycles() {
        /*
         * Scheduler và các command list cùng dùng hàm này. Mỗi auction tự quyết
         * định start/end theo thời gian hiện tại trong model Auction.
         */
        for (final Auction auction : auctions) {
            refreshAuctionLifecycle(auction);
        }
    }

    void refreshAuctionLifecycle(final Auction auction) {
        /*
         * startAuction/endAuction là idempotent theo trạng thái hiện tại, nên có
         * thể gọi nhiều lần từ scheduler, list và place bid mà không đổi sai.
         */
        if (auction == null) {
            return;
        }

        final AuctionStatus oldStatus = auction.getStatus();
        auction.startAuction();
        auction.endAuction();

        // Chỉ ghi disk khi status thật sự đổi để tránh flush liên tục mỗi vòng scheduler.
        if (oldStatus != auction.getStatus()) {
            database.auctions().save(auction);
            database.flushAll();
        }
    }

    boolean cancelById(final String auctionId) {
        /*
         * Cancel giữ auction trong registry/database nhưng chuyển trạng thái.
         * Observer được báo để client đang xem phiên biết phiên đã đóng.
         */
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
        /*
         * Delete loại hẳn phiên khỏi registry runtime và repository. Command admin
         * dùng kết quả boolean để quyết định trả OK/FAIL cho client.
         */
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
