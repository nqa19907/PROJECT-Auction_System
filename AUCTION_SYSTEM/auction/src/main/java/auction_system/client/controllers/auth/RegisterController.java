package auction_system.client.controllers.auth;

import auction_system.client.utils.ViewConstants;
import auction_system.common.utils.SecurityUtils;
import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller xử lý các sự kiện trên giao diện đăng ký.
 */
public class RegisterController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterController.class);

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    private void handleRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
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

        // 1. Mã hóa mật khẩu thành SHA-256 trước khi gửi
        String hashedPassword = SecurityUtils.hashPassword(password);
        LOGGER.info("Sẵn sàng gửi lệnh Đăng ký: " + name + " | "
                    + email + " | Hash: " + hashedPassword);
        
        // TODO: Viết thêm hàm AuthService.getInstance().register(...) để gửi Socket lên Server.
        // Tạm thời vẫn điều hướng để test giao diện
        navigateTo(ViewConstants.LOGIN_VIEW, "Đăng nhập");
    }

    @FXML
    private void handleGoToLogin() {
        navigateTo(ViewConstants.LOGIN_VIEW, "Đăng nhập");
    }

    private void navigateTo(String path, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (IOException e) {
            LOGGER.error("Lỗi điều hướng: " + e.getMessage(), e);
        }
    }

    private void showError(String msg) {
        if (errorLabel != null) {
            errorLabel.setText(msg);
            errorLabel.setVisible(true);
        }
    }
}