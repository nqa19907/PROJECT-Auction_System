package auction_system.server.persistence;

import auction_system.common.models.auctions.Auction;
import java.nio.file.Path;
import java.util.List;

/**
 * Repository quản lý dữ liệu phiên đấu giá.
 *
 * <p>Các service đấu giá sẽ dùng repository này để tạo phiên, cập nhật giá hiện
 * tại, cập nhật người dẫn đầu và chuyển trạng thái phiên đấu giá.
 */
public class AuctionRepository extends SerializedRepository<Auction> {
  /**
   * Khởi tạo repository phiên đấu giá.
   *
   * @param filePath đường dẫn file auctions.ser
   */
  public AuctionRepository(final Path filePath) {
    super(new SerializedFileStorage<>(filePath), Auction::getId);
  }

  /**
   * Tìm phiên đấu giá theo sản phẩm.
   *
   * @param itemId mã định danh sản phẩm
   * @return danh sách phiên đấu giá của sản phẩm
   */
  public List<Auction> findByItemId(final String itemId) {
    if (itemId == null || itemId.isBlank()) {
      return List.of();
    }

    return findAll().stream()
        .filter(auction -> itemId.equals(auction.getId()))
        .toList();
  }

  /**
   * Tìm phiên đấu giá theo người bán.
   *
   * @param sellerId mã định danh người bán
   * @return danh sách phiên đấu giá của người bán
   */
  public List<Auction> findBySellerId(final String sellerId) {
    if (sellerId == null || sellerId.isBlank()) {
      return List.of();
    }

    return findAll().stream()
        .filter(auction -> sellerId.equals(auction.getSellerId()))
        .toList();
  }
}