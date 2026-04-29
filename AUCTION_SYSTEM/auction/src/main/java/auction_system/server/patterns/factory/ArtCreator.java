package auction_system.server.patterns.factory;

import auction_system.common.models.Art;
import auction_system.common.models.Item;
import java.util.Map;

/**
 * Lớp Factory chịu trách nhiệm khởi tạo các sản phẩm là tác phẩm nghệ thuật (Art).
 */
public class ArtCreator implements ItemCreator {
    @Override
    public Item createItem(Map<String, Object> properties) {
        return new Art(
                (String) properties.getOrDefault("itemName", "New Art Piece"),
                (String) properties.getOrDefault("description", "Description here"),
                ((Number) properties.getOrDefault("startPrice", 0.0)).doubleValue(),
                (String) properties.getOrDefault("sellerId", "SYSTEM"),
                (String) properties.getOrDefault("condition", "Excellent"),
                (String) properties.getOrDefault("imagePath", "none"),
                (String) properties.getOrDefault("artistName", "Unknown Artist"),
                (String) properties.getOrDefault("creationYear", "Unknown Year"),
                (Boolean) properties.getOrDefault("hasAuthenticityCertificate", false)
        );
    }
}
