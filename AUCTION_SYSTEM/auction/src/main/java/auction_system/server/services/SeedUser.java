package auction_system.server.services;

import auction_system.common.models.users.Admin;
import auction_system.common.models.users.User;
import auction_system.common.utils.SecurityUtils;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Công cụ tạo nhanh tài khoản mẫu vào database serialization.
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

        final SerializedDatabase database = new SerializedDatabase(Path.of("data"));
        final AuthService authService = new AuthService(database);

        seedParticipant(authService);
        seedAdmin(database);
    }

    /**
     * Tạo tài khoản bidder mẫu qua luồng đăng ký public.
     *
     * @param authService service xác thực
     */
    private static void seedParticipant(final AuthService authService) {
        try {
            final User user = authService.register(
                    "testuser02",
                    "testuser02@example.com",
                    "123456",
                    "BIDDER");

            printUser("Đã tạo user", user);
        } catch (IllegalArgumentException exception) {
            System.out.println("Bỏ qua testuser02: " + exception.getMessage());
        }
    }

    /**
     * Tạo tài khoản admin bằng luồng seed nội bộ, không đi qua đăng ký public.
     *
     * @param database database serialization
     */
    private static void seedAdmin(final SerializedDatabase database) {
        final String adminEmail = "admin1@gmail.com";

        if (database.users().findByEmail(adminEmail).isPresent()) {
            System.out.println("Bỏ qua admin1: email đã tồn tại.");
            return;
        }

        final User admin = new Admin(
                "admin1",
                adminEmail,
                SecurityUtils.hashPassword("123456"));
        database.users().save(admin);
        database.flushAll();

        printUser("Đã tạo user", admin);
    }

    /**
     * In thông tin tài khoản vừa được seed.
     *
     * @param prefix tiền tố thông báo
     * @param user tài khoản vừa tạo
     */
    private static void printUser(final String prefix, final User user) {
        System.out.println(prefix + ": " + user.getUsername());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Role: " + user.getRoleName());
    }
}
