package auction_system.client.controllers.auction.components;

import auction_system.client.services.MyAuctionRow;
import auction_system.client.services.MyAuctionService;
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
 *
 * <p>Hiện tại chỉ xử lý:
 * - tải danh sách phiên do user hiện tại đăng
 * - hiển thị lên TableView
 * - gửi yêu cầu xóa phiên đã chọn
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
    private Button btnDeleteMyAuction;

    /** Nguồn dữ liệu cho bảng. */
    private final ObservableList<MyAuctionRow> myAuctionRows =
            FXCollections.observableArrayList();

    /**
     * Khởi tạo màn hình.
     */
    @Override
    public void initialize(final URL url, final ResourceBundle resourceBundle) {
        initTableColumns();
        tblMyAuctions.setItems(myAuctionRows);

        // Nút làm mới danh sách phiên của tôi.
        btnRefreshMyAuctions.setOnAction(event -> loadMyAuctions());

        // Nút xóa phiên đang chọn.
        btnDeleteMyAuction.setOnAction(event -> handleDeleteMyAuction());

        // Tải dữ liệu ngay khi mở màn.
        loadMyAuctions();
    }

    /**
     * Cấu hình mapping dữ liệu từ MyAuctionRow lên từng cột.
     */
    private void initTableColumns() {
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colAuctionStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
    }

    /**
     * Gọi service lấy danh sách phiên của user hiện tại và đổ lên bảng.
     */
    private void loadMyAuctions() {
        MyAuctionService.getInstance().fetchMyAuctions((success, message, rows) ->
                Platform.runLater(() -> handleFetchResult(success, message, rows)));
    }

    /**
     * Xử lý kết quả trả về từ service.
     *
     * @param success true nếu lấy dữ liệu thành công
     * @param message thông báo từ service/server
     * @param rows danh sách dòng để hiển thị
     */
    private void handleFetchResult(
            final boolean success,
            final String message,
            final List<MyAuctionRow> rows
    ) {
        if (success) {
            myAuctionRows.setAll(rows);
            return;
        }

        // Nếu fail thì xóa dữ liệu cũ để tránh hiển thị nhầm.
        myAuctionRows.clear();
    }

    /**
     * Xử lý khi user bấm nút xóa phiên.
     */
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
                        // Cách an toàn nhất: tải lại danh sách từ server.
                        loadMyAuctions();
                        showInfo("Thành công", "Đã xóa phiên " + deletedAuctionId);
                    } else {
                        showInfo("Lỗi", message);
                    }
                }));
    }

    /**
     * Hiển thị thông báo đơn giản.
     *
     * @param title tiêu đề thông báo
     * @param content nội dung thông báo
     */
    private void showInfo(final String title, final String content) {
        final Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
