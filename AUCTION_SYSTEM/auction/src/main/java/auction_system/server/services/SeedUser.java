package auction_system.server.services;

import auction_system.common.models.users.User;
import auction_system.server.persistence.serialization.DatabasePathProvider;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Công cụ tạo nhanh một tài khoản thật vào database serialization.
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
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        final SerializedDatabase database =
                new SerializedDatabase(DatabasePathProvider.defaultDataDirectory());
        final AuthService authService = new AuthService(database);

        final User user = authService.register(
                "ad1",
                "ad1@gmail.com",
                "123456",
                "ADMIN");

        System.out.println("Đã tạo user: " + user.getUsername());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Role: " + user.getRoleName());
    }
}
