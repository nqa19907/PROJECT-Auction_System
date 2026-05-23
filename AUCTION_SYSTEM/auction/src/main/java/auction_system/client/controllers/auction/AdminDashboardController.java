package auction_system.client.controllers.auction;

import auction_system.client.utils.Router;
import auction_system.client.utils.ViewConstants;
import auction_system.common.models.auctions.Auction;
import auction_system.common.models.users.User;
import auction_system.server.core.AuctionManager;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
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
 * Controller cho man hinh Admin Dashboard.
 *
 * <p>Man hinh nay nap du lieu that tu storage hien co cua du an.
 * Du lieu user lay tu SerializedDatabase, du lieu auction lay tu AuctionManager.
 * Ngoai ra co refresh va tim kiem nhanh cho ca 2 bang.
 */
public class AdminDashboardController {

    @FXML
    private BorderPane root;

    // User table
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

    // Auction table
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

    /** Truy cap kho du lieu .ser cua app. */
    private SerializedDatabase database;
    /** Manager lay danh sach auction dang duoc quan ly. */
    private AuctionManager auctionManager;

    /** Danh sach dong goc cho bang user. */
    private final ObservableList<UserRow> userRows = FXCollections.observableArrayList();
    /** Danh sach dong goc cho bang auction. */
    private final ObservableList<AuctionRow> auctionRows = FXCollections.observableArrayList();

    /**
     * Khoi tao controller sau khi FXML inject xong.
     */
    @FXML
    private void initialize() {
        initDataSource();
        initUserTable();
        initAuctionTable();
        bindActions();
        refreshUsers();
        refreshAuctions();
    }

    /**
     * Khoi tao datasource dung chung voi app.
     */
    private void initDataSource() {
        database = new SerializedDatabase(Path.of("data"));
        auctionManager = AuctionManager.getInstance(database);
    }

    /**
     * Cau hinh bang user: cot, sap xep va tim kiem.
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
     * Cau hinh bang auction: cot, sap xep va tim kiem.
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
     * Gan action cho nut refresh cua 2 bang.
     */
    private void bindActions() {
        btnRefreshUsers.setOnAction(event -> refreshUsers());
        btnRefreshAuctions.setOnAction(event -> refreshAuctions());
        btnDeleteUser.setOnAction(event -> removeSelectedUserRowOnly());
        btnDeleteAuction.setOnAction(event -> stopSelectedAuctionRowOnly());
    }

    /**
     * Nap lai du lieu user tu repository.
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
     * Nap lai du lieu auction tu AuctionManager.
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
     * Predicate tim kiem cho bang user.
     *
     * @param row dong user can kiem tra
     * @param keyword tu khoa tim kiem hien tai
     * @return true neu dong khop tu khoa, nguoc lai false
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
     * Predicate tim kiem cho bang auction.
     *
     * @param row dong auction can kiem tra
     * @param keyword tu khoa tim kiem hien tai
     * @return true neu dong khop tu khoa, nguoc lai false
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
     * Dinh dang gia hien tai de hien thi trong bang.
     *
     * @param auction phien dau gia can lay gia hien tai
     * @return chuoi gia da duoc dinh dang
     */
    private String formatPrice(final Auction auction) {
        if (auction.getItem() == null) {
            return "0";
        }
        return String.format(Locale.ROOT, "%,.0f", auction.getItem().getCurrentPrice());
    }

    /**
     * Xoa dong user dang chon tren UI.
     *
     * <p>Chi tac dong du lieu hien thi trong bang, khong ghi vao database.
     */
    private void removeSelectedUserRowOnly() {
        final UserRow selected = tblUsers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Thong bao", "Vui long chon nguoi dung can xoa tren bang.");
            return;
        }
        userRows.remove(selected);
    }

    /**
     * Dung phien dang chon tren UI.
     *
     * <p>Chi doi trang thai hien thi thanh CANCELED tren bang, khong ghi vao database.
     */
    private void stopSelectedAuctionRowOnly() {
        final AuctionRow selected = tblAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Thong bao", "Vui long chon phien can dung tren bang.");
            return;
        }
        selected.setStatus("CANCELED");
        tblAuctions.refresh();
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

    /**
     * DTO hien thi 1 dong user tren TableView.
     */
    public static class UserRow {
        private final SimpleStringProperty id;
        private final SimpleStringProperty username;
        private final SimpleStringProperty email;
        private final SimpleStringProperty status;

        /**
         * Tao 1 dong hien thi user cho bang.
         *
         * @param id id user
         * @param username ten dang nhap
         * @param email email user
         * @param status trang thai online/offline
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
         * Lay id user.
         *
         * @return id user
         */
        public String getId() {
            return id.get();
        }

        /**
         * Lay ten dang nhap.
         *
         * @return ten dang nhap
         */
        public String getUsername() {
            return username.get();
        }

        /**
         * Lay email user.
         *
         * @return email user
         */
        public String getEmail() {
            return email.get();
        }

        /**
         * Lay trang thai user.
         *
         * @return ONLINE hoac OFFLINE
         */
        public String getStatus() {
            return status.get();
        }
    }

    /**
     * DTO hien thi 1 dong auction tren TableView.
     */
    public static class AuctionRow {
        private final SimpleStringProperty id;
        private final SimpleStringProperty productName;
        private final SimpleStringProperty seller;
        private final SimpleStringProperty currentPrice;
        private final SimpleStringProperty status;

        /**
         * Tao 1 dong hien thi auction cho bang.
         *
         * @param id id phien dau gia
         * @param productName ten san pham
         * @param seller ten nguoi ban
         * @param currentPrice gia hien tai
         * @param status trang thai phien
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
         * Lay id phien.
         *
         * @return id phien
         */
        public String getId() {
            return id.get();
        }

        /**
         * Lay ten san pham.
         *
         * @return ten san pham
         */
        public String getProductName() {
            return productName.get();
        }

        /**
         * Lay ten nguoi ban.
         *
         * @return ten nguoi ban
         */
        public String getSeller() {
            return seller.get();
        }

        /**
         * Lay gia hien tai.
         *
         * @return gia hien tai da format
         */
        public String getCurrentPrice() {
            return currentPrice.get();
        }

        /**
         * Lay trang thai phien.
         *
         * @return trang thai phien
         */
        public String getStatus() {
            return status.get();
        }

        /**
         * Cap nhat trang thai phien tren dong hien thi.
         *
         * @param newStatus trang thai moi
         */
        public void setStatus(final String newStatus) {
            status.set(newStatus);
        }
    }
}
