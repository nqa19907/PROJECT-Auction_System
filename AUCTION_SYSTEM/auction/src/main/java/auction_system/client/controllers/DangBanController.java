package auction_system.client.controllers;

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

/**
 * Controller for the DangBan screen.
 */
public class DangBanController implements Initializable {

    @FXML private TextField fieldTenTaiSan;
    @FXML private ComboBox<String> comboDanhMuc;
    @FXML private ComboBox<String> comboTinhTrang;
    @FXML private TextArea fieldMoTa;
    @FXML private TextField fieldThoiGianBatDau;
    @FXML private TextField fieldThoiGianKetThuc;
    @FXML private TextField fieldGiaKhoiDiem;
    @FXML private TextField fieldBuocGia;
    @FXML private Label lblError;
    @FXML private Button btnHuy;
    @FXML private Button btnTiepTheo;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        comboDanhMuc.getItems().addAll(
                "Đồng hồ & Trang sức", "Nghệ thuật", "Cổ vật", "Phương tiện", "Điện tử"
        );
        comboTinhTrang.getItems().addAll(
                "Mới (100%)", "Như mới (95%+)", "Tốt (80–94%)", "Trung bình"
        );
    }

    @FXML
    private void handleGoDashboard(ActionEvent event) {
        try {
            BorderPane mainLayout = (BorderPane) btnHuy.getScene().getRoot();
            Node view = FXMLLoader.load(
                    getClass().getResource("/client/fxml/dashboardContent.fxml")
            );
            mainLayout.setCenter(view);
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