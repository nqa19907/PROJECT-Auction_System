package auction_system.server.persistence;

import auction_system.common.models.items.Item;
import java.nio.file.Path;
import java.util.List;

/**
 * Repository quản lý dữ liệu sản phẩm đấu giá.
 *
 * <p>Repository này lưu được Art, Electronic, Vehicle và các lớp con khác của
 * Item. Không cần tạo ArtRepository, ElectronicRepository hoặc VehicleRepository
 * nếu cách lưu trữ của chúng giống nhau.
 */
public class ItemRepository extends SerializedRepository<Item> {
  /**
   * Khởi tạo repository sản phẩm.
   *
   * @param filePath đường dẫn file items.ser
   */
  public ItemRepository(final Path filePath) {
    super(new SerializedFileStorage<>(filePath), Item::getId);
  }

  /**
   * Tìm sản phẩm theo người bán.
   *
   * @param sellerId mã định danh của người bán
   * @return danh sách sản phẩm của người bán
   */
  public List<Item> findBySellerId(final String sellerId) {
    if (sellerId == null || sellerId.isBlank()) {
      return List.of();
    }

    return findAll().stream()
        .filter(item -> sellerId.equals(item.getSellerId()))
        .toList();
  }

  /**
   * Tìm sản phẩm theo danh mục.
   *
   * @param category danh mục sản phẩm
   * @return danh sách sản phẩm thuộc danh mục
   */
  public List<Item> findByCategory(final String category) {
    if (category == null || category.isBlank()) {
      return List.of();
    }

    return findAll().stream()
        .filter(item -> category.equalsIgnoreCase(item.getCategory()))
        .toList();
  }
}