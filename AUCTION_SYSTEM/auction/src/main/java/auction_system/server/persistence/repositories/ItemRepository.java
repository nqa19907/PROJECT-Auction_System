package auction_system.server.persistence.repositories;

import auction_system.common.models.items.Item;
import auction_system.server.persistence.exceptions.DatabaseException;
import auction_system.server.persistence.serialization.SerializedFileStorage;
import auction_system.server.persistence.serialization.SerializedRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Repository quản lý dữ liệu sản phẩm đấu giá trong hệ thống.
 *
 * <p>Lớp này lưu toàn bộ sản phẩm vào file {@code items.ser} thông qua Java
 * Serialization. Các lớp con của {@link Item} như Art, Electronic và Vehicle
 * được lưu chung thông qua cơ chế đa hình.
 *
 * <p>Repository này chỉ xử lý dữ liệu bền vững của sản phẩm. Các nghiệp vụ như
 * mở phiên đấu giá, đặt giá, kết thúc phiên đấu giá nên được xử lý ở tầng
 * service hoặc AuctionManager.
 */
public class ItemRepository extends SerializedRepository<Item> {

    /**
     * Khởi tạo repository sản phẩm với đường dẫn file lưu trữ.
     *
     * @param storagePath đường dẫn tới file items.ser
     */
    public ItemRepository(final Path storagePath) {
        super(new SerializedFileStorage<>(storagePath), Item::getId);
    }

    /**
     * Lưu mới hoặc cập nhật thông tin sản phẩm.
     *
     * <p>Trước khi lưu, repository kiểm tra dữ liệu cơ bản của sản phẩm như mã
     * định danh, tên sản phẩm, người bán, giá khởi điểm và giá hiện tại.
     *
     * @param item sản phẩm cần lưu
     * @return sản phẩm đã được lưu
     */
    @Override
    public synchronized Item save(final Item item) {
        Objects.requireNonNull(item, "item");

        validateItem(item);

        return super.save(item);
    }

    /**
     * Tìm danh sách sản phẩm theo mã người bán.
     *
     * @param sellerId mã định danh của người bán
     * @return danh sách sản phẩm thuộc người bán đó
     */
    public List<Item> findBySellerId(final String sellerId) {
        validateText(sellerId, "Mã người bán không được rỗng.");

        return findAll().stream()
            .filter(item -> sellerId.equals(item.getSellerId()))
            .toList();
    }

    /**
     * Tìm danh sách sản phẩm theo loại sản phẩm.
     *
     * <p>Các loại hiện tại có thể là ART, ELECTRONIC hoặc VEHICLE tùy theo lớp
     * con của {@link Item}.
     *
     * @param category loại sản phẩm cần tìm
     * @return danh sách sản phẩm thuộc loại tương ứng
     */
    public List<Item> findByCategory(final String category) {
        validateText(category, "Loại sản phẩm không được rỗng.");

        return findAll().stream()
            .filter(item -> category.equalsIgnoreCase(item.getCategory()))
            .toList();
    }

    /**
     * Tìm sản phẩm theo tên chính xác.
     *
     * @param itemName tên sản phẩm cần tìm
     * @return sản phẩm nếu tồn tại
     */
    public Optional<Item> findByItemName(final String itemName) {
        validateText(itemName, "Tên sản phẩm không được rỗng.");

        return findAll().stream()
            .filter(item -> itemName.equalsIgnoreCase(item.getItemName()))
            .findFirst();
    }

    /**
     * Tìm danh sách sản phẩm có giá hiện tại nằm trong khoảng cho trước.
     *
     * @param minPrice giá thấp nhất
     * @param maxPrice giá cao nhất
     * @return danh sách sản phẩm trong khoảng giá
     */
    public List<Item> findByCurrentPriceRange(
        final double minPrice,
        final double maxPrice) {
        validatePriceRange(minPrice, maxPrice);

        return findAll().stream()
            .filter(item -> item.getCurrentPrice() >= minPrice)
            .filter(item -> item.getCurrentPrice() <= maxPrice)
            .toList();
    }

    /**
     * Kiểm tra dữ liệu sản phẩm trước khi lưu.
     *
     * @param item sản phẩm cần kiểm tra
     */
    private void validateItem(final Item item) {
        validateText(item.getId(), "Mã sản phẩm không được rỗng.");
        validateText(item.getItemName(), "Tên sản phẩm không được rỗng.");
        validateText(item.getDescription(), "Mô tả sản phẩm không được rỗng.");
        validateText(item.getSellerId(), "Mã người bán không được rỗng.");
        validateText(item.getCategory(), "Loại sản phẩm không được rỗng.");
        validatePositivePrice(item.getStartPrice(), "Giá khởi điểm phải lớn hơn 0.");
        validateCurrentPrice(item);
    }

    /**
     * Kiểm tra giá hiện tại của sản phẩm.
     *
     * @param item sản phẩm cần kiểm tra
     */
    private void validateCurrentPrice(final Item item) {
        if (item.getCurrentPrice() < item.getStartPrice()) {
        throw new DatabaseException(
            "Giá hiện tại không được nhỏ hơn giá khởi điểm.");
        }
    }

    /**
     * Kiểm tra khoảng giá tìm kiếm.
     *
     * @param minPrice giá thấp nhất
     * @param maxPrice giá cao nhất
     */
    private void validatePriceRange(
        final double minPrice,
        final double maxPrice) {
        if (minPrice < 0 || maxPrice < 0) {
        throw new DatabaseException("Khoảng giá không được âm.");
        }

        if (minPrice > maxPrice) {
        throw new DatabaseException(
            "Giá thấp nhất không được lớn hơn giá cao nhất.");
        }
    }

    /**
     * Kiểm tra giá phải lớn hơn 0.
     *
     * @param price giá cần kiểm tra
     * @param message thông báo lỗi
     */
    private void validatePositivePrice(final double price, final String message) {
        if (price <= 0) {
        throw new DatabaseException(message);
        }
    }

    /**
     * Kiểm tra chuỗi không được null hoặc rỗng.
     *
     * @param value giá trị cần kiểm tra
     * @param message thông báo lỗi
     */
    private void validateText(final String value, final String message) {
        if (value == null || value.isBlank()) {
        throw new DatabaseException(message);
        }
    }
}