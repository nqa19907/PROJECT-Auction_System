package auction_system.client;

/**
 * Lớp khởi chạy trung gian cho ứng dụng JavaFX client.
 *
 * <p>JavaFX không nên được khai báo trực tiếp làm main class trong fat JAR vì JVM có cơ chế
 * kiểm tra runtime JavaFX riêng. Lớp launcher này không kế thừa {@code Application}, nên có thể
 * được dùng làm điểm vào ổn định khi chạy bằng lệnh {@code java -jar}.
 */
public final class ClientLauncher {

    /**
     * Ngăn khởi tạo lớp tiện ích khởi chạy client.
     */
    private ClientLauncher() {
        // Lớp chỉ chứa điểm vào tĩnh cho file JAR client.
    }

    /**
     * Khởi chạy giao diện JavaFX client.
     *
     * @param args tham số dòng lệnh được truyền vào khi khởi động ứng dụng
     */
    public static void main(final String[] args) {
        ClientApp.main(args);
    }
}
