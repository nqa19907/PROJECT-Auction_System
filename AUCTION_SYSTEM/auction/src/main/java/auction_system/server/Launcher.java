package auction_system.server;

/**
 * Lớp khởi chạy trung gian cho ứng dụng server.
 */
public final class Launcher {

    /**
     * Ngăn khởi tạo lớp tiện ích khởi chạy server.
     */
    private Launcher() {
        // Lớp chỉ chứa điểm vào tĩnh cho file JAR server.
    }

    /**
     * Khởi chạy tiến trình server đấu giá.
     *
     * @param args tham số dòng lệnh được truyền vào khi khởi động server
     */
    public static void main(final String[] args) {
        ServerApp.main(args);
    }
}
