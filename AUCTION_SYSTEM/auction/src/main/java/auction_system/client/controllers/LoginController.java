package auction_system.client.controllers;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller xử lý các sự kiện trên giao diện đăng nhập.
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập email và mật khẩu.");
            return;
        }

        if (email.equals("admin@auction.com") && password.equals("123456")) {
            navigateTo("/fxml/dashboard.fxml", "Dashboard");
        } else {
            showError("Email hoặc mật khẩu không đúng.");
        }
    }

    @FXML
    private void handleGoToRegister() {
        navigateTo("/fxml/register.fxml", "Đăng ký");
    }

    @FXML
    private void handleForgotPassword() {
        showError("Tính năng đang phát triển.");
    }

    private void navigateTo(String path, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (IOException e) {
            System.err.println("Lỗi: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setText(msg);
            errorLabel.setVisible(true);
        }
    }
}
