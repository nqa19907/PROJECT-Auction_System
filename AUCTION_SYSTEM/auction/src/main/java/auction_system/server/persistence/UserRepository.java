package auction_system.server.persistence;

import auction_system.common.models.users.User;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Repository quản lý dữ liệu người dùng.
 *
 * <p>Repository này lưu được cả Admin, Seller và Bidder vì các lớp con đều là
 * User. Điều kiện là User và các lớp con phải implements Serializable.
 */
public class UserRepository extends SerializedRepository<User> {
  /**
   * Khởi tạo repository người dùng.
   *
   * @param filePath đường dẫn file users.ser
   */
  public UserRepository(final Path filePath) {
    super(new SerializedFileStorage<>(filePath), User::getId);
  }

  /**
   * Tìm người dùng theo tên đăng nhập.
   *
   * @param username tên đăng nhập
   * @return người dùng nếu tồn tại
   */
  public Optional<User> findByUsername(final String username) {
    if (username == null || username.isBlank()) {
      return Optional.empty();
    }

    return findAll().stream()
        .filter(user -> username.equalsIgnoreCase(user.getUsername()))
        .findFirst();
  }

  /**
   * Lấy danh sách người dùng theo vai trò.
   *
   * @param role tên vai trò cần lọc
   * @return danh sách người dùng thuộc vai trò tương ứng
   */
  public List<User> findByRole(final String role) {
    if (role == null || role.isBlank()) {
      return List.of();
    }

    return findAll().stream()
        .filter(user -> role.equalsIgnoreCase(user.getRoleName()))
        .toList();
  }
}