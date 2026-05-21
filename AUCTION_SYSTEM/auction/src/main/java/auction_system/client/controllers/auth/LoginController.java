package auction_system.client.controllers.auth;

import auction_system.client.services.AuthService;
import auction_system.client.utils.SceneManager;
import auction_system.client.utils.ViewConstants;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the Login screen.
 */
public class LoginController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    // --- CÁC HẰNG SỐ THÔNG BÁO ---
    private static final String MSG_ERR_EMPTY_EMAIL = "Vui lòng nhập Email!";
    private static final String MSG_ERR_EMPTY_PASSWORD = "Vui lòng nhập Mật khẩu!";
    private static final String BTN_TEXT_LOADING = "Đang xử lý...";
    private static final String BTN_TEXT_DEFAULT = "Đăng nhập";

    // --- CÁC THÀNH PHẦN GIAO DIỆN ---

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
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
        SceneManager.switchScene((Node) event.getSource(), ViewConstants.DASHBOARD_VIEW);
    }

    /**
     * Focus vào ô mật khẩu khi nhấn Enter nếu đang ở hộp Email.
     */
    @FXML
    private void handleEmailEnter() {
        passwordField.requestFocus();
    }

    /**
     * Hàm xử lý đăng nhập.
     *
     * @param event Sự kiện ActionEvent khi nhấn nút.
     */
    @FXML
    private void handleLogin(ActionEvent event) {
        // Dùng trim() để xoá khoảng trắng thừa ở đầu/cuối chuỗi tránh lỗi đăng nhập
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        LOGGER.info("Bắt đầu xử lý đăng nhập cho user: " + email);

        if (email.isEmpty()) {
            showError(MSG_ERR_EMPTY_EMAIL);
            emailField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showError(MSG_ERR_EMPTY_PASSWORD);
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
                    SceneManager.switchScene(loginButton, ViewConstants.DASHBOARD_VIEW);
                } else {
                    showError(result.getErrorMessage());
                }
            });
        });
    }

    private void setLoadingState(boolean isLoading) {
        loginButton.setDisable(isLoading);
        loginButton.setText(isLoading ? BTN_TEXT_LOADING : BTN_TEXT_DEFAULT);
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
        LOGGER.warn("Đăng nhập thất bại: " + message);
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
        }
    }
}