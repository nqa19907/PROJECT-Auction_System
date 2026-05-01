package auction_system.common.patterns.factory;

import auction_system.common.models.Item;
import auction_system.common.models.Vehicle;
import java.util.Map;

/**
 * Lớp Factory chịu trách nhiệm khởi tạo các sản phẩm là phương tiện giao thông (Vehicle).
 */
public class VehicleCreator implements ItemCreator {
    @Override
    public Item createItem(Map<String, Object> properties) {
        return new Vehicle(
                (String) properties.getOrDefault("itemName", "New Vehicle"),
                (String) properties.getOrDefault("description", "Description here"),
                ((Number) properties.getOrDefault("startPrice", 0.0)).doubleValue(),
                (String) properties.getOrDefault("sellerId", "SYSTEM"),
                (String) properties.getOrDefault("condition", "Used"),
                (String) properties.getOrDefault("imagePath", "none"),
                (String) properties.getOrDefault("make", "Generic Make"),
                (String) properties.getOrDefault("model", "Generic Model"),
                ((Number) properties.getOrDefault("manufacturingYear", 0)).intValue(),
                ((Number) properties.getOrDefault("mileage", 0.0)).doubleValue()
        );
    }
}