import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import auction_system.client.services.UserFactory;
import auction_system.common.models.users.Admin;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;

import org.junit.jupiter.api.Test;

/**
 * Kiểm thử lớp {@link UserFactory}.
 *
 * <p>Các nhóm test:
 * <ol>
 *   <li>Role ADMIN — trả về đối tượng {@link Admin} đúng loại.</li>
 *   <li>Role PARTICIPANT — trả về đối tượng {@link Participant} đúng loại.</li>
 *   <li>Role không phân biệt hoa/thường — "admin" và "ADMIN" tạo cùng loại.</li>
 *   <li>Role không xác định — fallback về Participant.</li>
 *   <li>userId hợp lệ — được gán vào đối tượng.</li>
 *   <li>userId null hoặc blank — không throw, không gán.</li>
 *   <li>Thuộc tính username và email được giữ nguyên.</li>
 * </ol>
 */
public class UserFactoryTest {

    // =========================================================================
    // Role ADMIN
    // =========================================================================

    /**
     * Role "ADMIN" phải trả về đối tượng Admin.
     */
    @Test
    void create_RoleAdmin_ReturnsAdminInstance() {
        User user = UserFactory.create("ADMIN", "adminUser", "admin@test.com", 0.0);

        assertInstanceOf(Admin.class, user,
                "Role ADMIN phai tao doi tuong Admin.");
    }

    /**
     * Role "ADMIN" không null, username và email được gán đúng.
     */
    @Test
    void create_RoleAdmin_UsernameAndEmailPreserved() {
        User user = UserFactory.create("ADMIN", "adminUser", "admin@test.com", 0.0);

        assertNotNull(user, "Doi tuong khong duoc null.");
        assertEquals("adminUser", user.getUsername(),
                "Username phai duoc giu nguyen.");
        assertEquals("admin@test.com", user.getEmail(),
                "Email phai duoc giu nguyen.");
    }

    // =========================================================================
    // Role PARTICIPANT
    // =========================================================================

    /**
     * Role "PARTICIPANT" phải trả về đối tượng Participant.
     */
    @Test
    void create_RoleParticipant_ReturnsParticipantInstance() {
        User user = UserFactory.create("PARTICIPANT", "buyer", "buyer@test.com", 5000.0);

        assertInstanceOf(Participant.class, user,
                "Role PARTICIPANT phai tao doi tuong Participant.");
    }

    /**
     * Balance được truyền qua cho Participant.
     */
    @Test
    void create_RoleParticipant_BalancePreserved() {
        Participant user = (Participant) UserFactory.create(
                "PARTICIPANT", "buyer", "buyer@test.com", 9999.0);

        assertEquals(9999.0, user.getBalance(), 0.001,
                "Balance phai duoc giu nguyen trong Participant.");
    }

    // =========================================================================
    // Không phân biệt hoa/thường
    // =========================================================================

    /**
     * Role "admin" viết thường phải tạo Admin giống "ADMIN".
     */
    @Test
    void create_RoleAdminLowercase_ReturnsAdminInstance() {
        User user = UserFactory.create("admin", "u", "u@u.com", 0.0);

        assertInstanceOf(Admin.class, user,
                "Role 'admin' viet thuong phai tao Admin.");
    }

    /**
     * Role "Admin" mixed-case phải tạo Admin.
     */
    @Test
    void create_RoleAdminMixedCase_ReturnsAdminInstance() {
        User user = UserFactory.create("Admin", "u", "u@u.com", 0.0);

        assertInstanceOf(Admin.class, user,
                "Role 'Admin' mixed-case phai tao Admin.");
    }

    // =========================================================================
    // Role không xác định
    // =========================================================================

    /**
     * Role không tồn tại phải fallback về Participant, không throw.
     */
    @Test
    void create_UnknownRole_ReturnsParticipantFallback() {
        User user = UserFactory.create("MODERATOR", "u", "u@u.com", 0.0);

        assertNotNull(user, "Doi tuong khong duoc null voi role la.");
        assertInstanceOf(Participant.class, user,
                "Role la phai fallback ve Participant.");
    }

    // =========================================================================
    // userId hợp lệ
    // =========================================================================

    /**
     * userId hợp lệ phải được gán vào đối tượng.
     */
    @Test
    void create_WithValidUserId_IdIsSet() {
        User user = UserFactory.create(
                "PARTICIPANT", "uid-42", "buyer", "buyer@test.com", 0.0);

        assertEquals("uid-42", user.getId(),
                "userId hop le phai duoc gan vao doi tuong.");
    }

    /**
     * userId null không throw và không ghi đè id tự sinh.
     */
    @Test
    void create_WithNullUserId_DoesNotThrow() {
        User user = UserFactory.create(
                "PARTICIPANT", null, "buyer", "buyer@test.com", 0.0);

        assertNotNull(user, "Doi tuong khong duoc null khi userId null.");
    }

    /**
     * userId blank không throw và không ghi đè id tự sinh.
     */
    @Test
    void create_WithBlankUserId_DoesNotThrow() {
        User user = UserFactory.create(
                "PARTICIPANT", "   ", "buyer", "buyer@test.com", 0.0);

        assertNotNull(user, "Doi tuong khong duoc null khi userId blank.");
    }

    // =========================================================================
    // Overload không nhận userId
    // =========================================================================

    /**
     * Overload không nhận userId phải trả về đúng loại như Admin.
     */
    @Test
    void create_ThreeArgOverload_RoleAdmin_ReturnsAdmin() {
        User user = UserFactory.create("ADMIN", "adminUser", "a@a.com", 0.0);

        assertInstanceOf(Admin.class, user,
                "Overload 3 tham so voi role ADMIN phai tra ve Admin.");
    }
}