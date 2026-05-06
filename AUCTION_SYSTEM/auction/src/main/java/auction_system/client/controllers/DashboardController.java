package auction_system.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Controller điều khiển giao diện Dashboard chính của người dùng.
 */
public class DashboardController {

    @FXML
    private Button btnSignIn;

    @FXML
    private void handleSignIn(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/client/fxml/Login.fxml")
            );
            Parent root = loader.load();

            Stage loginStage = new Stage();
            loginStage.setTitle("Đăng nhập");
            loginStage.setScene(new Scene(root));
            loginStage.setResizable(false);
            loginStage.initModality(Modality.APPLICATION_MODAL);
            loginStage.initOwner(btnSignIn.getScene().getWindow());
            loginStage.initStyle(StageStyle.UNDECORATED);
            loginStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}