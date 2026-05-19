package auction_system.client.controllers;

import auction_system.common.constants.AppConstants;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the Publish Item screen.
 */
public class PublishItemController implements Initializable {

    @FXML
    private TextField fieldTenTaiSan;
    @FXML
    private ComboBox<String> comboDanhMuc;
    @FXML
    private ComboBox<String> comboTinhTrang;
    @FXML
    private TextArea fieldMoTa;
    @FXML
    private TextField fieldThoiGianBatDau;
    @FXML
    private TextField fieldThoiGianKetThuc;
    @FXML
    private TextField fieldGiaKhoiDiem;
    @FXML
    private TextField fieldBuocGia;
    @FXML
    private Label lblError;
    @FXML
    private Button btnHuy;
    @FXML
    private Button btnTiepTheo;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboDanhMuc.getItems().addAll(
                "Nghệ thuật", "Đồ điện tử", "Phương tiện giao thông");
        comboTinhTrang.getItems().addAll(
                "Mới (100%)", "Như mới (95%+)", "Tốt (80–94%)", "Trung bình");
    }

    @FXML
    private void handleGoDashboard(ActionEvent event) {
        try {
            BorderPane mainLayout = (BorderPane) btnHuy.getScene().getRoot();
            Node view = FXMLLoader.load(
                    getClass().getResource("/client/fxml/ItemList.fxml"));

            // Tìm đến hộp chứa content bằng id mà chúng ta vừa đặt, sau đó thay đổi giao
            // diện con bên trong
            StackPane contentArea = (StackPane) mainLayout.lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            } else {
                mainLayout.setCenter(view);
            }

            // Reset lại hiệu ứng active trên Sidebar về chữ "Tất cả"
            VBox sidebar = (VBox) mainLayout.lookup("#categorySidebar");
            if (sidebar != null) {
                for (Node node : sidebar.getChildren()) {
                    node.getStyleClass().remove("active");
                    if (AppConstants.UI_ID_CATEGORY_ALL.equals(node.getId())) {
                        node.getStyleClass().add("active");
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleHuy(ActionEvent event) {
        handleGoDashboard(event);
    }

    @FXML
    private void handleTiepTheo(ActionEvent event) {
        // TODO: validate rồi chuyển sang bước 2
    }
}