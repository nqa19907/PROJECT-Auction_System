package auction_system.server.core;

import auction_system.common.models.users.Admin;
import auction_system.common.models.users.User;
import auction_system.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tạo dữ liệu mặc định tối thiểu cho môi trường phát triển.
 */
public final class TestDataGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestDataGenerator.class);
    private static final String adminUsername = "ad1";
    private static final String adminEmail = "ad1@gmail.com";
    private static final String adminPassword = "123456";

    private TestDataGenerator() {
        // Không cho khởi tạo utility class.
    }

    /**
     * Đảm bảo hệ thống có đúng seed admin mặc định khi chưa có admin nào.
     *
     * @param auctionManager manager runtime dùng để lưu user vào database và registry
     */
    public static void generate(final AuctionManager auctionManager) {
        if (hasAdmin(auctionManager)) {
            LOGGER.info("Đã có tài khoản admin, bỏ qua seed admin mặc định.");
            return;
        }

        final User admin = new Admin(
                adminUsername,
                adminEmail,
                SecurityUtils.hashPassword(adminPassword));

        auctionManager.registerUser(admin);
        LOGGER.info("Đã tạo admin mặc định: {} ({})", adminUsername, adminEmail);
    }

    private static boolean hasAdmin(final AuctionManager auctionManager) {
        return auctionManager.getAllUsers().stream()
                .anyMatch(user -> "ADMIN".equalsIgnoreCase(user.getRoleName()));
    }
}
