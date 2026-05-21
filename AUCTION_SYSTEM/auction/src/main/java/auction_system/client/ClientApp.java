package auction_system.client;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.NetworkConfig;
import auction_system.server.network.SocketServer;
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
 */
public class ClientApp extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientApp.class);

    private static final int WINDOW_WIDTH = 900;
    private static final int WINDOW_HEIGHT = 700;
    private static final int MIN_WINDOW_WIDTH = 900;

    @Override
    public void start(Stage stage) throws Exception {
        // Mở kết nối mạng tới Server
        try {
            NetworkClient.getInstance().connect(NetworkConfig.SERVER_HOST,
                                                NetworkConfig.SERVER_PORT);
        } catch (IOException e) {
            LOGGER.warn("Không thể kết nối tới Server. Đang tự động khởi chạy Server nội bộ...");
            startLocalServerAndConnect();
        }

        // Nạp font
        loadFonts();

        // Load giao diện đăng nhập
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/fxml/auth/Login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        
        stage.setScene(scene);
        stage.setMinWidth(MIN_WINDOW_WIDTH);
        stage.setMinHeight(WINDOW_HEIGHT);
        stage.setTitle("Đăng nhập - AuctionHub");
        stage.show();
    }

    /**
     * Tự động khởi chạy Server trên một luồng chạy ngầm (Daemon Thread) 
     * và kết nối lại sau khi khởi động xong.
     */
    private void startLocalServerAndConnect() {
        Thread serverThread = new Thread(() -> {
            SocketServer.getInstance().start();
        });
        serverThread.setDaemon(true); // Đảm bảo server tự tắt khi client đóng
        serverThread.start();

        try {
            Thread.sleep(1000); // Chờ 1 giây để Server khởi động xong và mở cổng
            NetworkClient.getInstance().connect(NetworkConfig.SERVER_HOST,
                                                NetworkConfig.SERVER_PORT);
        } catch (InterruptedException | IOException ex) {
            LOGGER.error("Thử kết nối lại với Server nội bộ thất bại: " + ex.getMessage());
        }
    }

    private void loadFonts() {
        String[] fontPaths = {
            "/fonts/GoogleSansCode/GoogleSansCode-Medium.ttf",
            "/fonts/GoogleSans/GoogleSans-Regular.ttf",
            "/fonts/GoogleSans/GoogleSans-Medium.ttf",
            "/fonts/GoogleSans/GoogleSans-Bold.ttf",
            "/fonts/GoogleSans/GoogleSans-Italic.ttf"
        };

        for (String fontPath : fontPaths) {
            if (getClass().getResource(fontPath) != null) {
                Font.loadFont(getClass().getResourceAsStream(fontPath), 16);
            } else {
                LOGGER.warn("Không tìm thấy font tại đường dẫn: " + fontPath);
            }
        }
    }

    @Override
    public void stop() throws Exception {
        // Ngắt kết nối khi đóng ứng dụng
        NetworkClient.getInstance().disconnect();

        // Tắt server nội bộ nếu nó đang chạy ngầm
        if (SocketServer.getInstance().isRunning()) {
            SocketServer.getInstance().stop();
        }

        super.stop();
        // Ép JVM tắt hoàn toàn, dọn sạch mọi Thread còn sót lại trên RAM
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
