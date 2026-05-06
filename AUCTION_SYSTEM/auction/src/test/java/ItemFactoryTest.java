import auction_system.common.models.Item;
import auction_system.common.patterns.factory.ElectronicCreator;
import auction_system.common.patterns.factory.ArtCreator;
import auction_system.common.patterns.factory.VehicleCreator;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class ItemFactoryTest {

    @Test
    void testElectronicCreator_CreatesItemWithCorrectName() {
        // Arrange
        ElectronicCreator creator = new ElectronicCreator();
        Map<String, Object> properties = new HashMap<>();
        properties.put("itemName", "iPhone 15");
        properties.put("startPrice", 2000.0);
        properties.put("brand", "Apple");
        properties.put("warrantyMonths", 12);

        // Act
        Item item = creator.createItem(properties);

        // Assert
        assertEquals("iPhone 15", item.getItemName(),
                "Tên sản phẩm phải đúng với giá trị đã truyền vào");
    }

    @Test
    void testElectronicCreator_CreatesItemWithCorrectPrice() {
        // Arrange
        ElectronicCreator creator = new ElectronicCreator();
        Map<String, Object> properties = new HashMap<>();
        properties.put("startPrice", 5000.0);

        // Act
        Item item = creator.createItem(properties);

        // Assert
        assertEquals(5000.0, item.getStartPrice(),
                "Giá khởi điểm phải đúng với giá trị đã truyền vào");
    }

    @Test
    void testElectronicCreator_DefaultValues_WhenNoProperties() {
        // Arrange
        ElectronicCreator creator = new ElectronicCreator();
        Map<String, Object> properties = new HashMap<>();

        // Act
        Item item = creator.createItem(properties);

        // Assert
        assertNotNull(item, "Item không được null dù không truyền properties");
        assertEquals("New Electronic Item", item.getItemName(),
                "Tên mặc định phải là 'New Electronic Item'");
    }
}