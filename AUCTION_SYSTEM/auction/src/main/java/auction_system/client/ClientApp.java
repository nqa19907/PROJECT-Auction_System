package auction_system.client;

import auction_system.client.network.NetworkClient;
import auction_system.client.utils.ViewConstants;
import auction_system.common.network.NetworkConfig;
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
    private static final int windowWidth = 900;
    private static final int windowHeight = 700;
    private static final int minimumWindowWidth = 900;

    /**
     * Khởi chạy giao diện client.
     *
     * @param stage cửa sổ chính của ứng dụng JavaFX
     * @throws Exception nếu không thể nạp giao diện
     */
    @Override
    public void start(final Stage stage) throws Exception {
        try {
            NetworkClient.getInstance().connect(
                NetworkConfig.SERVER_HOST,
                NetworkConfig.SERVER_PORT
            );

            loadFonts();
            loadLoginScene(stage);
        } catch (Exception exception) {
            LOGGER.error("Khởi chạy JavaFX thất bại.", exception);
            throw exception;
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
