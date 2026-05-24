package auction_system.client.controllers.auction;

import auction_system.client.network.NetworkClient;
import auction_system.client.services.UserSessionService;
import auction_system.client.utils.Router;
import auction_system.client.utils.ViewConstants;
import auction_system.common.models.auctions.Auction;
import auction_system.common.models.users.Admin;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
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
 * Controller cho màn hình Admin Dashboard.
 *
 * <p>Màn hình này nạp dữ liệu thật từ storage hiện có của dự án.
 * Dữ liệu user lấy từ SerializedDatabase, dữ liệu auction lấy từ AuctionManager.
 * Ngoài ra có refresh và tìm kiếm nhanh cho cả 2 bảng.
 */
public class AdminDashboardController {

    @FXML
    private BorderPane root;

    // Bảng user
    @FXML
    private TableView<UserRow> tblUsers;
    @FXML
    private TableColumn<UserRow, String> colUserId;
    @FXML
    private TableColumn<UserRow, String> colUsername;
    @FXML
    private TableColumn<UserRow, String> colEmail;
    @FXML
    private TableColumn<UserRow, String> colUserStatus;
    @FXML
    private TextField txtSearchUser;
    @FXML
    private Button btnRefreshUsers;
    @FXML
    private Button btnDeleteUser;

    // Bảng auction
    @FXML
    private TableView<AuctionRow> tblAuctions;
    @FXML
    private TableColumn<AuctionRow, String> colAuctionId;
    @FXML
    private TableColumn<AuctionRow, String> colProductName;
    @FXML
    private TableColumn<AuctionRow, String> colSeller;
    @FXML
    private TableColumn<AuctionRow, String> colCurrentPrice;
    @FXML
    private TableColumn<AuctionRow, String> colAuctionStatus;
    @FXML
    private TextField txtSearchAuction;
    @FXML
    private Button btnRefreshAuctions;
    @FXML
    private Button btnDeleteAuction;

    /** Truy cập kho dữ liệu .ser của app. */
    private SerializedDatabase database;
    /** Manager lấy danh sách auction đang được quản lý. */
    private AuctionManager auctionManager;

    /** Danh sách dòng gốc cho bảng user. */
    private final ObservableList<UserRow> userRows = FXCollections.observableArrayList();
    /** Danh sách dòng gốc cho bảng auction. */
    private final ObservableList<AuctionRow> auctionRows = FXCollections.observableArrayList();

