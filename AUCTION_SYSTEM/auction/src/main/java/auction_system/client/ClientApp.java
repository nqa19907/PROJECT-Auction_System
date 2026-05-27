package auction_system.client;

import auction_system.client.network.NetworkClient;
import auction_system.client.utils.ViewConstants;
import auction_system.common.models.users.Participant;
import auction_system.common.network.NetworkConfig;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.SocketServer;
import auction_system.server.persistence.serialization.DatabasePathProvider;
import auction_system.server.persistence.serialization.SerializedDatabase;
import auction_system.server.services.auction.ParticipantItemService;
import auction_system.server.services.auth.AuthService;
import auction_system.server.services.autobid.AutoBidService;
import auction_system.server.services.bidding.AuctionBidService;
import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp chính để khởi chạy ứng dụng Client.
 *
 * <p>Lớp này ưu tiên kết nối tới server có sẵn. Nếu không kết nối được, ứng dụng
 * sẽ tự khởi chạy một server nội bộ phục vụ môi trường chạy thử local.
 */
public class ClientApp extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientApp.class);
    private static final int windowWidth = 900;
    private static final int windowHeight = 700;
    private static final int minimumWindowWidth = 900;
    private static final int serverStartupDelayMillis = 1000;

    private SocketServer localServer;

    /**
     * Khởi chạy giao diện client.
     *
     * @param stage cửa sổ chính của ứng dụng JavaFX
     * @throws Exception nếu không thể nạp giao diện
     */
    @Override
    public void start(final Stage stage) throws Exception {
        try {
            try {
                NetworkClient.getInstance().connect(
                    NetworkConfig.SERVER_HOST,
                    NetworkConfig.SERVER_PORT
                );
            } catch (IOException e) {
                LOGGER.warn("Không thể kết nối tới server. Đang khởi chạy server nội bộ...");
                startLocalServerAndConnect();
            }

            loadFonts();
            loadLoginScene(stage);
        } catch (Exception exception) {
            LOGGER.error("Khởi chạy JavaFX thất bại.", exception);
            throw exception;
        }
    }

    /**
     * Tự động khởi chạy server nội bộ trên một luồng daemon và kết nối lại.
     *
     * <p>Phần này được phép dùng class server vì nó đang đóng vai trò boot local
     * server cho môi trường chạy thử. Controller phía client vẫn không được gọi
     * trực tiếp service server khi đăng nhập.
     */
    private void startLocalServerAndConnect() {
        final SerializedDatabase database = new SerializedDatabase(
            DatabasePathProvider.defaultDataDirectory());
        final int port = NetworkConfig.SERVER_PORT;
        final AuctionManager auctionManager = AuctionManager.getInstance(database);
        final AuthService authService = new AuthService(database);
        final AutoBidService autoBidService = new AutoBidService(database.autoBidSettings());
        final AuctionBidService auctionBidService =
                new AuctionBidService(database, auctionManager, autoBidService);
        final ParticipantItemService participantItemService = new ParticipantItemService(database);

        localServer = SocketServer.getInstance(
            port,
            authService,
            auctionManager,
            autoBidService,
            auctionBidService,
            participantItemService
        );

        final Thread serverThread = new Thread(localServer::start);
        serverThread.setDaemon(true);
        serverThread.start();

        reconnectToLocalServer();
    }

    /**
     * Kết nối lại tới server nội bộ sau khi server được khởi chạy.
     */
    private void reconnectToLocalServer() {
        try {
            Thread.sleep(serverStartupDelayMillis);
            NetworkClient.getInstance().connect(
                NetworkConfig.SERVER_HOST,
                NetworkConfig.SERVER_PORT
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Luồng chờ kết nối lại server nội bộ bị gián đoạn.");
        } catch (IOException e) {
            LOGGER.error("Kết nối lại server nội bộ thất bại: {}", e.getMessage());
        }
    }

    /**
     * Nạp màn hình đăng nhập.
     *
     * @param stage cửa sổ chính của ứng dụng JavaFX
     * @throws IOException nếu không thể đọc file FXML
     */
    private void loadLoginScene(final Stage stage) throws IOException {
        final FXMLLoader loader = new FXMLLoader(
            getClass().getResource(ViewConstants.LOGIN_VIEW)
        );
        final Parent root = loader.load();
        final Scene scene = new Scene(root, windowWidth, windowHeight);

        stage.setScene(scene);
        stage.setMinWidth(minimumWindowWidth);
        stage.setMinHeight(windowHeight);
        stage.setTitle("Đăng nhập - AuctionHub");
        stage.show();
    }

    /**
     * Nạp các font chữ dùng trong giao diện.
     */
    private void loadFonts() {
        final String[] fontPaths = {
            "/fonts/GoogleSansCode/GoogleSansCode-Medium.ttf",
            "/fonts/GoogleSans/GoogleSans-Regular.ttf",
            "/fonts/GoogleSans/GoogleSans-Medium.ttf",
            "/fonts/GoogleSans/GoogleSans-Bold.ttf",
            "/fonts/GoogleSans/GoogleSans-Italic.ttf"
        };

        for (final String fontPath : fontPaths) {
            if (getClass().getResource(fontPath) != null) {
                Font.loadFont(getClass().getResourceAsStream(fontPath), 16);
            } else {
                LOGGER.warn("Không tìm thấy font tại đường dẫn: {}", fontPath);
            }
        }
    }

    /**
     * Dọn tài nguyên khi đóng ứng dụng.
     *
     * @throws Exception nếu JavaFX gặp lỗi khi dừng ứng dụng
     */
    @Override
    public void stop() throws Exception {
        // Ngắt kết nối khi đóng ứng dụng
        NetworkClient.getInstance().disconnect();

        // Tắt server nội bộ nếu nó đang chạy ngầm
        if (localServer != null && localServer.isRunning()) {
            localServer.stop();
        }

        super.stop();
        // Ép JVM tắt hoàn toàn, dọn sạch mọi Thread còn sót lại trên RAM
        System.exit(0);
    }

    /**
     * Điểm bắt đầu của ứng dụng client.
     *
     * @param args tham số dòng lệnh
     */
    public static void main(final String[] args) {
        launch(args);
    }
}
