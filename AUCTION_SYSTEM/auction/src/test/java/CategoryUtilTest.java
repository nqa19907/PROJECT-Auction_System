import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import auction_system.client.utils.CategoryUtil;
import auction_system.common.constants.AppConstants;

import org.junit.jupiter.api.Test;

/**
 * Kiểm thử lớp tiện ích {@link CategoryUtil}.
 *
 * <p>Các nhóm test:
 * <ol>
 *   <li>getCategoryByUiId — ánh xạ đúng ID sang category code.</li>
 *   <li>getCategoryByUiId — ID không tồn tại trả về mặc định "ALL".</li>
 *   <li>getTitleByCategory — ánh xạ đúng category code sang tiêu đề.</li>
 *   <li>getTitleByCategory — category không tồn tại trả về mặc định "Tất cả".</li>
 * </ol>
 */
public class CategoryUtilTest {

    // =========================================================================
    // getCategoryByUiId
    // =========================================================================

    /**
     * UI ID của art phải trả về category code "Art".
     */
    @Test
    void getCategoryByUiId_ArtId_ReturnsCategoryArt() {
        String result = CategoryUtil.getCategoryByUiId(AppConstants.UI_ID_CATEGORY_ART);

        assertEquals(AppConstants.CATEGORY_ART, result,
                "ID art phai anh xa sang CATEGORY_ART.");
    }

    /**
     * UI ID của electronic phải trả về category code "Electronic".
     */
    @Test
    void getCategoryByUiId_ElectronicId_ReturnsCategoryElectronic() {
        String result = CategoryUtil.getCategoryByUiId(AppConstants.UI_ID_CATEGORY_ELECTRONIC);

        assertEquals(AppConstants.CATEGORY_ELECTRONIC, result,
                "ID electronic phai anh xa sang CATEGORY_ELECTRONIC.");
    }

    /**
     * UI ID của vehicle phải trả về category code "Vehicle".
     */
    @Test
    void getCategoryByUiId_VehicleId_ReturnsCategoryVehicle() {
        String result = CategoryUtil.getCategoryByUiId(AppConstants.UI_ID_CATEGORY_VEHICLE);

        assertEquals(AppConstants.CATEGORY_VEHICLE, result,
                "ID vehicle phai anh xa sang CATEGORY_VEHICLE.");
    }

    /**
     * UI ID không tồn tại phải trả về mặc định "ALL".
     */
    @Test
    void getCategoryByUiId_UnknownId_ReturnsDefaultAll() {
        String result = CategoryUtil.getCategoryByUiId("unknownCategoryId");

        assertEquals(AppConstants.CATEGORY_ALL, result,
                "ID khong ton tai phai tra ve CATEGORY_ALL mac dinh.");
    }

    /**
     * UI ID null không được throw — phải trả về mặc định.
     */
    @Test
    void getCategoryByUiId_NullId_ReturnsDefault() {
        String result = CategoryUtil.getCategoryByUiId(null);

        assertNotNull(result, "Ket qua khong duoc null khi truyen null.");
        assertEquals(AppConstants.CATEGORY_ALL, result,
                "Null ID phai tra ve CATEGORY_ALL mac dinh.");
    }

    // =========================================================================
    // getTitleByCategory
    // =========================================================================

    /**
     * Category code "Art" phải trả về tiêu đề "Nghệ thuật".
     */
    @Test
    void getTitleByCategory_ArtCategory_ReturnsArtTitle() {
        String result = CategoryUtil.getTitleByCategory(AppConstants.CATEGORY_ART);

        assertEquals(AppConstants.TITLE_ART, result,
                "Category Art phai co tieu de la TITLE_ART.");
    }

    /**
     * Category code "Electronic" phải trả về tiêu đề "Đồ điện tử".
     */
    @Test
    void getTitleByCategory_ElectronicCategory_ReturnsElectronicTitle() {
        String result = CategoryUtil.getTitleByCategory(AppConstants.CATEGORY_ELECTRONIC);

        assertEquals(AppConstants.TITLE_ELECTRONIC, result,
                "Category Electronic phai co tieu de la TITLE_ELECTRONIC.");
    }

    /**
     * Category code "Vehicle" phải trả về tiêu đề "Phương tiện giao thông".
     */
    @Test
    void getTitleByCategory_VehicleCategory_ReturnsVehicleTitle() {
        String result = CategoryUtil.getTitleByCategory(AppConstants.CATEGORY_VEHICLE);

        assertEquals(AppConstants.TITLE_VEHICLE, result,
                "Category Vehicle phai co tieu de la TITLE_VEHICLE.");
    }

    /**
     * Category code không tồn tại phải trả về mặc định "Tất cả".
     */
    @Test
    void getTitleByCategory_UnknownCategory_ReturnsDefaultTitle() {
        String result = CategoryUtil.getTitleByCategory("UNKNOWN_CATEGORY");

        assertEquals(AppConstants.TITLE_ALL, result,
                "Category khong ton tai phai tra ve TITLE_ALL mac dinh.");
    }

    /**
     * Category code null không được throw — phải trả về mặc định.
     */
    @Test
    void getTitleByCategory_NullCategory_ReturnsDefault() {
        String result = CategoryUtil.getTitleByCategory(null);

        assertNotNull(result, "Ket qua khong duoc null khi truyen null.");
        assertEquals(AppConstants.TITLE_ALL, result,
                "Null category phai tra ve TITLE_ALL mac dinh.");
    }

    /**
     * Round-trip: lấy category từ UI ID rồi lấy title phải cho kết quả đúng.
     */
    @Test
    void getCategoryByUiId_ThenGetTitle_RoundTripMatchesExpected() {
        String category = CategoryUtil.getCategoryByUiId(AppConstants.UI_ID_CATEGORY_ART);
        String title = CategoryUtil.getTitleByCategory(category);

        assertEquals(AppConstants.TITLE_ART, title,
                "Round-trip tu UI ID sang title phai cho ket qua dung.");
    }
}