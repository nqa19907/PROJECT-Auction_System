package auction_system.server;

import java.io.IOException;
import java.util.logging.Logger;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.NetworkConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Lớp chính để khởi chạy ứng dụng máy chủ (Server) cho hệ thống đấu giá.
 */
public class ServerApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(ServerApp.class.getName());

    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int MIN_WINDOW_WIDTH = 960;

    @Override
    public void start(Stage stage) throws Exception {
        // Mở kết nối mạng
        try {
            NetworkClient.getInstance().connect(NetworkConfig.SERVER_HOST,
                                                NetworkConfig.SERVER_PORT);
        } catch (IOException e) {
            LOGGER.severe("Không thể kết nối tới Server: " + e.getMessage());

            // TODO: Hiện một bảng cảnh báo cho người dùng bình thường
            /* Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi Mạng");
            alert.setHeaderText("Không thể kết nối đến Máy chủ");
            alert.setContentText("Vui lòng kiểm tra lại đường truyền hoặc thử lại sau!");
            alert.showAndWait();
            */
        }


        // Load giao diện
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/fxml/dashboard.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setScene(scene);
        stage.setMinWidth(MIN_WINDOW_WIDTH);
        stage.setMinHeight(WINDOW_HEIGHT);
        stage.setTitle("App Thử");
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        NetworkClient.getInstance().disconnect();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
