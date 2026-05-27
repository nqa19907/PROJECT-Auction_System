package auction_system.server.persistence.repositories;

import auction_system.common.models.users.User;
import auction_system.server.persistence.exceptions.DatabaseException;
import auction_system.server.persistence.serialization.SerializedFileStorage;
import auction_system.server.persistence.serialization.SerializedRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Repository quản lý dữ liệu người dùng trong hệ thống đấu giá.
 *
 * <p>Lớp này lưu trữ toàn bộ người dùng vào file {@code users.ser} thông qua
 * Java Serialization. Các lớp con của {@link User} như Seller, Bidder hoặc
 * Participant vẫn được lưu chung thông qua đa hình.
 *
 * <p>Repository này chỉ xử lý dữ liệu bền vững. Trạng thái online/offline của
 * người dùng nên được quản lý bởi tầng runtime như AuctionManager hoặc
 * SessionManager.
 */
public class UserRepository extends SerializedRepository<User> {

    /**
     * Khởi tạo repository người dùng với đường dẫn file lưu trữ.
     *
     * @param storagePath đường dẫn tới file users.ser
     */
    public UserRepository(final Path storagePath) {
        super(new SerializedFileStorage<>(storagePath), User::getId);
    }

    /**
     * Lưu mới hoặc cập nhật thông tin người dùng.
     *
     * <p>Nếu là người dùng mới, repository sẽ kiểm tra username và email để tránh
     * trùng tài khoản. Nếu là cập nhật người dùng cũ, username và email của chính
     * người dùng đó vẫn được chấp nhận.
     *
     * @param user người dùng cần lưu
     * @return người dùng đã được lưu
     */
    @Override
    public synchronized User save(final User user) {
        Objects.requireNonNull(user, "user");

        validateUser(user);
        validateUniqueUsername(user);
        validateUniqueEmail(user);

        return super.save(user);
    }

    /**
     * Tìm người dùng theo tên đăng nhập.
     *
     * @param username tên đăng nhập cần tìm
     * @return người dùng nếu tồn tại
     */
    public Optional<User> findByUsername(final String username) {
        validateText(username, "Tên đăng nhập không được rỗng.");

        return findAll().stream()
            .filter(user -> username.equalsIgnoreCase(user.getUsername()))
            .findFirst();
    }

    /**
     * Tìm người dùng theo email.
     *
     * @param email email cần tìm
     * @return người dùng nếu tồn tại
     */
    public Optional<User> findByEmail(final String email) {
        validateText(email, "Email không được rỗng.");

        return findAll().stream()
            .filter(user -> email.equalsIgnoreCase(user.getEmail()))
            .findFirst();
    }

    /**
     * Kiểm tra username đã tồn tại hay chưa.
     *
     * @param username tên đăng nhập cần kiểm tra
     * @return {@code true} nếu username đã tồn tại
     */
    public boolean existsByUsername(final String username) {
        return findByUsername(username).isPresent();
    }

    /**
     * Kiểm tra email đã tồn tại hay chưa.
     *
     * @param email email cần kiểm tra
     * @return {@code true} nếu email đã tồn tại
     */
    public boolean existsByEmail(final String email) {
        return findByEmail(email).isPresent();
    }

    /**
     * Tìm danh sách người dùng theo vai trò.
     *
     * <p>Hàm này dùng được với các vai trò như ADMIN hoặc PARTICIPANT nếu
     * model User có triển khai phương thức getRoleName().
     *
     * @param roleName tên vai trò cần tìm
     * @return danh sách người dùng thuộc vai trò tương ứng
     */
    public List<User> findByRoleName(final String roleName) {
        validateText(roleName, "Tên vai trò không được rỗng.");

        return findAll().stream()
            .filter(user -> roleName.equalsIgnoreCase(user.getRoleName()))
            .toList();
    }

    /**
     * Kiểm tra dữ liệu người dùng trước khi lưu.
     *
     * @param user người dùng cần kiểm tra
     */
    private void validateUser(final User user) {
        validateText(user.getId(), "Mã người dùng không được rỗng.");
        validateText(user.getUsername(), "Tên đăng nhập không được rỗng.");
        validateText(user.getEmail(), "Email không được rỗng.");
    }

    /**
     * Kiểm tra username không bị trùng với người dùng khác.
     *
     * @param user người dùng cần kiểm tra
     */
    private void validateUniqueUsername(final User user) {
        Optional<User> existingUser = findByUsername(user.getUsername());

        if (existingUser.isPresent()
            && !existingUser.get().getId().equals(user.getId())) {
            throw new DatabaseException(
            "Tên người dùng đã tồn tại: " + user.getUsername());
        }
    }

    /**
     * Kiểm tra email không bị trùng với người dùng khác.
     *
     * @param user người dùng cần kiểm tra
     */
    private void validateUniqueEmail(final User user) {
        Optional<User> existingUser = findByEmail(user.getEmail());

        if (existingUser.isPresent()
            && !existingUser.get().getId().equals(user.getId())) {
            throw new DatabaseException("Email đã tồn tại: " + user.getEmail());
        }
    }

    /**
     * Kiểm tra chuỗi không được null hoặc rỗng.
     *
     * @param value giá trị cần kiểm tra
     * @param message thông báo lỗi
     */
    private void validateText(final String value, final String message) {
        if (value == null || value.isBlank()) {
            throw new DatabaseException(message);
        }
    }
}
