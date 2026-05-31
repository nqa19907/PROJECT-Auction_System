package auction_system.client.controllers.auction;

import auction_system.client.network.NetworkClient;
import auction_system.client.services.AuthService;
import auction_system.client.services.UserSessionService;
import auction_system.client.utils.ViewConstants;
import auction_system.client.utils.WindowTitleUtil;
import auction_system.common.models.users.Admin;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Controller cho màn hình Admin Dashboard.
 *
 * <p>Controller chỉ giữ phần điều phối UI: cấu hình bảng, tìm kiếm, action button
 * và chuyển màn. Socket protocol và parse response được tách sang service riêng.
 */
public class AdminDashboardController {

    @FXML
    private BorderPane root;

    // Bảng user.
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

    // Bảng auction.
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

    private final AdminDashboardService dashboardService = new AdminDashboardService();
    private final ObservableList<AdminUserRow> userRows = FXCollections.observableArrayList();
    private final ObservableList<AdminAuctionRow> auctionRows = FXCollections.observableArrayList();
    /** Cờ đảm bảo chỉ đăng ký cleanup lifecycle một lần. */
    private boolean cleanupRegistered;
    /**
     * Handler realtime cho các sự kiện đấu giá.
     * Khi có thay đổi từ server, dashboard sẽ kéo lại snapshot mới nhất.
     */
    private final Consumer<String> auctionRealtimeHandler = response -> {
        if (isAdminDashboardActive()) {
            refreshAuctions();
        }
    };
    /** Handler realtime cho thay đổi danh sách người dùng. */
    private final Consumer<String> userRealtimeHandler = response -> {
        if (isAdminDashboardActive()) {
            refreshUsers();
        }
    };

    /**
     * Khởi tạo controller sau khi FXML inject xong.
     */
    @FXML
    private void initialize() {
        initUserTable();
        initAuctionTable();
        bindActions();
        bindServiceCallbacks();
        registerRealtimeHandlers();
        registerLifecycleCleanup();
        // Chỉ gọi refresh sau khi đã đăng ký đầy đủ handler để tránh mất phản hồi đầu tiên.
        refreshUsers();
        refreshAuctions();
    }

    /**
     * Cấu hình bảng user: cột, sắp xếp và tìm kiếm.
     */
    private void initUserTable() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // FilteredList giữ danh sách gốc nguyên vẹn và chỉ đổi predicate khi nhập tìm kiếm.
        final FilteredList<AdminUserRow> filtered = new FilteredList<>(userRows, row -> true);
        txtSearchUser.textProperty().addListener((obs, oldValue, newValue) ->
                filtered.setPredicate(row -> matchUser(row, newValue)));

        // SortedList nhận comparator từ TableView để click header vẫn sắp xếp được.
        final SortedList<AdminUserRow> sorted = new SortedList<>(filtered);
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

        // Predicate tìm kiếm chạy trên ObservableList đang được refresh từ socket.
        final FilteredList<AdminAuctionRow> filtered =
                new FilteredList<>(auctionRows, row -> true);
        txtSearchAuction.textProperty().addListener((obs, oldValue, newValue) ->
                filtered.setPredicate(row -> matchAuction(row, newValue)));

