package auction_system.client;

import auction_system.client.network.NetworkClient;
import auction_system.client.utils.ViewConstants;
import auction_system.client.utils.WindowTitleUtil;
import auction_system.common.network.NetworkConfig;
import java.io.IOException;
import java.util.Map;
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
    private static final int windowWidth = 900;
    private static final int windowHeight = 700;
    private static final int minimumWindowWidth = 900;

    private String serverHost;
    private int serverPort;

    /**
     * Khởi chạy giao diện client.
     *
     * @param stage cửa sổ chính của ứng dụng JavaFX
     * @throws Exception nếu không thể nạp giao diện
     */
    @Override
    public void start(final Stage stage) throws Exception {
        try {
            resolveServerAddress();
            try {
                NetworkClient.getInstance().connect(serverHost, serverPort);
            } catch (IOException e) {
                LOGGER.error("Không thể kết nối tới server {}:{}.", serverHost, serverPort);
                throw e;
            }

            loadFonts();
            loadLoginScene(stage);
        } catch (Exception exception) {
            LOGGER.error("Khởi chạy JavaFX thất bại.", exception);
            throw exception;
        }
    }

    /**
     * Xác định địa chỉ server từ JavaFX args, system property/env hoặc mặc định.
     */
    private void resolveServerAddress() {
        final Map<String, String> namedArgs = getParameters().getNamed();
        serverHost = namedArgs.getOrDefault("server-host", NetworkConfig.SERVER_HOST);
        serverPort = parsePort(namedArgs.get("server-port"), NetworkConfig.SERVER_PORT);
    }

    /**
     * Parse cổng kết nối, nếu sai thì giữ nguyên cổng mặc định.
     *
     * @param rawPort giá trị cổng dạng chuỗi
     * @param defaultPort cổng mặc định
     * @return cổng hợp lệ để kết nối
     */
    private int parsePort(final String rawPort, final int defaultPort) {
        if (rawPort == null || rawPort.isBlank()) {
            return defaultPort;
        }

        try {
            return Integer.parseInt(rawPort);
        } catch (NumberFormatException exception) {
            LOGGER.warn("Cổng server không hợp lệ '{}', dùng cổng {}", rawPort, defaultPort);
            return defaultPort;
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
        WindowTitleUtil.applyTitle(stage, ViewConstants.LOGIN_VIEW);
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
