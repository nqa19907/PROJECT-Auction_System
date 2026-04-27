package auction_system.client.controllers;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.io.IOException;

public class RegisterController {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleRegister() {
        String name     = nameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Vui lòng điền đầy đủ thông tin.");
            return;
        }
        if (!email.contains("@")) {
            showError("Email không hợp lệ.");
            return;
        }
        if (password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự.");
            return;
        }

        System.out.println("Đăng ký: " + name + " | " + email);
        navigateTo("/fxml/login.fxml", "Đăng nhập");
    }

    @FXML
    private void handleGoToLogin() {
        navigateTo("/fxml/login.fxml", "Đăng nhập");
    }

    private void navigateTo(String path, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            Stage stage = (Stage) nameField.getScene().getWindow();
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