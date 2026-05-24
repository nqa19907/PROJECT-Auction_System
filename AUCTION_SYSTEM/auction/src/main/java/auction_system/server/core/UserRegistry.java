package auction_system.server.core;

import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry runtime cho tài khoản đã nạp từ persistence.
 */
final class UserRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRegistry.class);

    private final SerializedDatabase database;
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();

    UserRegistry(final SerializedDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    void loadFromPersistence() {
        usersByUsername.clear();
        database.users().findAll().forEach(user -> usersByUsername.put(user.getUsername(), user));
    }

    boolean isEmpty() {
        return usersByUsername.isEmpty();
    }

    int count() {
        return usersByUsername.size();
    }

    User register(final User user) {
        Objects.requireNonNull(user, "user");

        // Ưu tiên dữ liệu đã có trong DB để tránh tạo trùng account khi seed/test chạy lại.
        final User persistedUser = database.users()
                .findByEmail(user.getEmail())
                .or(() -> database.users().findByUsername(user.getUsername()))
                .orElseGet(() -> database.users().save(user));

        usersByUsername.put(persistedUser.getUsername(), persistedUser);
        LOGGER.info("Đăng ký/nạp user: {}", persistedUser.getUsername());

        return persistedUser;
    }

    boolean containsUsername(final String username) {
        return usersByUsername.containsKey(username);
    }

    User findByCredentials(final String email, final String password) {
        if (email == null || password == null) {
            return null;
        }

        return usersByUsername.values().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email)
                        && user.getPassword().equals(password))
                .findFirst()
                .orElse(null);
    }

    User findById(final String userId) {
        if (userId == null) {
            return null;
        }

        return usersByUsername.values().stream()
                .filter(user -> userId.equals(user.getId()))
                .findFirst()
                .orElse(null);
    }

    List<User> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(usersByUsername.values()));
    }

    void remove(final User user) {
        if (user != null) {
            usersByUsername.remove(user.getUsername());
        }
    }

    boolean deleteById(final String userId) {
        return database.users().deleteById(userId);
    }

    List<User> findParticipants() {
        return usersByUsername.values().stream()
                .filter(user -> user instanceof Participant)
                .collect(Collectors.toList());
    }
}