        // TableView quản lý thứ tự sort, còn source list vẫn giữ dữ liệu server trả về.
        final SortedList<AdminAuctionRow> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(tblAuctions.comparatorProperty());
        tblAuctions.setItems(sorted);
    }

    /**
     * Gán action cho các nút thao tác trên dashboard.
     */
    private void bindActions() {
        btnRefreshUsers.setOnAction(event -> refreshUsers());
        btnRefreshAuctions.setOnAction(event -> refreshAuctions());
        btnDeleteUser.setOnAction(event -> deleteSelectedUser());
        btnDeleteAuction.setOnAction(event -> deleteSelectedAuction());
    }

    /**
     * Gắn callback service để mọi cập nhật bảng đều quay về JavaFX Application Thread.
     */
    private void bindServiceCallbacks() {
        dashboardService.setUserListCallback(rows ->
                Platform.runLater(() -> replaceUserRows(rows)));
        dashboardService.setAuctionListCallback(rows ->
                Platform.runLater(() -> replaceAuctionRows(rows)));
        dashboardService.setDeleteUserCallback(userId ->
                Platform.runLater(() -> removeDeletedUser(userId)));
        dashboardService.setDeleteAuctionCallback(auctionId ->
                Platform.runLater(() -> removeDeletedAuction(auctionId)));
        dashboardService.setFailureCallback(message ->
                Platform.runLater(() -> showInfo("Lỗi", message)));
    }

    /**
     * Nạp lại dữ liệu user qua server.
     */
    private void refreshUsers() {
        if (getCurrentAdmin() == null) {
            return;
        }

        if (!dashboardService.fetchUsers()) {
            showInfo("Lỗi", "Không gửi được yêu cầu tải danh sách người dùng.");
        }
    }

    /**
     * Nạp lại dữ liệu auction qua server.
     */
    private void refreshAuctions() {
        if (getCurrentAdmin() == null) {
            return;
        }

        if (!dashboardService.fetchAuctions()) {
            showInfo("Lỗi", "Không gửi được yêu cầu tải danh sách phiên đấu giá.");
        }
    }

    /**
     * Đăng ký các sự kiện realtime liên quan đến thay đổi đấu giá.
     *
     * <p>Tận dụng luồng socket sẵn có: khi có tín hiệu thay đổi,
     * dashboard gọi refresh để lấy dữ liệu chuẩn mới nhất từ server.
     */
    private void registerRealtimeHandlers() {
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.AUCTION_CREATED.name(),
                auctionRealtimeHandler);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.UPDATE_PRICE.name(),
                auctionRealtimeHandler);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.AUCTION_STARTED.name(),
                auctionRealtimeHandler);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.AUCTION_ENDED.name(),
                auctionRealtimeHandler);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.AUCTION_EXTENDED.name(),
                auctionRealtimeHandler);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ANTI_SNIPING_UPDATED.name(),
                auctionRealtimeHandler);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.USER_LIST_CHANGED.name(),
                userRealtimeHandler);
    }

    /**
     * Gỡ đăng ký handler khi màn hình admin bị loại khỏi scene.
     * Tránh giữ listener cũ gây refresh lặp nhiều lần.
     */
    private void registerLifecycleCleanup() {
        if (cleanupRegistered || root == null) {
            return;
        }

        cleanupRegistered = true;
        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (oldScene != null && newScene == null) {
                dispose();
            }
        });
    }

    private void dispose() {
        final NetworkClient client = NetworkClient.getInstance();
        client.unregisterHandler(Protocol.Response.AUCTION_CREATED.name(), auctionRealtimeHandler);
        client.unregisterHandler(Protocol.Response.UPDATE_PRICE.name(), auctionRealtimeHandler);
        client.unregisterHandler(Protocol.Response.AUCTION_STARTED.name(), auctionRealtimeHandler);
        client.unregisterHandler(Protocol.Response.AUCTION_ENDED.name(), auctionRealtimeHandler);
        client.unregisterHandler(Protocol.Response.AUCTION_EXTENDED.name(), auctionRealtimeHandler);
        client.unregisterHandler(
                Protocol.Response.ANTI_SNIPING_UPDATED.name(),
                auctionRealtimeHandler);
        client.unregisterHandler(Protocol.Response.USER_LIST_CHANGED.name(), userRealtimeHandler);
        dashboardService.dispose();
    }

    private boolean isAdminDashboardActive() {
        return root != null && root.getScene() != null && getCurrentAdmin() != null;
    }

    /**
     * Predicate tìm kiếm cho bảng user.
     *
     * @param row dòng user cần kiểm tra
     * @param keyword từ khóa tìm kiếm hiện tại
     * @return true nếu dòng khớp từ khóa, ngược lại false
     */
    private boolean matchUser(final AdminUserRow row, final String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        // Chuẩn hóa keyword một lần để các phép contains không phụ thuộc chữ hoa/thường.
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
    private boolean matchAuction(final AdminAuctionRow row, final String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        // Tìm kiếm phủ toàn bộ cột đang hiển thị để admin lọc nhanh theo bất kỳ thông tin nào.
        final String q = keyword.toLowerCase(Locale.ROOT).trim();
        return row.getId().toLowerCase(Locale.ROOT).contains(q)
                || row.getProductName().toLowerCase(Locale.ROOT).contains(q)
                || row.getSeller().toLowerCase(Locale.ROOT).contains(q)
                || row.getCurrentPrice().toLowerCase(Locale.ROOT).contains(q)
                || row.getStatus().toLowerCase(Locale.ROOT).contains(q);
    }

    /**
     * Xóa user đang chọn trên UI.
     */
    private void deleteSelectedUser() {
        final AdminUserRow selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Thông báo", "Vui lòng chọn người dùng cần xóa trên bảng.");
            return;
        }

        final Admin admin = getCurrentAdmin();
        if (admin == null) {
            showInfo("Lỗi", "Tài khoản hiện tại không phải admin.");
            return;
        }

        // Request xóa được tạo từ model Admin để giữ đúng format command hiện có.
        if (!dashboardService.deleteUser(admin, selected.getId())) {
            showInfo("Lỗi", "Không gửi được yêu cầu xóa người dùng tới server.");
        }
    }

    /**
     * Gửi yêu cầu xóa phiên đang chọn lên server.
     */
    private void deleteSelectedAuction() {
        final AdminAuctionRow selected = tblAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Thông báo", "Vui lòng chọn phiên cần xóa trên bảng.");
            return;
        }

        final Admin admin = getCurrentAdmin();
        if (admin == null) {
            showInfo("Lỗi", "Tài khoản hiện tại không phải admin.");
            return;
        }

        // Bảng chỉ xóa dòng sau khi server xác nhận thành công qua callback.
        if (!dashboardService.deleteAuction(admin, selected.getId())) {
            showInfo("Lỗi", "Không gửi được yêu cầu xóa phiên tới server.");
        }
    }

    private Admin getCurrentAdmin() {
        final User currentUser = UserSessionService.getInstance().getCurrentUser();
        return currentUser instanceof Admin admin ? admin : null;
    }

    private void replaceUserRows(final List<AdminUserRow> rows) {
        // Thay toàn bộ danh sách để bảng phản ánh đúng snapshot mới nhất từ server.
        userRows.setAll(rows);
        tblUsers.refresh();
    }

    private void replaceAuctionRows(final List<AdminAuctionRow> rows) {
        // Thay toàn bộ danh sách để xóa các dòng server không còn trả về.
        auctionRows.setAll(rows);
        tblAuctions.refresh();
    }

    private void removeDeletedUser(final String userId) {
        // Xóa theo id server xác nhận, không dựa vào selection hiện tại có thể đã đổi.
        userRows.removeIf(row -> userId.equals(row.getId()));
        tblUsers.refresh();
        showInfo("Thành công", "Đã xóa người dùng " + userId);
    }

    private void removeDeletedAuction(final String auctionId) {
        // Xóa theo id server xác nhận, không dựa vào selection hiện tại có thể đã đổi.
        auctionRows.removeIf(row -> auctionId.equals(row.getId()));
        tblAuctions.refresh();
        showInfo("Thành công", "Đã xóa phiên " + auctionId);
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
        AuthService.getInstance().logout(result -> Platform.runLater(() -> {
            try {
                // Load lại màn đăng nhập sau khi server đã xử lý logout.
                final FXMLLoader loader = new FXMLLoader(
                        getClass().getResource(ViewConstants.LOGIN_VIEW));
                final Parent loginRoot = loader.load();
                final Stage stage = (Stage) root.getScene().getWindow();
                stage.setScene(new Scene(loginRoot));
                WindowTitleUtil.applyTitle(stage, ViewConstants.LOGIN_VIEW);
            } catch (IOException e) {
                showInfo("Lỗi", "Không thể chuyển về màn đăng nhập.");
            }
        }));
    }
}
