package auction_system.common.models.items.factory;

import auction_system.common.models.items.Item;
import java.util.Map;

/**
 * Giao diện định nghĩa phương thức Factory để tạo ra các loại sản phẩm đấu giá (Item).
 */
public interface ItemCreator {
    String PROPERTY_ITEM_NAME = "itemName";
    String PROPERTY_DESCRIPTION = "description";
    String PROPERTY_START_PRICE = "startPrice";
    String PROPERTY_SELLER_ID = "sellerId";
    String PROPERTY_IMAGE_PATH = "imagePath";

    String DEFAULT_DESCRIPTION = "Description here";
    String DEFAULT_SELLER_ID = "SYSTEM";
    String DEFAULT_IMAGE_PATH = "";
    String DEFAULT_ART_ITEM_NAME = "New Art Piece";
    String DEFAULT_ELECTRONIC_ITEM_NAME = "New Electronic Item";
    String DEFAULT_VEHICLE_ITEM_NAME = "New Vehicle";

    Item createItem(Map<String, Object> properties);
}
