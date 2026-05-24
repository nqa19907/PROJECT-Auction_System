package auction_system.client.controllers.auction;

import auction_system.client.services.AdminDashboardService;
import auction_system.client.services.AdminDashboardService.AuctionSnapshot;
import auction_system.client.services.AdminDashboardService.UserSnapshot;
import auction_system.client.utils.Router;
import auction_system.client.utils.ViewConstants;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;

/**
 * Controller cho man hinh Admin Dashboard.
 *
 * <p>Controller chi dieu phoi UI. Du lieu quan tri duoc lay qua
 * AdminDashboardService de client khong phu thuoc truc tiep vao persistence
 * hoac runtime manager cua server.
 */
public class AdminDashboardController {

    @FXML
    private BorderPane root;

    // User table
    @FXML
    private TableView<AdminUserRow> tblUsers;
    @FXML
    private TableColumn<AdminUserRow, String> colUserId;
    @FXML
    private TableColumn<AdminUserRow, String> colUsername;
    @FXML
    private TableColumn<AdminUserRow, String> colEmail;
    @FXML
    private TableColumn<AdminUserRow, String> colUserStatus;
    @FXML
    private TextField txtSearchUser;
    @FXML
    private Button btnRefreshUsers;
    @FXML
    private Button btnDeleteUser;

    // Auction table
    @FXML
    private TableView<AdminAuctionRow> tblAuctions;
    @FXML
    private TableColumn<AdminAuctionRow, String> colAuctionId;
    @FXML
    private TableColumn<AdminAuctionRow, String> colProductName;
    @FXML
    private TableColumn<AdminAuctionRow, String> colSeller;
    @FXML
    private TableColumn<AdminAuctionRow, String> colCurrentPrice;
    @FXML
    private TableColumn<AdminAuctionRow, String> colAuctionStatus;
    @FXML
    private TextField txtSearchAuction;
    @FXML
    private Button btnRefreshAuctions;
    @FXML
    private Button btnDeleteAuction;

    /** Service client gui command quan tri len server. */
    private final AdminDashboardService adminService = AdminDashboardService.getInstance();

    /** Danh sach dong goc cho bang user. */
    private final ObservableList<AdminUserRow> userRows = FXCollections.observableArrayList();
    /** Danh sach dong goc cho bang auction. */
    private final ObservableList<AdminAuctionRow> auctionRows = FXCollections.observableArrayList();

    /**
     * Khoi tao controller sau khi FXML inject xong.
     */
    @FXML
    private void initialize() {
        initUserTable();
        initAuctionTable();
        bindActions();
        refreshUsers();
        refreshAuctions();
    }

    /**
     * Cau hinh bang user: cot, sap xep va tim kiem.
     */
    private void initUserTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        final FilteredList<AdminUserRow> filtered = new FilteredList<>(userRows, row -> true);
        txtSearchUser.textProperty().addListener((obs, oldValue, newValue) ->
            filtered.setPredicate(row -> matchUser(row, newValue)));

