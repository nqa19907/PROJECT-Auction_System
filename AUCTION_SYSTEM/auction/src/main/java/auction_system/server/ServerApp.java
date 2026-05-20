package auction_system.server;

import auction_system.client.services.AuthService;
import auction_system.common.network.NetworkConfig;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.SocketServer;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Lớp chính để khởi chạy ứng dụng máy chủ (Server) cho hệ thống đấu giá.
 */
public class ServerApp {
    private static final Logger LOGGER = Logger.getLogger(ServerApp.class.getName());

    /** Đường dẫn thư mục lưu trữ dữ liệu serialization. */
    private static final Path DATA_DIRECTORY = Path.of("data");
    
    /**
     * Phương thức khởi chạy chính của ứng dụng máy chủ (Server).
    *
    * @param args Các tham số dòng lệnh được truyền vào khi khởi động.
    */
    private ServerApp() {
        // Không cho phép khởi tạo lớp tiện ích khởi chạy ứng dụng.
    }
   public static void main(String[] args) {
        int port = NetworkConfig.SERVER_PORT;
        SerializedDatabase database = new SerializedDatabase(DATA_DIRECTORY);
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException exception) {
                LOGGER.warning(
                    "Cổng không hợp lệ, dùng cổng mặc định " + NetworkConfig.SERVER_PORT);
            }
        }
        final AuctionManager auctionManager = AuctionManager.getInstance(database);
        final AuthService authService = AuthService.getInstance(database);
        final SocketServer socketServer = SocketServer.getInstance(port, authService, auctionManager);
        LOGGER.info("Đang khởi động hệ thống đấu giá...");
        
        // Khởi chạy SocketServer trên luồng hiện tại
        // Lưu ý: SocketServer.start() là một vòng lặp vô hạn (blocking)
        socketServer.start();
    }
}