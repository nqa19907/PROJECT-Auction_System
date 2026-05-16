package auction_system.common.models.items.factory;

import auction_system.common.models.items.Item;
import auction_system.common.models.items.builder.ElectronicBuilder;
import java.util.Map;

/**
 * Lớp Factory chịu trách nhiệm khởi tạo các sản phẩm là thiết bị điện tử (Electronic).
 */
public class ElectronicCreator implements ItemCreator {
    @Override
    public Item createItem(Map<String, Object> properties) {
        return new ElectronicBuilder()
                .itemName((String) properties.getOrDefault("itemName", "New Electronic Item"))
                .description((String) properties.getOrDefault("description", "Description here"))
                .startPrice(((Number) properties.getOrDefault("startPrice", 0.0)).doubleValue())
                .sellerId((String) properties.getOrDefault("sellerId", "SYSTEM"))
                .build();
    }
}
