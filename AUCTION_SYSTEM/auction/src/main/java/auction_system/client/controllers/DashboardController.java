package auction_system.client.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

/**
 * Controller điều khiển giao diện Dashboard chính của người dùng.
 */
public class DashboardController {
    @FXML
    private Button btnSignIn;

    @FXML
    private void handleSignIn() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/fxml/Login.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) btnSignIn.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }

}
