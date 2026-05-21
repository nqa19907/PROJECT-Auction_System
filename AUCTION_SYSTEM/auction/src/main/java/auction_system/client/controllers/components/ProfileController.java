package auction_system.client.controllers.components;

import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller điều khiển giao thông tin cá nhân (thanh bên phải).
 */
public class ProfileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileController.class);

    @FXML
    private TextField fieldDeposit;
    
    @FXML private Label lblUsername;
    @FXML private Label lblEmail;
    @FXML private Label lblRole;
    @FXML private Label lblBalance;
    @FXML private Button btnDeposit;
    @FXML private Region profileDivider;
    @FXML private VBox walletSection;

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
            walletSection.setVisible(true);
            walletSection.setManaged(true);
            profileDivider.setVisible(true);
            profileDivider.setManaged(true);
            lblBalance.setText(String.format("%,.0f", participant.getBalance()));
        } else {
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
        // TODO: Viết logic validate dữ liệu và gọi AuthService để xử lý nạp tiền
    }
}