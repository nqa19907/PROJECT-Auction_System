package auction_system.common.patterns.factory;

import auction_system.common.models.Item;
import auction_system.common.patterns.builder.VehicleBuilder;
import java.util.Map;

/**
 * Lớp Factory chịu trách nhiệm khởi tạo các sản phẩm là phương tiện giao thông (Vehicle).
 */
public class VehicleCreator implements ItemCreator {
    @Override
    public Item createItem(Map<String, Object> properties) {
        return new VehicleBuilder()
            .itemName((String) properties.getOrDefault("itemName", "New Vehicle"))
            .description((String) properties.getOrDefault("description", "Description here"))
            .startPrice(((Number) properties.getOrDefault("startPrice", 0.0)).doubleValue())
            .sellerId((String) properties.getOrDefault("sellerId", "SYSTEM"))
            .condition((String) properties.getOrDefault("condition", "Used"))
            .imagePath((String) properties.getOrDefault("imagePath", "none"))
            .make((String) properties.getOrDefault("make", "Generic Make"))
            .model((String) properties.getOrDefault("model", "Generic Model"))
            .manufacturingYear(((Number) properties
                    .getOrDefault("manufacturingYear", 0)).intValue())
            .mileage(((Number) properties.getOrDefault("mileage", 0.0)).doubleValue())
            .build();
    }
}
