package auction_system.client.controllers.auth;

import auction_system.client.network.dto.AuthResult;
import auction_system.client.services.AuthService;
import auction_system.client.utils.ViewConstants;
import java.io.IOException;
import javafx.application.Platform;
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

    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);
    private static final String defaultRoleName = "PARTICIPANT";

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    /**
     * Chuyển focus sang ô email khi nhấn Enter ở ô tên người dùng.
     */
    @FXML
    private void handleNameEnter() {
        emailField.requestFocus();
    }

    /**
     * Chuyển focus sang ô mật khẩu khi nhấn Enter ở ô email.
     */
    @FXML
    private void handleEmailEnter() {
        passwordField.requestFocus();
    }

    /**
     * Xử lý sự kiện đăng ký tài khoản.
     */
    @FXML
    private void handleRegister() {
        final String username = nameField.getText().trim();
        final String email = emailField.getText().trim();
        final String password = passwordField.getText();

        if (!validateInput(username, email, password)) {
            return;
        }

        hideError();
        logger.info("Gửi yêu cầu đăng ký tài khoản: " + username + " | " + email);

        AuthService.getInstance().register(
                username,
                email,
                password,
                defaultRoleName,
                result -> Platform.runLater(() -> handleRegisterResult(result)));
    }

    /**
     * Xử lý sự kiện quay lại màn đăng nhập.
     */
    @FXML
    private void handleGoToLogin() {
        navigateTo(ViewConstants.LOGIN_VIEW, "Đăng nhập");
    }

    /**
     * Kiểm tra dữ liệu người dùng nhập trước khi gửi lên server.
     *
     * @param username tên đăng nhập
     * @param email email đăng ký
     * @param password mật khẩu đăng ký
     * @return true nếu dữ liệu hợp lệ
     */
    private boolean validateInput(
            final String username,
            final String email,
            final String password) {
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Vui lòng điền đầy đủ thông tin.");
            return false;
        }

        if (!email.contains("@")) {
            showError("Email không hợp lệ.");
            return false;
        }

        if (password.length() < 6) {
            showError("Mật khẩu phải có ít nhất 6 ký tự.");
            return false;
        }

        return true;
    }

    /**
     * Xử lý kết quả đăng ký trả về từ server.
     *
     * @param result kết quả đăng ký
     */
    private void handleRegisterResult(final AuthResult result) {
        if (result.isSuccess()) {
            logger.info("Đăng ký thành công, chuyển về màn đăng nhập.");
            navigateTo(ViewConstants.LOGIN_VIEW, "Đăng nhập");
            return;
        }

        showError(result.getErrorMessage());
    }

    /**
     * Chuyển sang màn hình khác.
     *
     * @param path đường dẫn FXML
     * @param title tiêu đề cửa sổ
     */
    private void navigateTo(final String path, final String title) {
        try {
            final FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            final Parent root = loader.load();
            final Stage stage = (Stage) nameField.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle(title);
        } catch (IOException exception) {
            logger.error("Lỗi điều hướng: " + exception.getMessage(), exception);
        }
    }

    /**
     * Ẩn nhãn lỗi trên giao diện.
     */
    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    /**
     * Hiển thị lỗi trên giao diện.
     *
     * @param message nội dung lỗi
     */
    private void showError(final String message) {
        logger.warn("Đăng ký thất bại: " + message);

        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
}
