package auction_system.common.models.items.factory;

import auction_system.common.constants.AppConstants;
import auction_system.common.models.items.Item;
import java.util.Locale;
import java.util.Map;

/**
 * Factory chọn ItemCreator phù hợp theo danh mục sản phẩm.
 */
public final class ItemCreatorFactory {
    private static final String INVALID_CATEGORY_MESSAGE = "Danh mục sản phẩm không hợp lệ.";

    private static final Map<String, ItemCreator> CREATORS = Map.of(
            normalize(AppConstants.CATEGORY_ART), new ArtCreator(),
            normalize(AppConstants.CATEGORY_ELECTRONIC), new ElectronicCreator(),
            normalize(AppConstants.CATEGORY_VEHICLE), new VehicleCreator());

    private ItemCreatorFactory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Tạo Item theo danh mục và thông tin sản phẩm đầu vào.
     *
     * @param category danh mục sản phẩm
     * @param itemName tên sản phẩm
     * @param description mô tả sản phẩm
     * @param startPrice giá khởi điểm
     * @param sellerId mã người bán
     * @return item đã được tạo
     */
    public static Item createItem(
            String category,
            String itemName,
            String description,
            double startPrice,
            String sellerId) {
        ItemCreator creator = getCreator(category);
        Map<String, Object> properties = Map.of(
                ItemCreator.PROPERTY_ITEM_NAME, itemName,
                ItemCreator.PROPERTY_DESCRIPTION, description,
                ItemCreator.PROPERTY_START_PRICE, startPrice,
                ItemCreator.PROPERTY_SELLER_ID, sellerId);
        return creator.createItem(properties);
    }

    private static ItemCreator getCreator(String category) {
        if (category == null) {
            throw new IllegalArgumentException(INVALID_CATEGORY_MESSAGE);
        }

        ItemCreator creator = CREATORS.get(normalize(category));
        if (creator == null) {
            throw new IllegalArgumentException(INVALID_CATEGORY_MESSAGE);
        }
        return creator;
    }

    private static String normalize(String category) {
        return category.trim().toUpperCase(Locale.ROOT);
    }
}
