package auction_system.server.core;

import auction_system.server.network.ClientHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry socket handler theo user đang online.
 */
final class ClientConnectionRegistry {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ClientConnectionRegistry.class);

    private final Map<String, ClientHandler> handlersByUserId = new ConcurrentHashMap<>();

    void register(final String userId, final ClientHandler handler) {
        if (userId == null || userId.isBlank() || handler == null) {
            return;
        }

        handlersByUserId.put(userId, handler);
    }

    void unregister(final String userId, final ClientHandler handler) {
        if (userId == null || userId.isBlank() || handler == null) {
            return;
        }

        // So sánh cả handler để không gỡ nhầm nếu user reconnect rất nhanh.
        handlersByUserId.remove(userId, handler);
    }

    void notifyUser(final String userId, final String message) {
        final ClientHandler handler = handlersByUserId.get(userId);

        if (handler == null) {
            LOGGER.debug("Bỏ qua realtime theo user vì user offline: {}", userId);
            return;
        }

        handler.sendDirect(message);
    }
}
