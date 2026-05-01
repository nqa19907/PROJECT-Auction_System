package auction_system.common.patterns.factory;

import auction_system.common.models.Electronic;
import auction_system.common.models.Item;
import java.util.Map;

/**
 * Lớp Factory chịu trách nhiệm khởi tạo các sản phẩm là thiết bị điện tử (Electronic).
 */
public class ElectronicCreator implements ItemCreator {
    @Override
    public Item createItem(Map<String, Object> properties) {
        return new Electronic(
                (String) properties.getOrDefault("itemName", "New Electronic Item"),
                (String) properties.getOrDefault("description", "Description here"),
                ((Number) properties.getOrDefault("startPrice", 0.0)).doubleValue(),
                (String) properties.getOrDefault("sellerId", "SYSTEM"),
                (String) properties.getOrDefault("condition", "New"),
                (String) properties.getOrDefault("imagePath", "none"),
                (String) properties.getOrDefault("brand", "Generic Brand"),
                ((Number) properties.getOrDefault("warrantyMonths", 0)).intValue()
        );
    }
}