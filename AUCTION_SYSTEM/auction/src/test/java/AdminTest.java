import auction_system.common.models.users.Admin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử lớp Admin.
 * Các behavior chung của User và Entity đã được cover trong ModelTest —
 * test đặc thù của Admin.
 */
public class AdminTest {

    private Admin admin;

    @BeforeEach
    void setUp() {
        admin = new Admin("admin01", "admin@auction.com", "securepass");
    }

    @Test
    void testGetRoleName_ReturnsADMIN() {
        assertEquals("ADMIN", admin.getRoleName());
    }

    @Test
    void testGetRoleDisplayName_ReturnsVietnameseLabel() {
        assertEquals("Quản trị viên", admin.getRoleDisplayName());
    }

    @Test
    void testSetBanned_DoesNotThrow() {
        assertDoesNotThrow(() -> admin.setBanned(true));
        assertDoesNotThrow(() -> admin.setBanned(false));
    }

    @Test
    void testToString_ContainsUsernameAndEmail() {
        String str = admin.toString();
        assertTrue(str.contains("admin01"));
        assertTrue(str.contains("admin@auction.com"));
    }
}