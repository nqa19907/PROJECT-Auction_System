package auction_system.server;

/**
 * Lớp trung gian để khởi chạy ứng dụng JavaFX.
 * Việc tạo lớp này là bắt buộc để build Fat JAR cho JavaFX 11+ trở lên
 * nhằm tránh lỗi "JavaFX runtime components are missing".
 */
public class Launcher {

    private Launcher() {
        // Ngăn chặn khởi tạo lớp tiện ích từ bên ngoài
    }

    public static void main(String[] args) {
        ServerApp.main(args);
    }
}