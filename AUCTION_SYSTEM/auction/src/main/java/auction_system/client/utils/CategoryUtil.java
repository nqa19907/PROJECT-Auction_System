package auction_system.client.utils;

import auction_system.common.constants.AppConstants;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiện ích quản lý ánh xạ (mapping) liên quan đến Danh mục (Category).
 * Tập trung cấu hình tại một nơi, tránh lặp lại code ở các Controller.
 */
public class CategoryUtil {
    private static final Map<String, String> ID_TO_CATEGORY_MAP = new HashMap<>();
    private static final Map<String, String> CATEGORY_TO_TITLE_MAP = new HashMap<>();

    static {
        // Ánh xạ từ ID của UI sang Category Code
        ID_TO_CATEGORY_MAP.put(AppConstants.UI_ID_CATEGORY_ART, AppConstants.CATEGORY_ART);
        ID_TO_CATEGORY_MAP.put(AppConstants.UI_ID_CATEGORY_ELECTRONIC,
                                    AppConstants.CATEGORY_ELECTRONIC);
        ID_TO_CATEGORY_MAP.put(AppConstants.UI_ID_CATEGORY_VEHICLE, AppConstants.CATEGORY_VEHICLE);

        // Ánh xạ từ Category Code sang Tiêu đề hiển thị
        CATEGORY_TO_TITLE_MAP.put(AppConstants.CATEGORY_ART, AppConstants.TITLE_ART);
        CATEGORY_TO_TITLE_MAP.put(AppConstants.CATEGORY_ELECTRONIC, AppConstants.TITLE_ELECTRONIC);
        CATEGORY_TO_TITLE_MAP.put(AppConstants.CATEGORY_VEHICLE, AppConstants.TITLE_VEHICLE);
    }

    public static String getCategoryByUiId(String uiId) {
        return ID_TO_CATEGORY_MAP.getOrDefault(uiId, AppConstants.CATEGORY_ALL);
    }

    public static String getTitleByCategory(String category) {
        return CATEGORY_TO_TITLE_MAP.getOrDefault(category, AppConstants.TITLE_ALL);
    }
}