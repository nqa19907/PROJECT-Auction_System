import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.client.security.RoleUiPolicy;
import auction_system.client.security.SidebarItem;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Kiểm thử lớp {@link RoleUiPolicy} — chính sách hiển thị sidebar theo vai trò.
 *
 * <p>Các nhóm test:
 * <ol>
 *   <li>Role ADMIN — chỉ hiện mục admin, không hiện mục participant.</li>
 *   <li>Role PARTICIPANT — chỉ hiện mục participant, không hiện mục admin.</li>
 *   <li>Role không phân biệt hoa/thường.</li>
 *   <li>Role null hoặc blank — trả về tập mặc định, không throw.</li>
 *   <li>Role không tồn tại — trả về tập mặc định.</li>
 *   <li>Kết quả không bao giờ null.</li>
 * </ol>
 */
public class RoleUiPolicyTest {

    // =========================================================================
    // ADMIN
    // =========================================================================

    /**
     * Role ADMIN phải chứa mục ADMIN_DEMO.
     */
    @Test
    void sidebarItemsForRole_Admin_ContainsAdminDemo() {
        Set<SidebarItem> items = RoleUiPolicy.sidebarItemsForRole("ADMIN");

        assertTrue(items.contains(SidebarItem.ADMIN_DEMO),
                "Role ADMIN phai chua ADMIN_DEMO.");
    }

    /**
     * Role ADMIN không được chứa mục PUBLISH_ITEM của participant.
     */
    @Test
    void sidebarItemsForRole_Admin_DoesNotContainPublishItem() {
        Set<SidebarItem> items = RoleUiPolicy.sidebarItemsForRole("ADMIN");

        assertFalse(items.contains(SidebarItem.PUBLISH_ITEM),
                "Role ADMIN khong duoc chua PUBLISH_ITEM.");
    }

    /**
     * Role ADMIN — tập kết quả không được null.
     */
    @Test
    void sidebarItemsForRole_Admin_NotNull() {
        Set<SidebarItem> items = RoleUiPolicy.sidebarItemsForRole("ADMIN");

        assertNotNull(items, "Ket qua khong duoc null cho role ADMIN.");
    }

    /**
     * Role ADMIN — tập kết quả không được rỗng.
     */
    @Test
    void sidebarItemsForRole_Admin_NotEmpty() {
        Set<SidebarItem> items = RoleUiPolicy.sidebarItemsForRole("ADMIN");

        assertFalse(items.isEmpty(), "Ket qua khong duoc rong cho role ADMIN.");
    }

    // =========================================================================
    // PARTICIPANT
    // =========================================================================

    /**
     * Role PARTICIPANT phải chứa mục PUBLISH_ITEM.
     */
    @Test
    void sidebarItemsForRole_Participant_ContainsPublishItem() {
        Set<SidebarItem> items = RoleUiPolicy.sidebarItemsForRole("PARTICIPANT");

        assertTrue(items.contains(SidebarItem.PUBLISH_ITEM),
                "Role PARTICIPANT phai chua PUBLISH_ITEM.");
    }

    /**
     * Role PARTICIPANT không được chứa mục ADMIN_DEMO.
     */
    @Test
    void sidebarItemsForRole_Participant_DoesNotContainAdminDemo() {
        Set<SidebarItem> items = RoleUiPolicy.sidebarItemsForRole("PARTICIPANT");

        assertFalse(items.contains(SidebarItem.ADMIN_DEMO),
                "Role PARTICIPANT khong duoc chua ADMIN_DEMO.");
    }

    /**
     * Role PARTICIPANT — tập kết quả không được null.
     */
    @Test
    void sidebarItemsForRole_Participant_NotNull() {
        assertNotNull(RoleUiPolicy.sidebarItemsForRole("PARTICIPANT"),
                "Ket qua khong duoc null cho role PARTICIPANT.");
    }

    // =========================================================================
    // Không phân biệt hoa/thường
    // =========================================================================

    /**
     * Role "admin" chữ thường phải cho kết quả giống "ADMIN".
     */
    @Test
    void sidebarItemsForRole_LowercaseAdmin_SameAsUppercase() {
        Set<SidebarItem> upper = RoleUiPolicy.sidebarItemsForRole("ADMIN");
        Set<SidebarItem> lower = RoleUiPolicy.sidebarItemsForRole("admin");

        assertTrue(lower.contains(SidebarItem.ADMIN_DEMO),
                "Role 'admin' chu thuong phai cho ket qua giong 'ADMIN'.");
        assertFalse(lower.contains(SidebarItem.PUBLISH_ITEM),
                "Role 'admin' chu thuong khong duoc chua PUBLISH_ITEM.");
    }

    /**
     * Role "participant" chữ thường phải cho kết quả giống "PARTICIPANT".
     */
    @Test
    void sidebarItemsForRole_LowercaseParticipant_SameAsUppercase() {
        Set<SidebarItem> items = RoleUiPolicy.sidebarItemsForRole("participant");

        assertTrue(items.contains(SidebarItem.PUBLISH_ITEM),
                "Role 'participant' chu thuong phai chua PUBLISH_ITEM.");
    }

    // =========================================================================
    // Role null và blank — fallback mặc định
    // =========================================================================

    /**
     * Role null không được throw — phải trả về tập mặc định không null.
     */
    @Test
    void sidebarItemsForRole_NullRole_ReturnsDefault() {
        Set<SidebarItem> items = RoleUiPolicy.sidebarItemsForRole(null);

        assertNotNull(items, "Role null phai tra ve tap mac dinh, khong null.");
        assertFalse(items.isEmpty(), "Tap mac dinh khong duoc rong.");
    }

    /**
     * Role blank không được throw — phải trả về tập mặc định.
     */
    @Test
    void sidebarItemsForRole_BlankRole_ReturnsDefault() {
        Set<SidebarItem> items = RoleUiPolicy.sidebarItemsForRole("   ");

        assertNotNull(items, "Role blank phai tra ve tap mac dinh.");
        assertFalse(items.isEmpty(), "Tap mac dinh khong duoc rong.");
    }

    // =========================================================================
    // Role không tồn tại
    // =========================================================================

    /**
     * Role không xác định phải trả về tập mặc định không null, không rỗng.
     */
    @Test
    void sidebarItemsForRole_UnknownRole_ReturnsDefault() {
        Set<SidebarItem> items = RoleUiPolicy.sidebarItemsForRole("MANAGER");

        assertNotNull(items, "Role khong ton tai phai tra ve tap mac dinh khong null.");
        assertFalse(items.isEmpty(), "Tap mac dinh khong duoc rong.");
    }

    /**
     * Role không xác định không được chứa ADMIN_DEMO.
     */
    @Test
    void sidebarItemsForRole_UnknownRole_DoesNotContainAdminDemo() {
        Set<SidebarItem> items = RoleUiPolicy.sidebarItemsForRole("UNKNOWN");

        assertFalse(items.contains(SidebarItem.ADMIN_DEMO),
                "Role khong xac dinh khong duoc chua ADMIN_DEMO.");
    }
}