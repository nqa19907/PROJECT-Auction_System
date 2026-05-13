package auction_system.common.patterns.factory;

import java.util.Map;

import auction_system.common.models.items.Item;

/**
 * Giao diện định nghĩa phương thức Factory để tạo ra các loại sản phẩm đấu giá (Item).
 */
public interface ItemCreator {
    Item createItem(Map<String, Object> properties);
}
