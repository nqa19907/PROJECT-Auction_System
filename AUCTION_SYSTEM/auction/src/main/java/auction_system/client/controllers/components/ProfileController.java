package auction_system.client.controllers.components;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller điều khiển giao thông tin cá nhân (thanh bên phải).
 */
public class ProfileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileController.class);

    @FXML
    private TextField fieldDeposit;

    @FXML
    private Button btnDeposit;

    @FXML
    private void handleDeposit(ActionEvent event) {
        String amountStr = fieldDeposit.getText();
        LOGGER.info("Xử lý nạp tiền, số tiền: " + amountStr);
        // TODO: Viết logic validate dữ liệu và gọi AuthService để xử lý nạp tiền
    }
}