package auction_system.server;

import auction_system.common.network.NetworkConfig;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.SocketServer;
import auction_system.server.persistence.serialization.SerializedDatabase;
import auction_system.server.services.AuctionBidService;
import auction_system.server.services.AuthService;
import auction_system.server.services.ParticipantItemService;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp chính để khởi chạy ứng dụng máy chủ (Server) cho hệ thống đấu giá.
 */
public class ServerApp {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerApp.class);

    /** Đường dẫn thư mục lưu trữ dữ liệu serialization. */
    private static final Path DATA_DIRECTORY = Path.of("data");

    /**
     * Ngăn việc khởi tạo lớp tiện ích ServerApp từ bên ngoài.
     */
    private ServerApp() {
        // Không cho phép khởi tạo lớp tiện ích khởi chạy ứng dụng.
    }

    /**
     * Phương thức khởi chạy chính của ứng dụng máy chủ (Server).
     *
     * @param args Các tham số dòng lệnh được truyền vào khi khởi động.
     */
    public static void main(final String[] args) {
        int port = NetworkConfig.SERVER_PORT;
        final SerializedDatabase database = new SerializedDatabase(DATA_DIRECTORY);

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException exception) {
                LOGGER.warn("Cổng không hợp lệ, dùng cổng mặc định "
                        + NetworkConfig.SERVER_PORT);
            }
        }

        final AuctionManager auctionManager = AuctionManager.getInstance(database);
        final AuthService authService = new AuthService(database);
        final AuctionBidService auctionBidService = new AuctionBidService(database, auctionManager);
        final ParticipantItemService participantItemService = new ParticipantItemService(database);

        if (!authService.isEmailTaken("qa@gmail.com")) {
            authService.register(
                "qa",
                "qa@gmail.com",
                "123456",
                "PARTICIPANT");
        }

        final SocketServer socketServer = SocketServer.getInstance(
                port, authService, auctionManager, auctionBidService, participantItemService);



        LOGGER.info("Đang khởi động hệ thống đấu giá...");

        // Khởi chạy SocketServer trên luồng hiện tại
        // Lưu ý: SocketServer.start() là một vòng lặp vô hạn (blocking)
        socketServer.start();
    }
}
