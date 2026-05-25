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
        activeUsers.put(user.getId(), user);
        LOGGER.debug("Online: {} (total: {})", user.getUsername(), activeUsers.size());
    }

    void markOffline(final User user) {
        activeUsers.remove(user.getId());
        LOGGER.debug("Offline: {} (total: {})", user.getUsername(), activeUsers.size());
    }

    void remove(final String userId) {
        activeUsers.remove(userId);
    }

    boolean isOnline(final String userId) {
        return activeUsers.containsKey(userId);
    }

    int count() {
        return activeUsers.size();
    }

    Map<String, User> findAll() {
        return Collections.unmodifiableMap(activeUsers);
    }
}
