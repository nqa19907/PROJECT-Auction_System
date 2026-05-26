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
        // Username là khóa runtime vì luồng đăng ký/login kiểm tra trùng theo username.
        database.users().findAll().forEach(user -> usersByUsername.put(user.getUsername(), user));
    }

    boolean isEmpty() {
        // Dùng khi khởi động server để quyết định có seed dữ liệu mẫu hay không.
        return usersByUsername.isEmpty();
    }

    int count() {
        // Số lượng user runtime đã nạp, không phải số user đang online.
        return usersByUsername.size();
    }

    User register(final User user) {
        Objects.requireNonNull(user, "user");

        /*
         * Ưu tiên dữ liệu đã có trong DB để tránh tạo trùng account khi seed/test
         * chạy lại. Nếu không tìm thấy thì lưu user mới rồi đưa vào registry.
         */
        final User persistedUser = database.users()
                .findByEmail(user.getEmail())
                .or(() -> database.users().findByUsername(user.getUsername()))
                .orElseGet(() -> database.users().save(user));

        usersByUsername.put(persistedUser.getUsername(), persistedUser);
        LOGGER.info("Đăng ký/nạp user: {}", persistedUser.getUsername());

        return persistedUser;
    }

    boolean containsUsername(final String username) {
        // Kiểm tra nhanh trong registry trước khi AuthService tiếp tục validate đăng ký.
        return usersByUsername.containsKey(username);
    }

    User findByCredentials(final String email, final String password) {
        /*
         * Registry đang index theo username, còn login dùng email. Vì vậy cần
         * duyệt values để tìm email/password khớp trong bộ dữ liệu đã nạp.
         */
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
        /*
         * ID là UUID nằm trong object User, không phải key của map hiện tại.
         * Duyệt values giữ cho registry chỉ có một khóa chính là username.
         */
        if (userId == null) {
            return null;
        }

        return usersByUsername.values().stream()
                .filter(user -> userId.equals(user.getId()))
                .findFirst()
                .orElse(null);
    }

    List<User> findAll() {
        /*
         * Tạo ArrayList mới để caller có snapshot ổn định, rồi bọc unmodifiable
         * để không thể sửa trực tiếp dữ liệu trong registry.
         */
        return Collections.unmodifiableList(new ArrayList<>(usersByUsername.values()));
    }

    void remove(final User user) {
        /*
         * Xóa theo username vì đó là key runtime. Repository sẽ xóa theo id ở
         * bước riêng để giữ rõ ràng giữa bộ nhớ và persistence.
         */
        if (user != null) {
            usersByUsername.remove(user.getUsername());
        }
    }

    boolean deleteById(final String userId) {
        // Persistence vẫn quản lý khóa bền vững theo id, nên delegate xuống repository.
        return database.users().deleteById(userId);
    }

    List<User> findParticipants() {
        // Hiện Bidder/Seller trong UI cũ đều dùng Participant, nên giữ API facade tương thích.
        return usersByUsername.values().stream()
                .filter(user -> user instanceof Participant)
                .collect(Collectors.toList());
    }
}
