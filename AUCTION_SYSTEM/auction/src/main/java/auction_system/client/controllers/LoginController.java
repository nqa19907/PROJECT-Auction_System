package auction_system.client.controllers;

import auction_system.client.network.NetworkClient;
import auction_system.client.services.AuthService;
import auction_system.client.utils.SceneManager;
import auction_system.common.network.Protocol;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Controller for the Login screen.
 */
public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    // --- CÁC THÀNH PHẦN GIAO DIỆN ---

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Label forgetLabel;
    @FXML private Button loginButton;
    @FXML private Button googleButton;
    @FXML private HBox registerLabel;

    // --- PHƯƠNG THỨC KHỞI TẠO ---
    @FXML
    public void initialize() {
        hideError();
        LOGGER.info("Giao diện Login đã được nạp thành công.");
    }

    // --- CÁC SỰ KIỆN ---
    
    @FXML
    private void handleBackToDashboard(MouseEvent event) {
        LOGGER.info("Quay về trang chủ Dashboard.");
        SceneManager.switchScene((Node) event.getSource(), "dashboard.fxml");
    }

    /**
     * Hàm xử lý đăng nhập.
     *
     * @param event Sự kiện ActionEvent khi nhấn nút.
     */
    @FXML
    private void handleLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        LOGGER.info("Bắt đầu xử lý đăng nhập cho user: " + email);

        if (email.isEmpty()) {
            showError("Vui lòng nhập Email!");
            emailField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError("Vuil lòng nhập Mật khẩu!");
            passwordField.requestFocus();
            return;
        }

        hideError();
        setLoadingState(true);

        // Gọi qua tầng Service bằng luồng Callback bất đồng bộ
        AuthService.getInstance().login(email, password, result -> {
            Platform.runLater(() -> {
                setLoadingState(false);
                
                if (result.isSuccess()) {
                    LOGGER.info("Đăng nhập thành công, điều hướng bằng SceneManager.");
                    SceneManager.switchScene(loginButton, "Dashboard.fxml");
                } else {
                    showError(result.getErrorMessage());
                }
            });
        });
    }

    private void setLoadingState(boolean isLoading) {
        loginButton.setDisable(isLoading);
        loginButton.setText(isLoading ? "Đang xử lý..." : "Đăng nhập");
        emailField.setDisable(isLoading);
        passwordField.setDisable(isLoading);
    }

    private void hideError() {
        if (errorLabel != null) {
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    /**
     * Phương thức hiển thị lỗi, ghi log cảnh báo.
     *
     * @param message Nội dung thông báo lỗi.
     */
    private void showError(String message) {
        LOGGER.warning("Đăng nhập thất bại: " + message);
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }

    @FXML
    private void handleGoogleLogin() {
        // TODO: Xử lý logic đăng nhập qua Google
    }
}