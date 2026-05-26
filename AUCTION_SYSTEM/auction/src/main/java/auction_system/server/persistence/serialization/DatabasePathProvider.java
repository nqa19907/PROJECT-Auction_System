package auction_system.server.persistence.serialization;

import java.nio.file.Path;

/**
 * Cung cấp đường dẫn thư mục database serialization dùng chung cho hệ thống.
 *
 * <p>Lớp này tập trung hóa cách xác định thư mục {@code data}, giúp server độc lập,
 * local server trong JavaFX, dashboard quản trị và công cụ seed user cùng dùng một
 * nguồn dữ liệu.
 */
public final class DatabasePathProvider {
    private static final Path moduleDirectory = Path.of("AUCTION_SYSTEM", "auction");
    private static final String dataDirectoryName = "data";

    private DatabasePathProvider() {
        // Không cho khởi tạo lớp tiện ích.
    }

    /**
     * Xác định thư mục data mặc định của module auction.
     *
     * <p>Nếu chương trình chạy từ thư mục module thì dùng trực tiếp thư mục
     * {@code data}. Nếu chạy từ thư mục gốc dự án thì tự trỏ vào
     * {@code AUCTION_SYSTEM/auction/data}.
     *
     * @return đường dẫn tuyệt đối đã chuẩn hóa tới thư mục data của module auction
     */
    public static Path defaultDataDirectory() {
        final Path workingDirectory = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath()
                .normalize();

        if (workingDirectory.endsWith(moduleDirectory)) {
            return workingDirectory.resolve(dataDirectoryName);
        }

        return workingDirectory.resolve(moduleDirectory)
                .resolve(dataDirectoryName)
                .normalize();
    }
}
