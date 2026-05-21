package auction_system.server.network;

import auction_system.common.network.NetworkConfig;
import auction_system.server.core.AuctionManager;
import auction_system.server.persistence.serialization.SerializedDatabase;
import auction_system.server.services.AuctionBidService;
import auction_system.server.services.AuthService;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Server Socket chính của hệ thống đấu giá — Singleton.
 * Lắng nghe kết nối TCP từ client và tạo một {@link ClientHandler}
 * trên thread riêng cho mỗi kết nối mới.
 *
 * <p>Giao thức (text-based, UTF-8, mỗi lệnh một dòng):
 * * <pre>
 * Client → Server : COMMAND|param1|param2|...
 * Server → Client : RESPONSE_CODE|data...
 * </pre>
 *
 * <p>Lệnh client gửi lên:
 * * <pre>
 * LOGIN|username|password
 * REGISTER|username|email|password|role   (role: BIDDER / SELLER)
 * LIST_AUCTIONS
 * GET_AUCTION|auctionId
 * JOIN_AUCTION|auctionId
 * LEAVE_AUCTION|auctionId
 * PLACE_BID|auctionId|amount
 * LOGOUT
 * </pre>
 *
 * <p>Phản hồi server gửi xuống:
 * * <pre>
 * LOGIN_OK|userId|username|role
 * LOGIN_FAIL|message
 * REGISTER_OK
 * REGISTER_FAIL|message
 * AUCTION_LIST|n   (sau đó n dòng: auctionId|itemName|currentPrice|status|endTime)
 * AUCTION_DETAIL|auctionId|itemName|desc|startPrice|currentPrice|status|endTime|sellerName
 * JOIN_OK|auctionId
 * JOIN_FAIL|message
 * LEAVE_OK|auctionId
 * BID_OK|auctionId|newPrice
 * BID_FAIL|message
 * LOGOUT_OK
 * ERROR|message
 * </pre>
 *
 * <p>Broadcast server tự push khi có sự kiện (đến các client đang JOIN phiên):
 * * <pre>
 * UPDATE_PRICE|auctionId|newPrice
 * AUCTION_STARTED|auctionId
 * AUCTION_ENDED|auctionId|winnerUsername
 * </pre>
 */
public class SocketServer {

    private static final Logger LOGGER = Logger.getLogger(SocketServer.class.getName());

    private static final int THREAD_POOL_SIZE = 20;
    private static final int SHUTDOWN_TIMEOUT = 5; // giây
    private final AuctionManager auctionManager;
    private final AuthService authService;

    // Singleton

    private static volatile SocketServer instance;

    /**
     * Lấy instance duy nhất của server.
     *
     * @param port              cổng lắng nghe kết nối
     * @param authService       service xác thực dùng chung của server
     * @param auctionManager    manager đấu giá dùng chung của server
     * @param auctionBidService service đặt giá dùng chung của server
     * @return instance duy nhất của {@link SocketServer}
     */
    public static SocketServer getInstance(
            final int port,
            final AuthService authService,
            final AuctionManager auctionManager,
            final AuctionBidService auctionBidService) {
        if (instance == null) {
            synchronized (SocketServer.class) {
                if (instance == null) {
                    instance = new SocketServer(
                            port,
                            auctionManager,
                            authService,
                            auctionBidService);
                }
            }
        }
        return instance;
    }

    // State

    private final int port;
    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private final AuctionBidService auctionBidService;

    private SocketServer(
            final int port,
            final AuctionManager auctionManager,
            final AuthService authService,
            final AuctionBidService auctionBidService) {
        this.port = port;
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.authService = Objects.requireNonNull(authService, "authService");
        this.auctionBidService = Objects.requireNonNull(auctionBidService, "auctionBidService");
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    // Lifecycle

    /**
     * Khởi động server: mở ServerSocket và bắt đầu vòng lặp chấp nhận kết nối.
     * Phương thức này block cho đến khi {@link #stop()} được gọi.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;

            LOGGER.info("Server khởi động trên cổng " + port);

            acceptLoop();

        } catch (IOException exception) {
            LOGGER.severe("Không thể khởi động server tại cổng "
                + port + ": "
                + exception.getMessage());
        } finally {
            stop();
        }
    }

    /**
     * Vòng lặp chính: chờ và chấp nhận kết nối từ client.
     * Mỗi client được xử lý bởi một {@link ClientHandler} chạy trên thread pool.
     */
    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                LOGGER.info("Client kết nối: " + clientSocket.getInetAddress());

                threadPool.execute(new ClientHandler(
                        clientSocket,
                        auctionManager,
                        authService,
                        auctionBidService));

            } catch (IOException exception) {
                if (running) {
                    LOGGER.warning("Lỗi khi chấp nhận kết nối: " + exception.getMessage());
                }
                // Nếu !running thì server đang dừng — bỏ qua lỗi
            }
        }
    }

    /**
     * Dừng server và giải phóng tài nguyên.
     * Thread pool sẽ hoàn thành các task đang chạy trong {@value #SHUTDOWN_TIMEOUT}
     * giây.
     */
    public void stop() {
        if (!running) {
            return;
        }
        running = false;

        // Đóng server socket để unblock serverSocket.accept()
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException exception) {
                LOGGER.warning("Lỗi khi đóng ServerSocket: " + exception.getMessage());
            }
        }

        // Đợi các ClientHandler hiện tại hoàn thành
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
                LOGGER.warning("Thread pool bị buộc dừng sau " + SHUTDOWN_TIMEOUT + " giây.");
            }
        } catch (InterruptedException exception) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOGGER.info("Server đã dừng.");
    }

    /**
     * Kiểm tra xem server có đang chạy hay không.
     *
     * @return true nếu server đang chạy, false nếu đã dừng.
     */
    public boolean isRunning() {
        return running;
    }

    // Entry point độc lập (không dùng JavaFX)

    /**
     * Khởi chạy server độc lập qua dòng lệnh.
     * Có thể truyền cổng qua tham số: {@code java SocketServer 6000}
     *
     * @param args Tham số dòng lệnh (phần tử đầu tiên có thể là số cổng cần lắng nghe).
     */
    public static void main(final String[] args) {
        int port = NetworkConfig.SERVER_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException exception) {
                System.err.println("Cổng không hợp lệ, dùng cổng mặc định "
                        + NetworkConfig.SERVER_PORT);
            }
        }
        final SerializedDatabase database = new SerializedDatabase(Path.of("data"));
        final AuctionBidService auctionBidService = new AuctionBidService(database);
        final AuctionManager auctionManager = AuctionManager.getInstance(database);
        final AuthService authService = new AuthService(database);

        final SocketServer socketServer = SocketServer.getInstance(
                port, 
                authService,
                auctionManager, 
                auctionBidService);

        socketServer.start();
    }
}