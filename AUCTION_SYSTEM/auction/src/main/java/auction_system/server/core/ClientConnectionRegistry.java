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
        /*
         * Registry này chỉ phục vụ realtime theo user sau khi login. Nếu thiếu
         * userId hoặc handler thì không có đích gửi ổn định, nên bỏ qua thay vì
         * làm hỏng luồng đăng nhập.
         */
        if (userId == null || userId.isBlank() || handler == null) {
            return;
        }

        // Mỗi user hiện chỉ giữ một socket nhận realtime cá nhân mới nhất.
        handlersByUserId.put(userId, handler);
    }

    void unregister(final String userId, final ClientHandler handler) {
        /*
         * Có thể xảy ra reconnect nhanh: handler cũ cleanup sau khi handler mới
         * đã register. remove(key, value) đảm bảo chỉ gỡ đúng kết nối đang đóng.
         */
        if (userId == null || userId.isBlank() || handler == null) {
            return;
        }

        // So sánh cả handler để không gỡ nhầm nếu user reconnect rất nhanh.
        handlersByUserId.remove(userId, handler);
    }

    void notifyUser(final String userId, final String message) {
        /*
         * Realtime theo user không đi qua observer của Auction vì nó chứa dữ liệu
         * cá nhân như số dư ví. Chỉ socket của đúng userId mới nhận message này.
         */
        final ClientHandler handler = handlersByUserId.get(userId);

        if (handler == null) {
            LOGGER.debug("Bỏ qua realtime theo user vì user offline: {}", userId);
            return;
        }

        handler.sendDirect(message);
    }
}
