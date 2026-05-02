package auction_system.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
<<<<<<< HEAD
<<<<<<< HEAD
 * Hello world!
 *
=======
 * Lớp chính để khởi chạy ứng dụng máy chủ (Server) cho hệ thống đấu giá.
>>>>>>> main
=======
 * Lớp chính để khởi chạy ứng dụng máy chủ (Server) cho hệ thống đấu giá.
>>>>>>> b0fb4130f2604c301b7c387274233639f63a848d
 */
public class ServerApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/dashboard.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("App Thử");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
