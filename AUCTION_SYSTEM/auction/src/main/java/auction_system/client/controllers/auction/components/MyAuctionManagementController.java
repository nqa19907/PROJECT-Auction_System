package auction_system.client.controllers.auction.components;

import auction_system.client.controllers.auction.PublishItemController;
import auction_system.client.services.MyAuctionRow;
import auction_system.client.services.MyAuctionService;
import auction_system.client.utils.Router;
import auction_system.client.utils.ViewConstants;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller cho màn hình "Quản lý phiên của tôi".
 */
public class MyAuctionManagementController implements Initializable {

    @FXML
    private TableView<MyAuctionRow> tblMyAuctions;
    @FXML
    private TableColumn<MyAuctionRow, String> colAuctionId;
    @FXML
    private TableColumn<MyAuctionRow, String> colProductName;
    @FXML
    private TableColumn<MyAuctionRow, String> colCurrentPrice;
    @FXML
    private TableColumn<MyAuctionRow, String> colAuctionStatus;
    @FXML
    private TableColumn<MyAuctionRow, String> colEndTime;
    @FXML
    private TextField txtSearchMyAuction;
    @FXML
    private Button btnRefreshMyAuctions;
    @FXML
    private Button btnEditMyAuction;
    @FXML
    private Button btnDeleteMyAuction;

    /** Nguồn dữ liệu cho bảng. */
    private final ObservableList<MyAuctionRow> myAuctionRows = FXCollections.observableArrayList();

    @Override
    public void initialize(final URL url, final ResourceBundle resourceBundle) {
        initTableColumns();
        tblMyAuctions.setItems(myAuctionRows);
        btnRefreshMyAuctions.setOnAction(event -> loadMyAuctions());
        btnEditMyAuction.setOnAction(event -> handleEditMyAuction());
        btnDeleteMyAuction.setOnAction(event -> handleDeleteMyAuction());
        loadMyAuctions();
    }

    private void initTableColumns() {
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colAuctionStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
    }

    private void loadMyAuctions() {
        MyAuctionService.getInstance().fetchMyAuctions((success, message, rows) ->
                Platform.runLater(() -> handleFetchResult(success, message, rows)));
    }

    private void handleFetchResult(
            final boolean success,
            final String message,
            final List<MyAuctionRow> rows) {
        if (success) {
            myAuctionRows.setAll(rows);
            return;
        }
        myAuctionRows.clear();
    }

    /**
     * Mở form đăng bán ở chế độ chỉnh sửa và nạp dữ liệu phiên đã chọn.
     */
    private void handleEditMyAuction() {
        final MyAuctionRow selected = tblMyAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Thông báo", "Vui lòng chọn phiên cần chỉnh sửa.");
            return;
        }

        final PublishItemController controller = Router.navigateContentAndGetController(
                btnEditMyAuction,
                ViewConstants.PUBLISH_ITEM_VIEW);
        if (controller == null) {
            showInfo("Lỗi", "Không mở được màn hình chỉnh sửa phiên.");
            return;
        }

        // Truyền dữ liệu phiên hiện tại sang form để nạp sẵn.
        controller.startEditMode(selected);
    }

    private void handleDeleteMyAuction() {
        final MyAuctionRow selected = tblMyAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Thông báo", "Vui lòng chọn phiên cần xóa.");
            return;
        }

        MyAuctionService.getInstance().deleteMyAuction(
                selected.getId(),
                (success, message, deletedAuctionId) -> Platform.runLater(() -> {
                    if (success) {
                        loadMyAuctions();
                        showInfo("Thành công", "Đã xóa phiên " + deletedAuctionId);
                    } else {
                        showInfo("Lỗi", message);
                    }
                }));
    }

    private void showInfo(final String title, final String content) {
        final Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
