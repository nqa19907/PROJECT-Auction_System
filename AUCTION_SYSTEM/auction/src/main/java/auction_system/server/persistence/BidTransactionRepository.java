package auction_system.server.persistence;

import auction_system.common.models.auctions.BidTransaction;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Repository quản lý lịch sử giao dịch đặt giá.
 *
 * <p>Mỗi lần đặt giá hợp lệ nên tạo một BidTransaction và lưu vào repository
 * này. Client có thể dùng dữ liệu này để hiển thị lịch sử bid hoặc biểu đồ giá
 * realtime.
 */
public class BidTransactionRepository
    extends SerializedRepository<BidTransaction> {

    /**
     * Khởi tạo repository giao dịch đặt giá.
     *
     * @param filePath đường dẫn file bid_transactions.ser
     */
    public BidTransactionRepository(final Path filePath) {
        super(new SerializedFileStorage<>(filePath),
            BidTransaction::getId);
    }

    /**
     * Tìm lịch sử đặt giá theo phiên đấu giá.
     *
     * @param auctionId mã định danh phiên đấu giá
     * @return danh sách giao dịch đặt giá của phiên
     */
    public List<BidTransaction> findByAuctionId(
        final String auctionId
    ) {
        if (auctionId == null || auctionId.isBlank()) {
            return List.of();
        }

        return findAll().stream()
            .filter(transaction ->
                auctionId.equals(transaction.getId()))
            .sorted(
                Comparator.comparing(
                    BidTransaction::getTimestamp
                )
            )
            .toList();
    }

    /**
     * Tìm lịch sử đặt giá theo người đặt.
     *
     * @param bidderId mã định danh người đặt giá
     * @return danh sách giao dịch đặt giá của người dùng
     */
    public List<BidTransaction> findByBidderId(
        final String bidderId
    ) {
        if (bidderId == null || bidderId.isBlank()) {
            return List.of();
        }

        return findAll().stream()
            .filter(transaction ->
                bidderId.equals(transaction.getId()))
            .sorted(
                Comparator.comparing(
                    BidTransaction::getTimestamp
                )
            )
            .toList();
    }
}