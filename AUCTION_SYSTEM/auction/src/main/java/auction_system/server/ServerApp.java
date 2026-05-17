package auction_system.server;

import auction_system.server.network.SocketServer;
import java.util.logging.Logger;

/**
 * Lớp chính để khởi chạy ứng dụng máy chủ (Server) cho hệ thống đấu giá.
 */
public class ServerApp {
    private static final Logger LOGGER = Logger.getLogger(ServerApp.class.getName());

    /**
     * Phương thức khởi chạy chính của ứng dụng máy chủ (Server).
     *
     * @param args Các tham số dòng lệnh được truyền vào khi khởi động.
     */
    public static void main(String[] args) {
        LOGGER.info("Đang khởi động hệ thống đấu giá...");
        
        // Khởi chạy SocketServer trên luồng hiện tại
        // Lưu ý: SocketServer.start() là một vòng lặp vô hạn (blocking)
        SocketServer.getInstance().start();
    }
}