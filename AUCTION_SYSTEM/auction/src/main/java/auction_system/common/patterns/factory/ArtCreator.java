package auction_system.common.patterns.factory;

import auction_system.common.models.Item;
import auction_system.common.patterns.builder.ArtBuilder;
import java.util.Map;

/**
 * Lớp Factory chịu trách nhiệm khởi tạo các sản phẩm là tác phẩm nghệ thuật (Art).
 */
public class ArtCreator implements ItemCreator {
    @Override
    public Item createItem(Map<String, Object> properties) {
        return (new ArtBuilder())
                .itemName((String) properties.getOrDefault("itemName", "New Art Piece"))
                .description((String) properties.getOrDefault("description", "Description here"))
                .startPrice(((Number) properties.getOrDefault("startPrice", 0.0)).doubleValue())
                .sellerId((String) properties.getOrDefault("sellerId", "SYSTEM"))
                .condition((String) properties.getOrDefault("condition", "Excellent"))
                .imagePath((String) properties.getOrDefault("imagePath", "none"))
                .artistName((String) properties.getOrDefault("artistName", "Unknown Artist"))
                .creationYear((String) properties.getOrDefault("creationYear", "Unknown Year"))
                .hasAuthenticityCertificate((Boolean) properties.getOrDefault(
                        "hasAuthenticityCertificate", false))
                .build();
    }
}
