package auction_system.client.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Chính sách hiển thị giao diện theo vai trò người dùng.
 *
 * <p>Class này gom toàn bộ rule UI theo role về một chỗ để dễ bảo trì.
 */
public final class RoleUiPolicy {

    private RoleUiPolicy() {
    }

    /**
     * Trả về tập mục Sidebar được hiển thị cho một role.
     *
     * @param roleName tên role (ví dụ: ADMIN hoặc PARTICIPANT)
     * @return tập mục được phép hiển thị
     */
    public static Set<SidebarItem> sidebarItemsForRole(final String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return defaultItems();
        }
        final String normalizedRole = roleName.toUpperCase(Locale.ROOT);
        return switch (normalizedRole) {
            case "ADMIN" -> EnumSet.of(SidebarItem.ADMIN_DEMO);
            case "PARTICIPANT" -> EnumSet.of(SidebarItem.PUBLISH_ITEM);
            default -> defaultItems();
        };
    }

    /**
     * Tập mặc định khi role không có rule riêng.
     *
     * @return tập mục mặc định
     */
    private static Set<SidebarItem> defaultItems() {
        return EnumSet.of(SidebarItem.PUBLISH_ITEM);
    }
}
