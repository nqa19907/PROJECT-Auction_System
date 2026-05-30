import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import auction_system.common.models.items.Item;
import auction_system.common.models.items.factory.ArtCreator;
import auction_system.common.models.items.factory.ElectronicCreator;
import auction_system.common.models.items.factory.VehicleCreator;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử ba Factory Creator: {@link ElectronicCreator}, {@link ArtCreator},
 * {@link VehicleCreator}.
 *
 * <p>Hai nhóm cho mỗi creator:
 * <ol>
 *   <li>Properties đầy đủ — dữ liệu đúng phải được ánh xạ vào Item.</li>
 *   <li>Properties rỗng — mỗi creator phải trả về Item với giá trị mặc định.</li>
 * </ol>
 */
public class ItemFactoryTest {

    // =========================================================================
    // Helper
    // =========================================================================

    /**
     * Tạo map properties đầy đủ với các trường cơ bản.
     *
     * @param name       tên sản phẩm
     * @param price      giá khởi điểm
     * @param sellerId   id người bán
     * @return map properties sẵn sàng truyền vào creator
     */
    private Map<String, Object> buildProps(
            final String name,
            final double price,
            final String sellerId) {
        Map<String, Object> props = new HashMap<>();
        props.put("itemName", name);
        props.put("description", "Mo ta san pham");
        props.put("startPrice", price);
        props.put("sellerId", sellerId);
        return props;
    }

    // =========================================================================
    // ElectronicCreator
    // =========================================================================

    @Test
    void electronicCreator_FullProperties_ReturnsItemWithCorrectName() {
        Item item = new ElectronicCreator()
                .createItem(buildProps("iPhone 15", 2000.0, "seller01"));

        assertEquals("iPhone 15", item.getItemName());
    }

    @Test
    void electronicCreator_FullProperties_ReturnsItemWithCorrectPrice() {
        Item item = new ElectronicCreator()
                .createItem(buildProps("MacBook", 5000.0, "seller01"));

        assertEquals(5000.0, item.getStartPrice(), 0.001);
    }

    @Test
    void electronicCreator_FullProperties_ReturnsItemWithCorrectSellerId() {
        Item item = new ElectronicCreator()
                .createItem(buildProps("iPad", 1500.0, "sellerXYZ"));

        assertEquals("sellerXYZ", item.getSellerId());
    }

    @Test
    void electronicCreator_FullProperties_ReturnsItemWithElectronicCategory() {
        Item item = new ElectronicCreator()
                .createItem(buildProps("TV", 800.0, "seller01"));

        assertEquals("ELECTRONIC", item.getCategory());
    }

    @Test
    void electronicCreator_EmptyProperties_ReturnsItemWithDefaultName() {
        Item item = new ElectronicCreator().createItem(new HashMap<>());

        assertNotNull(item);
        assertEquals("New Electronic Item", item.getItemName(),
                "Ten mac dinh phai la 'New Electronic Item' khi khong truyen properties.");
    }

    @Test
    void electronicCreator_EmptyProperties_ReturnsItemWithZeroStartPrice() {
        Item item = new ElectronicCreator().createItem(new HashMap<>());

        assertEquals(0.0, item.getStartPrice(), 0.001);
    }

    @Test
    void electronicCreator_EmptyProperties_ReturnsItemWithSystemSellerId() {
        Item item = new ElectronicCreator().createItem(new HashMap<>());

        assertEquals("SYSTEM", item.getSellerId());
    }

    // =========================================================================
    // ArtCreator
    // =========================================================================

    @Test
    void artCreator_FullProperties_ReturnsItemWithCorrectName() {
        Item item = new ArtCreator()
                .createItem(buildProps("Buc tranh Son Dong", 3000.0, "seller02"));

        assertEquals("Buc tranh Son Dong", item.getItemName());
    }

    @Test
    void artCreator_FullProperties_ReturnsItemWithCorrectPrice() {
        Item item = new ArtCreator()
                .createItem(buildProps("Tuong da", 7500.0, "seller02"));

        assertEquals(7500.0, item.getStartPrice(), 0.001);
    }

    @Test
    void artCreator_FullProperties_ReturnsItemWithCorrectSellerId() {
        Item item = new ArtCreator()
                .createItem(buildProps("Tranh son dau", 4000.0, "artSellerABC"));

        assertEquals("artSellerABC", item.getSellerId());
    }

    @Test
    void artCreator_FullProperties_ReturnsItemWithArtCategory() {
        Item item = new ArtCreator()
                .createItem(buildProps("Gom su", 2500.0, "seller02"));

        assertEquals("ART", item.getCategory());
    }

    @Test
    void artCreator_EmptyProperties_ReturnsItemWithDefaultName() {
        Item item = new ArtCreator().createItem(new HashMap<>());

        assertNotNull(item);
        assertEquals("New Art Piece", item.getItemName(),
                "Ten mac dinh phai la 'New Art Piece' khi khong truyen properties.");
    }

    @Test
    void artCreator_EmptyProperties_ReturnsItemWithZeroStartPrice() {
        Item item = new ArtCreator().createItem(new HashMap<>());

        assertEquals(0.0, item.getStartPrice(), 0.001);
    }

    @Test
    void artCreator_EmptyProperties_ReturnsItemWithSystemSellerId() {
        Item item = new ArtCreator().createItem(new HashMap<>());

        assertEquals("SYSTEM", item.getSellerId());
    }

    // =========================================================================
    // VehicleCreator
    // =========================================================================

    @Test
    void vehicleCreator_FullProperties_ReturnsItemWithCorrectName() {
        Item item = new VehicleCreator()
                .createItem(buildProps("Toyota Camry 2023", 45000.0, "seller03"));

        assertEquals("Toyota Camry 2023", item.getItemName());
    }

    @Test
    void vehicleCreator_FullProperties_ReturnsItemWithCorrectPrice() {
        Item item = new VehicleCreator()
                .createItem(buildProps("Honda Civic", 28000.0, "seller03"));

        assertEquals(28000.0, item.getStartPrice(), 0.001);
    }

    @Test
    void vehicleCreator_FullProperties_ReturnsItemWithCorrectSellerId() {
        Item item = new VehicleCreator()
                .createItem(buildProps("BMW X5", 85000.0, "vehicleSeller99"));

        assertEquals("vehicleSeller99", item.getSellerId());
    }

    @Test
    void vehicleCreator_FullProperties_ReturnsItemWithVehicleCategory() {
        Item item = new VehicleCreator()
                .createItem(buildProps("Xe may Honda", 3000.0, "seller03"));

        assertEquals("VEHICLE", item.getCategory());
    }

    @Test
    void vehicleCreator_EmptyProperties_ReturnsItemWithDefaultName() {
        Item item = new VehicleCreator().createItem(new HashMap<>());

        assertNotNull(item);
        assertEquals("New Vehicle", item.getItemName(),
                "Ten mac dinh phai la 'New Vehicle' khi khong truyen properties.");
    }

    @Test
    void vehicleCreator_EmptyProperties_ReturnsItemWithZeroStartPrice() {
        Item item = new VehicleCreator().createItem(new HashMap<>());

        assertEquals(0.0, item.getStartPrice(), 0.001);
    }

    @Test
    void vehicleCreator_EmptyProperties_ReturnsItemWithSystemSellerId() {
        Item item = new VehicleCreator().createItem(new HashMap<>());

        assertEquals("SYSTEM", item.getSellerId());
    }

}