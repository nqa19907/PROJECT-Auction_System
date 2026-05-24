package auction_system.server.services;

import auction_system.common.models.users.User;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.nio.file.Path;

/**
 * Công cụ tạo nhanh tài khoản vào database serialization.
 *
 * <p>Lớp này chỉ dùng khi phát triển để kiểm tra việc ghi dữ liệu vào file
 * {@code data/users.ser}. Không nên gọi lớp này từ {@code ClientApp}.
 */
public final class SeedUser {
    private SeedUser() {
        // Không cho khởi tạo utility class.
    }

    /**
     * Chạy công cụ tạo user mẫu.
     *
     * @param args tham số dòng lệnh, hiện chưa sử dụng
     */
    public static void main(final String[] args) {
        // Dùng cùng thư mục data với luồng chạy app hiện tại của nhóm.
        final SerializedDatabase database =
                new SerializedDatabase(Path.of("data"));
        final AuthService authService = new AuthService(database);

        // Seed tài khoản bidder mẫu. Nếu đã tồn tại thì bỏ qua để không dừng chương trình.
        try {
            final User user = authService.register(
                    "testuser02",
                    "testuser02@example.com",
                    "123456",
                    "BIDDER");

            System.out.println("Đã tạo user: " + user.getUsername());
            System.out.println("Email: " + user.getEmail());
            System.out.println("Role: " + user.getRoleName());
        } catch (IllegalArgumentException exception) {
            System.out.println("Bỏ qua testuser02: " + exception.getMessage());
        }

        // Seed thêm admin để đăng nhập dashboard quản trị.
        try {
            final User admin = authService.register(
                    "admin02",
                    "admin02@example.com",
                    "123456",
                    "ADMIN");

            System.out.println("Đã tạo user: " + admin.getUsername());
            System.out.println("Email: " + admin.getEmail());
            System.out.println("Role: " + admin.getRoleName());
        } catch (IllegalArgumentException exception) {
            System.out.println("Bỏ qua admin02: " + exception.getMessage());
        }
    }
}
