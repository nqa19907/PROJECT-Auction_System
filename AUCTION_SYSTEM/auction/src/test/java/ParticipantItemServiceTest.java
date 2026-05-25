import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.common.models.items.Art;
import auction_system.common.models.items.Item;
import auction_system.common.models.users.Participant;
import auction_system.server.persistence.serialization.SerializedDatabase;
import auction_system.server.services.ParticipantItemService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử việc đăng sản phẩm và ghi sản phẩm xuống database serialization.
 */
public class ParticipantItemServiceTest {
    private static final String PARTICIPANT_ROLE = "PARTICIPANT";

    @TempDir
    private Path tempDirectory;

    /**
     * Kiểm tra service có thể lưu item xuống repository và file items.ser.
     */
    @Test
    void testListItemForAuction_SavesItemToSerializedDatabase() {
        SerializedDatabase database = new SerializedDatabase(tempDirectory);
        ParticipantItemService service = new ParticipantItemService(database);
        Participant seller = new Participant(
                "seller01",
                "seller01@gmail.com",
                "hashed-password",
                10000.0,
                PARTICIPANT_ROLE);
        Item item = new Art(
                "Tranh sơn dầu",
                "Tranh phong cảnh còn mới.",
                1500.0,
                "");

        Item savedItem = service.listItemForAuction(seller, item);

        Optional<Item> actualItem = database.items().findById(savedItem.getId());

        assertTrue(actualItem.isPresent(), "Item phải tồn tại trong database.");
        assertEquals(seller.getId(), actualItem.get().getSellerId());
        assertEquals("Tranh sơn dầu", actualItem.get().getItemName());
        assertEquals("ART", actualItem.get().getCategory());
        assertEquals(1500.0, actualItem.get().getStartPrice());
        assertTrue(Files.exists(tempDirectory.resolve("items.ser")));
    }

    /**
     * Kiểm tra item vẫn còn tồn tại sau khi tạo database mới từ cùng thư mục.
     */
    @Test
    void testListItemForAuction_ItemStillExistsAfterReloadingDatabase() {
        SerializedDatabase database = new SerializedDatabase(tempDirectory);
        ParticipantItemService service = new ParticipantItemService(database);
        Participant seller = new Participant(
                "seller02",
                "seller02@gmail.com",
                "hashed-password",
                20000.0,
                PARTICIPANT_ROLE);
        Item item = new Art(
                "Tượng gỗ",
                "Tượng gỗ thủ công.",
                2500.0,
                "");

        Item savedItem = service.listItemForAuction(seller, item);

        SerializedDatabase reloadedDatabase = new SerializedDatabase(tempDirectory);
        Optional<Item> actualItem = reloadedDatabase.items().findById(savedItem.getId());

        assertTrue(actualItem.isPresent(), "Item phải đọc lại được từ file items.ser.");
        assertNotNull(actualItem.get().getId());
        assertEquals(savedItem.getId(), actualItem.get().getId());
        assertEquals(seller.getId(), actualItem.get().getSellerId());
        assertEquals("Tượng gỗ", actualItem.get().getItemName());
    }
}