        final SortedList<AdminUserRow> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(tblUsers.comparatorProperty());
        tblUsers.setItems(sorted);
    }

    /**
     * Cau hinh bang auction: cot, sap xep va tim kiem.
     */
    private void initAuctionTable() {
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("seller"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colAuctionStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        final FilteredList<AdminAuctionRow> filtered =
                new FilteredList<>(auctionRows, row -> true);
        txtSearchAuction.textProperty().addListener((obs, oldValue, newValue) ->
            filtered.setPredicate(row -> matchAuction(row, newValue)));

        final SortedList<AdminAuctionRow> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(tblAuctions.comparatorProperty());
        tblAuctions.setItems(sorted);
    }

    /**
     * Gan action cho nut refresh cua 2 bang.
     */
    private void bindActions() {
        btnRefreshUsers.setOnAction(event -> refreshUsers());
        btnRefreshAuctions.setOnAction(event -> refreshAuctions());
        btnDeleteUser.setOnAction(event -> deleteSelectedUser());
        btnDeleteAuction.setOnAction(event -> deleteSelectedAuction());
    }

    /**
     * Nap lai du lieu user qua server command.
     */
    private void refreshUsers() {
        adminService.fetchUsers((success, message, users) -> {
            if (!success) {
                showInfo("Lỗi", fallbackMessage(message, "Không tải được danh sách người dùng."));
                return;
            }

            userRows.setAll(users.stream()
                    .map(this::toUserRow)
                    .toList());
        });
    }

    /**
     * Nap lai du lieu auction qua server command.
     */
    private void refreshAuctions() {
        adminService.fetchAuctions((success, message, auctions) -> {
            if (!success) {
                showInfo("Lỗi", fallbackMessage(message, "Không tải được danh sách phiên."));
                return;
            }

            auctionRows.setAll(auctions.stream()
                    .map(this::toAuctionRow)
                    .toList());
        });
    }

    /**
     * Predicate tim kiem cho bang user.
     *
     * @param row dong user can kiem tra
     * @param keyword tu khoa tim kiem hien tai
     * @return true neu dong khop tu khoa, nguoc lai false
     */
    private boolean matchUser(final AdminUserRow row, final String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        final String q = keyword.toLowerCase(Locale.ROOT).trim();
        return row.getId().toLowerCase(Locale.ROOT).contains(q)
                || row.getUsername().toLowerCase(Locale.ROOT).contains(q)
                || row.getEmail().toLowerCase(Locale.ROOT).contains(q)
                || row.getStatus().toLowerCase(Locale.ROOT).contains(q);
    }

    /**
     * Predicate tim kiem cho bang auction.
     *
     * @param row dong auction can kiem tra
     * @param keyword tu khoa tim kiem hien tai
     * @return true neu dong khop tu khoa, nguoc lai false
     */
    private boolean matchAuction(final AdminAuctionRow row, final String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        final String q = keyword.toLowerCase(Locale.ROOT).trim();
        return row.getId().toLowerCase(Locale.ROOT).contains(q)
                || row.getProductName().toLowerCase(Locale.ROOT).contains(q)
                || row.getSeller().toLowerCase(Locale.ROOT).contains(q)
                || row.getCurrentPrice().toLowerCase(Locale.ROOT).contains(q)
                || row.getStatus().toLowerCase(Locale.ROOT).contains(q);
    }

    /**
     * Xoa user dang chon tren UI.
     */
    private void deleteSelectedUser() {
        final AdminUserRow selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Thông báo", "Vui lòng chọn người dùng cần xóa trên bảng.");
            return;
        }

        String userId = selected.getId();
        adminService.deleteUser(userId, this::handleDeleteUserResult);
    }

    /**
     * Gửi yêu cầu xóa phiên đang chọn lên server.
     *
     * <p>Chỉ khi server trả về thành công thì bảng mới xóa dòng tương ứng.
     */
    private void deleteSelectedAuction() {
        final AdminAuctionRow selected = tblAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Thông báo", "Vui lòng chọn phiên cần xóa trên bảng.");
            return;
        }
        String auctionId = selected.getId();
        adminService.deleteAuction(auctionId, this::handleDeleteAuctionResult);
    }

    /**
     * Xu ly ket qua xoa user tu service.
     *
     * @param success true neu server xoa thanh cong
     * @param userId id user vua xu ly
     * @param message thong bao tu server/service
     */
    private void handleDeleteUserResult(
            final boolean success,
            final String userId,
            final String message) {
        if (!success) {
            showInfo("Lỗi", fallbackMessage(message, "Xóa người dùng thất bại."));
            return;
        }

        userRows.removeIf(row -> userId.equals(row.getId()));
        tblUsers.refresh();
        showInfo("Thành công", message);
    }

    /**
     * Xu ly ket qua xoa auction tu service.
     *
     * @param success true neu server xoa thanh cong
     * @param auctionId id phien vua xu ly
     * @param message thong bao tu server/service
     */
    private void handleDeleteAuctionResult(
            final boolean success,
            final String auctionId,
            final String message) {
        if (!success) {
            showInfo("Lỗi", fallbackMessage(message, "Xóa phiên thất bại."));
            return;
        }

        auctionRows.removeIf(row -> auctionId.equals(row.getId()));
        tblAuctions.refresh();
        showInfo("Thành công", message);
    }

    private AdminUserRow toUserRow(final UserSnapshot user) {
        return new AdminUserRow(user.id(), user.username(), user.email(), user.status());
    }

    private AdminAuctionRow toAuctionRow(final AuctionSnapshot auction) {
        return new AdminAuctionRow(
                auction.id(),
                auction.productName(),
                auction.seller(),
                formatPrice(auction.currentPrice()),
                auction.status());
    }

    private String formatPrice(final String rawPrice) {
        try {
            return String.format(Locale.ROOT, "%,.0f", Double.parseDouble(rawPrice));
        } catch (NumberFormatException exception) {
            return rawPrice == null || rawPrice.isBlank() ? "0" : rawPrice;
        }
    }

    private String fallbackMessage(final String message, final String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }

    /**
     * Hien thong bao thong tin don gian.
     *
     * @param title tieu de thong bao
     * @param content noi dung thong bao
     */
    private void showInfo(final String title, final String content) {
        final Alert alert = new Alert(Alert.AlertType.INFORMATION, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Xu ly khi nguoi dung nhan "Quay ve" o header Admin Dashboard.
     */
    @FXML
    private void handleBack() {
        Router.navigateContent(root, ViewConstants.ITEM_LIST_VIEW);
    }
}
