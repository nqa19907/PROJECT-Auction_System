package auction_system;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Hello world!
 *
 */
public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader=new FXMLLoader(getClass().getResource("/dashboard.fxml"));
        Parent root=loader.load();
        Scene scene=new Scene(root,800,600);
        stage.setScene(scene);
        stage.setTitle("App Thử");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
