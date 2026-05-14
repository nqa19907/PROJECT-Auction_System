package auction_system.common.models.items.factory;

import auction_system.common.models.items.Item;
import java.util.Map;

/**
 * Giao diện định nghĩa phương thức Factory để tạo ra các loại sản phẩm đấu giá (Item).
 */
public interface ItemCreator {
    Item createItem(Map<String, Object> properties);
}
