package auction_system.common.patterns.factory;

import auction_system.common.models.Item;
import auction_system.common.patterns.builder.ElectronicBuilder;
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
                .condition((String) properties.getOrDefault("condition", "New"))
                .imagePath((String) properties.getOrDefault("imagePath", "none"))
                .brand((String) properties.getOrDefault("brand", "Generic Brand"))
                .warrantyMonths(((Number) properties.getOrDefault("warrantyMonths", 0)).intValue())
                .build();
    }
}
