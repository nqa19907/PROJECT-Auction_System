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
import javafx.scene.control.Button;
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
 * Chưa xử lý logic xóa/sửa.
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
    private TableColumn<MyAuctionRow, String> colBidCount;

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
    private final ObservableList<MyAuctionRow> myAuctionRows = FXCollections.observableArrayList();

    /**
     * Khởi tạo màn hình.
     * - map cột với field trong MyAuctionRow
     * - bind dữ liệu bảng
     * - gắn sự kiện refresh
     * - tải dữ liệu lần đầu
     */
    @Override
    public void initialize(final URL url, final ResourceBundle resourceBundle) {
        initTableColumns();
        tblMyAuctions.setItems(myAuctionRows);

        // Nút làm mới danh sách phiên của tôi.
        btnRefreshMyAuctions.setOnAction(event -> loadMyAuctions());

        // TODO: chưa có logic xóa thật, tạm disable để tránh bấm lỗi.
        btnDeleteMyAuction.setDisable(true);

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
        colBidCount.setCellValueFactory(new PropertyValueFactory<>("bidCount"));
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
        // Dù message là cảnh báo parse count lệch, vẫn ưu tiên hiển thị dữ liệu đã parse được.
        if (success) {
            myAuctionRows.setAll(rows);
            return;
        }

        // Nếu fail thì xóa dữ liệu cũ để tránh hiển thị nhầm.
        myAuctionRows.clear();

        // TODO: có thể thêm Alert/Label lỗi tại đây nếu bạn muốn hiển thị message ra UI.
        // System.out.println("Load my auctions failed: " + message);
    }
}