    /**
     * Khởi tạo controller sau khi FXML inject xong.
     */
    @FXML
    private void initialize() {
        initDataSource();
        initUserTable();
        initAuctionTable();
        bindActions();
        refreshUsers();
        refreshAuctions();

        // Đăng ký handler phản hồi xóa phiên từ server
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_DELETE_AUCTION_OK.name(),
                this::handleAdminDeleteAuctionOk);

        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_DELETE_AUCTION_FAIL.name(),
                this::handleAdminDeleteAuctionFail);

        // Đăng ký handler phản hồi xóa người dùng
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_DELETE_USER_OK.name(),
                this::handleAdminDeleteUserOk);

        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_DELETE_USER_FAIL.name(),
                this::handleAdminDeleteUserFail);
    }

    /**
     * Khởi tạo datasource dùng chung với app.
     */
    private void initDataSource() {
        database = new SerializedDatabase(Path.of("data"));
        auctionManager = AuctionManager.getInstance(database);
    }

    /**
     * Cấu hình bảng user: cột, sắp xếp và tìm kiếm.
     */
    private void initUserTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        final FilteredList<UserRow> filtered = new FilteredList<>(userRows, row -> true);
        txtSearchUser.textProperty().addListener((obs, oldValue, newValue) ->
            filtered.setPredicate(row -> matchUser(row, newValue)));

        final SortedList<UserRow> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(tblUsers.comparatorProperty());
        tblUsers.setItems(sorted);
    }

    /**
     * Cấu hình bảng auction: cột, sắp xếp và tìm kiếm.
     */
    private void initAuctionTable() {
        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("seller"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colAuctionStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        final FilteredList<AuctionRow> filtered = new FilteredList<>(auctionRows, row -> true);
        txtSearchAuction.textProperty().addListener((obs, oldValue, newValue) ->
            filtered.setPredicate(row -> matchAuction(row, newValue)));

        final SortedList<AuctionRow> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(tblAuctions.comparatorProperty());
        tblAuctions.setItems(sorted);
    }

    /**
     * Gán action cho nút refresh của 2 bảng.
     */
    private void bindActions() {
        btnRefreshUsers.setOnAction(event -> refreshUsers());
        btnRefreshAuctions.setOnAction(event -> refreshAuctions());
        btnDeleteUser.setOnAction(event -> deleteSelectedUser());
        btnDeleteAuction.setOnAction(event -> deleteSelectedAuction());
    }

    /**
     * Nạp lại dữ liệu user từ repository.
     */
    private void refreshUsers() {
        database.users().reload();
        final List<User> users = database.users().findAll();

        userRows.setAll(users.stream()
                .map(user -> new UserRow(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.isOnline() ? "ONLINE" : "OFFLINE"))
                .toList());
    }

    /**
     * Nạp lại dữ liệu auction từ AuctionManager.
     */
    private void refreshAuctions() {
        database.auctions().reload();
        final List<Auction> auctions = auctionManager.getAllAuctions();

        auctionRows.setAll(auctions.stream()
                .map(auction -> new AuctionRow(
                        auction.getId(),
                        auction.getItem() != null
                                ? auction.getItem().getItemName()
                                : "(Khong co ten)",
                        auction.getParticipant() != null
                                ? auction.getParticipant().getUsername()
                                : "(Khong ro)",
                        formatPrice(auction),
                        auction.getStatus() != null ? auction.getStatus().name() : "UNKNOWN"))
                .toList());
    }

    /**
     * Predicate tìm kiếm cho bảng user.
     *
     * @param row dòng user cần kiểm tra
     * @param keyword từ khóa tìm kiếm hiện tại
     * @return true nếu dòng khớp từ khóa, ngược lại false
     */
    private boolean matchUser(final UserRow row, final String keyword) {
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
     * Predicate tìm kiếm cho bảng auction.
     *
     * @param row dòng auction cần kiểm tra
     * @param keyword từ khóa tìm kiếm hiện tại
     * @return true nếu dòng khớp từ khóa, ngược lại false
     */
    private boolean matchAuction(final AuctionRow row, final String keyword) {
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
     * Định dạng giá hiện tại để hiển thị trong bảng.
     *
     * @param auction phiên đấu giá cần lấy giá hiện tại
     * @return chuỗi giá đã được định dạng
     */
    private String formatPrice(final Auction auction) {
        if (auction.getItem() == null) {
            return "0";
        }
        return String.format(Locale.ROOT, "%,.0f", auction.getItem().getCurrentPrice());
    }

    /**
     * Xóa user đang chọn trên UI.
     */
    private void deleteSelectedUser() {
        final UserRow selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Thông báo", "Vui lòng chọn người dùng cần xóa trên bảng.");
            return;
        }

        final User currentUser = UserSessionService.getInstance().getCurrentUser();
        if (!(currentUser instanceof Admin admin)) {
            showInfo("Lỗi", "Tài khoản hiện tại không phải admin.");
            return;
        }

        String userId = selected.getId();
        String request = admin.deleteUser(userId);

        boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent) {
            showInfo("Lỗi", "Không gửi được yêu cầu xóa người dùng tới server.");
        }
    }

    /**
     * Xử lý phản hồi xóa phiên thành công từ server.
     *
     * @param response chuỗi phản hồi theo protocol
     */
    private void handleAdminDeleteAuctionOk(final String response) {
        Platform.runLater(() -> {
            String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
            if (parts.length > 1) {
                String auctionId = parts[1];
                auctionRows.removeIf(row -> auctionId.equals(row.getId()));
                tblAuctions.refresh();
                showInfo("Thành công", "Đã xóa phiên " + auctionId);
            }
        });
    }

    /**
     * Xử lý phản hồi xóa phiên thất bại từ server.
     *
     * @param response chuỗi phản hồi theo protocol
     */
    private void handleAdminDeleteAuctionFail(final String response) {
        Platform.runLater(() -> {
            String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
            String message = parts.length > 1 ? parts[1] : "Xóa phiên thất bại.";
            showInfo("Lỗi", message);
        });
    }

    /**
     * Gửi yêu cầu xóa phiên đang chọn lên server.
     *
     * <p>Chỉ khi server trả về thành công thì bảng mới xóa dòng tương ứng.
     */
    private void deleteSelectedAuction() {
        final AuctionRow selected = tblAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Thông báo", "Vui lòng chọn phiên cần xóa trên bảng.");
            return;
        }
        final User currentUser = UserSessionService.getInstance().getCurrentUser();
        if (!(currentUser instanceof Admin admin)) {
            showInfo("Lỗi", "Tài khoản hiện tại không phải admin.");
            return;
        }

        String auctionId = selected.getId();
        String request = admin.deleteAuction(auctionId);

        boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent) {
            showInfo("Lỗi", "Không gửi được yêu cầu xóa phiên tới server.");
        }
    }

    /**
     * Xử lý phản hồi xóa người dùng thành công từ server.
     *
     * <p>Khi nhận được userId hợp lệ, hàm sẽ xóa dòng tương ứng khỏi bảng
     * người dùng và hiển thị thông báo thành công.
     *
     * @param response chuỗi phản hồi theo protocol
     */
    private void handleAdminDeleteUserOk(final String response) {
        Platform.runLater(() -> {
            String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
            if (parts.length > 1) {
                String userId = parts[1];
                userRows.removeIf(row -> userId.equals(row.getId()));
                tblUsers.refresh();
                showInfo("Thành công", "Đã xóa người dùng " + userId);
            }
        });
    }

    /**
     * Xử lý phản hồi xóa người dùng thất bại từ server.
     *
     * <p>Hàm tách thông điệp lỗi từ phản hồi và hiển thị cho quản trị viên.
     *
     * @param response chuỗi phản hồi theo protocol
     */
    private void handleAdminDeleteUserFail(final String response) {
        Platform.runLater(() -> {
            String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
            String message = parts.length > 1 ? parts[1] : "Xóa người dùng thất bại.";
            showInfo("Lỗi", message);
        });
    }

    /**
     * Hiện thông báo thông tin đơn giản.
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

    /**
     * Xử lý khi người dùng nhấn "Quay về" ở header Admin Dashboard.
     */
    @FXML
    private void handleBack() {
        Router.navigateContent(root, ViewConstants.ITEM_LIST_VIEW);
    }

    /**
     * DTO hiển thị 1 dòng user trên TableView.
     */
    public static class UserRow {
        private final SimpleStringProperty id;
        private final SimpleStringProperty username;
        private final SimpleStringProperty email;
        private final SimpleStringProperty status;

        /**
         * Tạo 1 dòng hiển thị user cho bảng.
         *
         * @param id id user
         * @param username tên đăng nhập
         * @param email email user
         * @param status trạng thái online/offline
         */
        public UserRow(
                final String id,
                final String username,
                final String email,
                final String status) {
            this.id = new SimpleStringProperty(id);
            this.username = new SimpleStringProperty(username);
            this.email = new SimpleStringProperty(email);
            this.status = new SimpleStringProperty(status);
        }

        /**
         * Lấy id user.
         *
         * @return id user
         */
        public String getId() {
            return id.get();
        }

        /**
         * Lấy tên đăng nhập.
         *
         * @return tên đăng nhập
         */
        public String getUsername() {
            return username.get();
        }

        /**
         * Lấy email user.
         *
         * @return email user
         */
        public String getEmail() {
            return email.get();
        }

        /**
         * Lấy trạng thái user.
         *
         * @return ONLINE hoặc OFFLINE
         */
        public String getStatus() {
            return status.get();
        }
    }

    /**
     * DTO hiển thị 1 dòng auction trên TableView.
     */
    public static class AuctionRow {
        private final SimpleStringProperty id;
        private final SimpleStringProperty productName;
        private final SimpleStringProperty seller;
        private final SimpleStringProperty currentPrice;
        private final SimpleStringProperty status;

        /**
         * Tạo 1 dòng hiển thị auction cho bảng.
         *
         * @param id id phiên đấu giá
         * @param productName tên sản phẩm
         * @param seller tên người bán
         * @param currentPrice giá hiện tại
         * @param status trạng thái phiên
         */
        public AuctionRow(
                final String id,
                final String productName,
                final String seller,
                final String currentPrice,
                final String status) {
            this.id = new SimpleStringProperty(id);
            this.productName = new SimpleStringProperty(productName);
            this.seller = new SimpleStringProperty(seller);
            this.currentPrice = new SimpleStringProperty(currentPrice);
            this.status = new SimpleStringProperty(status);
        }

        /**
         * Lấy id phiên.
         *
         * @return id phiên
         */
        public String getId() {
            return id.get();
        }

        /**
         * Lấy tên sản phẩm.
         *
         * @return tên sản phẩm
         */
        public String getProductName() {
            return productName.get();
        }

        /**
         * Lấy tên người bán.
         *
         * @return tên người bán
         */
        public String getSeller() {
            return seller.get();
        }

        /**
         * Lấy giá hiện tại.
         *
         * @return giá hiện tại đã format
         */
        public String getCurrentPrice() {
            return currentPrice.get();
        }

        /**
         * Lấy trạng thái phiên.
         *
         * @return trạng thái phiên
         */
        public String getStatus() {
            return status.get();
        }

        /**
         * Cập nhật trạng thái phiên trên dòng hiển thị.
         *
         * @param newStatus trạng thái mới
         */
        public void setStatus(final String newStatus) {
            status.set(newStatus);
        }
    }
}
