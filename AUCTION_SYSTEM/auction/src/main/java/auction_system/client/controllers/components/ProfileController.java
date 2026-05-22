package auction_system.client.controllers.components;

import auction_system.client.services.WalletService;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller điều khiển giao thông tin cá nhân (thanh bên phải).
 */
public class ProfileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileController.class);
    private static final String NUMBER_GROUP_SEPARATOR_REGEX = "[,.\\s]";
    private static final String STYLE_DEPOSIT_SUCCESS = "deposit-message-success";
    private static final String STYLE_DEPOSIT_ERROR = "deposit-message-error";
    private static final String STYLE_DEPOSIT_INFO = "deposit-message-info";
    private static final Duration DEPOSIT_MESSAGE_DURATION = Duration.seconds(5);

    private Participant currentParticipant;
    private PauseTransition depositMessageTimer;

    @FXML
    private TextField fieldDeposit;
    
    @FXML private Label lblUsername;
    @FXML private Label lblEmail;
    @FXML private Label lblRole;
    @FXML private Label lblBalance;
    @FXML private Label lblDepositMessage;
    @FXML private Button btnDeposit;
    @FXML private Region profileDivider;
    @FXML private VBox walletSection;

    @FXML
    private void initialize() {
        depositMessageTimer = new PauseTransition(DEPOSIT_MESSAGE_DURATION);
        depositMessageTimer.setOnFinished(event -> hideDepositMessage());
        hideDepositMessage();
        if (fieldDeposit != null) {
            fieldDeposit.setOnAction(this::handleDeposit);
        }
    }

    /**
     * Đổ dữ liệu người dùng lên giao diện Profile.
     * Sử dụng tính đa hình để lấy tên hiển thị vai trò.
     *
     * @param user Đối tượng người dùng (có thể là Admin hoặc Participant).
     */
    public void setUserData(User user) {
        if (user == null) {
            return;
        }

        lblUsername.setText(user.getUsername());
        lblEmail.setText(user.getEmail());
        lblRole.setText(user.getRoleDisplayName());

        if (user instanceof Participant participant) {
            currentParticipant = participant;
            walletSection.setVisible(true);
            walletSection.setManaged(true);
            profileDivider.setVisible(true);
            profileDivider.setManaged(true);
            updateBalanceLabel(participant.getBalance());
        } else {
            currentParticipant = null;
            // Ẩn hoàn toàn phần Ví nếu là Admin
            walletSection.setVisible(false);
            walletSection.setManaged(false);
            profileDivider.setVisible(false);
            profileDivider.setManaged(false);
        }
    }

    @FXML
    private void handleDeposit(ActionEvent event) {
        String amountStr = fieldDeposit.getText();
        LOGGER.info("Xử lý nạp tiền, số tiền: " + amountStr);

        if (currentParticipant == null) {
            showDepositMessage(
                    "Không thể nạp tiền cho tài khoản này.",
                    STYLE_DEPOSIT_ERROR);
            return;
        }

        double amount = parseDepositAmount(amountStr);
        if (amount <= 0) {
            showDepositMessage(
                    "Vui lòng nhập số tiền nạp lớn hơn 0.",
                    STYLE_DEPOSIT_ERROR);
            fieldDeposit.requestFocus();
            return;
        }

        setDepositLoading(true);
        showDepositMessage("Đang xử lý nạp tiền...", STYLE_DEPOSIT_INFO);
        WalletService.getInstance().deposit(amount, (isSuccess, message, balance) -> {
            setDepositLoading(false);

            if (isSuccess) {
                currentParticipant.setBalance(balance);
                updateBalanceLabel(balance);
                fieldDeposit.clear();
                LOGGER.info("Nạp tiền thành công. Số dư mới: {}", balance);
                showDepositMessage(message, STYLE_DEPOSIT_SUCCESS);
                return;
            }

            showDepositMessage(message, STYLE_DEPOSIT_ERROR);
            fieldDeposit.requestFocus();
        });
    }

    private double parseDepositAmount(String amountStr) {
        if (amountStr == null || amountStr.isBlank()) {
            return 0;
        }

        String normalizedAmount =
            amountStr.trim().replaceAll(NUMBER_GROUP_SEPARATOR_REGEX, "");
        try {
            return Double.parseDouble(normalizedAmount);
        } catch (NumberFormatException e) {
            LOGGER.warn("Số tiền nạp không hợp lệ: {}", amountStr);
            return 0;
        }
    }

    private void updateBalanceLabel(double balance) {
        lblBalance.setText(String.format("%,.0f", balance));
    }

    private void setDepositLoading(boolean isLoading) {
        btnDeposit.setDisable(isLoading);
        fieldDeposit.setDisable(isLoading);
        btnDeposit.setText(isLoading ? "Đang nạp..." : "Nạp tiền");
    }

    private void showDepositMessage(String message, String styleClass) {
        if (lblDepositMessage == null) {
            return;
        }

        stopDepositMessageTimer();
        lblDepositMessage.setText(message);
        lblDepositMessage.getStyleClass().removeAll(
                STYLE_DEPOSIT_SUCCESS,
                STYLE_DEPOSIT_ERROR,
                STYLE_DEPOSIT_INFO);

        if (styleClass != null && !lblDepositMessage.getStyleClass().contains(styleClass)) {
            lblDepositMessage.getStyleClass().add(styleClass);
        }

        lblDepositMessage.setVisible(true);
        lblDepositMessage.setManaged(true);

        if (!STYLE_DEPOSIT_INFO.equals(styleClass)) {
            depositMessageTimer.playFromStart();
        }
    }

    private void hideDepositMessage() {
        if (lblDepositMessage == null) {
            return;
        }

        stopDepositMessageTimer();
        lblDepositMessage.setText("");
        lblDepositMessage.setVisible(false);
        lblDepositMessage.setManaged(false);
        lblDepositMessage.getStyleClass().removeAll(
                STYLE_DEPOSIT_SUCCESS,
                STYLE_DEPOSIT_ERROR,
                STYLE_DEPOSIT_INFO);
    }

    private void stopDepositMessageTimer() {
        if (depositMessageTimer != null) {
            depositMessageTimer.stop();
        }
    }
}
