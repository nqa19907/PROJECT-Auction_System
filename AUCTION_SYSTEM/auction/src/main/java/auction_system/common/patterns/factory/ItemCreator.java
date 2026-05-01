package auction_system.common.patterns.factory;
import auction_system.common.models.Item;
import java.util.Map;

/**
 * Giao diện định nghĩa phương thức Factory để tạo ra các loại sản phẩm đấu giá (Item).
 */
public interface ItemCreator {
    Item createItem(Map<String, Object> properties);
}
