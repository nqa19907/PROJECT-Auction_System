package auction_system.server.core;

import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry runtime cho user đã đăng ký.
 */
final class UserRegistry {

    private final SerializedDatabase database;
    private final Map<String, User> usersByUsername;

    UserRegistry(final SerializedDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
        this.usersByUsername = new ConcurrentHashMap<>();
    }

    void loadFromDatabase() {
        database.users().findAll().forEach(user -> usersByUsername.put(user.getUsername(), user));
    }

    int size() {
        return usersByUsername.size();
    }

    boolean isEmpty() {
        return usersByUsername.isEmpty();
    }

    User registerUser(final User user) {
        Objects.requireNonNull(user, "user");

        // Thử tìm user bằng email, nếu không thấy thì thử bằng username.
        // Nếu cả hai đều không tồn tại, lưu user mới vào DB.
        User persistedUser = database.users()
                .findByEmail(user.getEmail())
                .or(() -> database.users().findByUsername(user.getUsername()))
                .orElseGet(() -> database.users().save(user));

        // Cập nhật registry in-memory
        usersByUsername.put(persistedUser.getUsername(), persistedUser);

        return persistedUser;
    }

    boolean isUsernameTaken(final String username) {
        return usersByUsername.containsKey(username);
    }

    User findUserByCredentials(final String email, final String password) {
        if (email == null || password == null) {
            return null;
        }

        return usersByUsername.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email)
                        && u.getPassword().equals(password))
                .findFirst()
                .orElse(null);
    }

    User findUserById(final String userId) {
        return usersByUsername.values().stream()
                .filter(u -> u.getId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    void remove(final User user) {
        usersByUsername.remove(user.getUsername());
    }

    List<User> getAllParticipants() {
        return usersByUsername.values().stream()
                .filter(u -> u instanceof Participant)
                .collect(Collectors.toList());
    }
}
