package auction_system.client.controllers.auction;

import auction_system.client.utils.Router;
import auction_system.client.utils.ViewConstants;
import auction_system.common.constants.AppConstants;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the Publish Item screen.
 */
public class PublishItemController implements Initializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishItemController.class);

    @FXML private TextField fieldTenTaiSan;
    @FXML private ComboBox<String> comboCategory;
    @FXML private ComboBox<String> comboCondition;
    @FXML private TextArea fieldDescription;
    @FXML private TextField fieldStartingTime;
    @FXML private TextField fieldEndingTime;
    @FXML private TextField fieldStartingPrice;
    @FXML private TextField fieldBidStep;
    @FXML private Label lblError;
    @FXML private Button btnCancel;
    @FXML private Button btnConfirm;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboCategory.getItems().addAll(
                AppConstants.CATEGORY_ART, 
                AppConstants.CATEGORY_ELECTRONIC, 
                AppConstants.CATEGORY_VEHICLE
        );
        comboCondition.getItems().addAll(
                "Mới (100%)", "Như mới (95%+)", "Tốt (80–94%)", "Trung bình");
    }

    @FXML
    private void handleGoDashboard(ActionEvent event) {
        Router.navigateContent(btnCancel, ViewConstants.ITEM_LIST_VIEW);
        Router.updateSidebarActive(btnCancel, AppConstants.UI_ID_CATEGORY_ALL);
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        handleGoDashboard(event);
    }

    @FXML
    private void handleConfirm(ActionEvent event) {
        // TODO: xác nhận
    }
}