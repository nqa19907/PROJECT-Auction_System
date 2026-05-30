package auction_system.server.network;

import auction_system.common.network.NetworkConfig;
import auction_system.server.core.AuctionManager;
import auction_system.server.persistence.serialization.DatabasePathProvider;
import auction_system.server.persistence.serialization.SerializedDatabase;
import auction_system.server.services.auction.ParticipantItemService;
import auction_system.server.services.auth.AuthService;
import auction_system.server.services.autobid.AutoBidService;
import auction_system.server.services.bidding.AuctionBidService;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server Socket chính của hệ thống đấu giá — Singleton.
 * Lắng nghe kết nối TCP từ client và tạo một {@link ClientHandler}
 * trên thread riêng cho mỗi kết nối mới.
 *
 * <p>Giao thức socket dùng JSON UTF-8, mỗi request, response hoặc notification nằm trên một dòng.
 * Request dùng trường {@code command}; response và notification dùng trường {@code type}.
 * Dữ liệu nghiệp vụ được đặt trong trường {@code payload}.
 */
public class SocketServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketServer.class);

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
     * @param autoBidService    service auto-bid dùng chung của server
     * @param auctionBidService service đặt giá dùng chung của server
     * @param participantItemService service xử lý item người dùng
     * @return instance duy nhất của {@link SocketServer}
     */
    public static SocketServer getInstance(
            final int port,
            final AuthService authService,
            final AuctionManager auctionManager,
            final AutoBidService autoBidService,
            final AuctionBidService auctionBidService,
            final ParticipantItemService participantItemService) {
        if (instance == null) {
            synchronized (SocketServer.class) {
                if (instance == null) {
                    instance = new SocketServer(
                            port,
                            auctionManager,
                            authService,
                            autoBidService,
                            auctionBidService,
                            participantItemService);
                }
            }
        }
        return instance;
    }

    // State

    private final int port;
    private volatile boolean running = false;
    private ServerSocket serverSocket;
    final ParticipantItemService participantItemService;
    private final ExecutorService threadPool;
    private final AuctionBidService auctionBidService;
    private final AutoBidService autoBidService;

    private SocketServer(
            final int port,
            final AuctionManager auctionManager,
            final AuthService authService,
            final AutoBidService autoBidService,
            final AuctionBidService auctionBidService,
            final ParticipantItemService participantItemService) {
        this.port = port;
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.authService = Objects.requireNonNull(authService, "authService");
        this.autoBidService = Objects.requireNonNull(autoBidService, "autoBidService");
        this.auctionBidService = Objects.requireNonNull(auctionBidService, "auctionBidService");
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.participantItemService = participantItemService;
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

        } catch (IOException e) {
            LOGGER.error("Không thể khởi động server tại cổng " + port + ": " + e.getMessage());
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
                        autoBidService,
                        auctionBidService,
                        participantItemService));

            } catch (IOException e) {
                if (running) {
                    LOGGER.warn("Lỗi khi chấp nhận kết nối: " + e.getMessage());
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
            } catch (IOException e) {
                LOGGER.warn("Lỗi khi đóng ServerSocket: " + e.getMessage());
            }
        }

        // Đợi các ClientHandler hiện tại hoàn thành
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
                LOGGER.warn("Thread pool bị buộc dừng sau " + SHUTDOWN_TIMEOUT + " giây.");
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
        final SerializedDatabase database = new SerializedDatabase(
                DatabasePathProvider.defaultDataDirectory());
        final AuctionManager auctionManager = AuctionManager.getInstance(database);
        final AutoBidService autoBidService = new AutoBidService(database.autoBidSettings());
        final AuctionBidService auctionBidService =
                new AuctionBidService(database, auctionManager, autoBidService);
        final AuthService authService = new AuthService(database);
        final ParticipantItemService participantItemService = new ParticipantItemService(database);

        final SocketServer socketServer = SocketServer.getInstance(
                port,
                authService,
                auctionManager,
                autoBidService,
                auctionBidService,
                participantItemService);

        socketServer.start();
    }
}
