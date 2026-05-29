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
                .itemName((String) properties.getOrDefault(
                        PROPERTY_ITEM_NAME,
                        DEFAULT_ELECTRONIC_ITEM_NAME))
                .description((String) properties.getOrDefault(
                        PROPERTY_DESCRIPTION,
                        DEFAULT_DESCRIPTION))
                .startPrice(((Number) properties.getOrDefault(
                        PROPERTY_START_PRICE,
                        0.0)).doubleValue())
                .sellerId((String) properties.getOrDefault(PROPERTY_SELLER_ID, DEFAULT_SELLER_ID))
                .build();
    }
}
