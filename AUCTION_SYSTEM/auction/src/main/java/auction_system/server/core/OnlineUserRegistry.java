package auction_system.server.core;

import auction_system.common.models.users.User;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Theo dõi trạng thái online runtime của user.
 */
final class OnlineUserRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(OnlineUserRegistry.class);

    private final Map<String, User> activeUsers = new ConcurrentHashMap<>();

    void markOnline(final User user) {
        /*
         * Online state là trạng thái runtime của socket, không phải nguồn dữ liệu
         * bền vững. Admin dashboard đọc map này để hiển thị ONLINE/OFFLINE.
         */
        activeUsers.put(user.getId(), user);
        LOGGER.debug("Online: {} (total: {})", user.getUsername(), activeUsers.size());
    }

    void markOffline(final User user) {
        /*
         * Khi socket đóng hoặc logout, user rời map online. Thông tin tài khoản
         * vẫn còn trong UserRegistry/database, chỉ trạng thái kết nối bị xoá.
         */
        activeUsers.remove(user.getId());
        LOGGER.debug("Offline: {} (total: {})", user.getUsername(), activeUsers.size());
    }

    void remove(final String userId) {
        /*
         * Đường này dùng khi admin xóa user. Không cần User object vì chỉ cần
         * loại userId khỏi danh sách online runtime.
         */
        activeUsers.remove(userId);
    }

    boolean isOnline(final String userId) {
        // Query nhanh cho admin list user và các kiểm tra trạng thái kết nối.
        return activeUsers.containsKey(userId);
    }

    int count() {
        // Dùng cho thống kê dashboard/server log, không tính từ database.
        return activeUsers.size();
    }

    Map<String, User> findAll() {
        // Trả view chỉ đọc để caller không sửa trực tiếp map runtime.
        return Collections.unmodifiableMap(activeUsers);
    }
}
