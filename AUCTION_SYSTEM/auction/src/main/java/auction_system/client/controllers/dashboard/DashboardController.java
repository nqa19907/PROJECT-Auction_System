package auction_system.client.controllers.dashboard;

import auction_system.client.controllers.auction.ItemListController;
import auction_system.client.controllers.components.ProfileController;
import auction_system.client.controllers.components.SidebarController;
import auction_system.client.security.RoleUiPolicy;
import auction_system.client.services.AuthService;
import auction_system.client.utils.Router;
import auction_system.client.utils.SceneManager;
import auction_system.client.utils.ViewConstants;
import auction_system.common.constants.AppConstants;
import auction_system.common.models.users.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller điều khiển giao diện Dashboard chính của người dùng.
 */
public class DashboardController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardController.class);
    private static final double loginWindowWidth = 900.0;
    private static final double loginWindowHeight = 700.0;

    @FXML
    private Button btnSignOut;

    @FXML
    private StackPane contentArea;

    @FXML
    private SidebarController sidebarController;

    @FXML
    private ProfileController profileController;

    /**
     * Khởi tạo màn hình Dashboard.
     *
     * <p>Mặc định phóng to cửa sổ, nạp danh sách sản phẩm và áp dụng policy theo role.
     */
    @FXML
    public void initialize() {
        Platform.runLater(this::maximizeAndLoadDefaultContent);
        setupSidebarActions();
        setupUserProfile();
    }

    /**
     * Phóng to cửa sổ và nạp danh sách sản phẩm mặc định.
     */
    private void maximizeAndLoadDefaultContent() {
        final Stage stage = (Stage) contentArea.getScene().getWindow();

        if (stage != null) {
            stage.setMaximized(true);
        }

        handleShowItems();
    }

    /**
     * Gắn callback điều hướng và policy hiển thị cho Sidebar.
     */
    private void setupSidebarActions() {
        if (sidebarController == null) {
            return;
        }

        sidebarController.setOnCategorySelected(this::loadItemList);
        sidebarController.setOnPublishItemSelected(this::loadPublishItemView);
        // Nối sự kiện cho mục mới "Quản lý phiên của tôi" (hiện tại chỉ UI).
        sidebarController.setOnMyAuctionManagementSelected(this::loadMyAuctionManagementView);
        sidebarController.setOnAdminSelected(this::loadAdminView);
        applySidebarPolicyByRole();
    }

    /**
     * Áp dụng chính sách hiển thị Sidebar theo vai trò user hiện tại.
     *
     * <p>Đây chỉ là kiểm soát giao diện phía client. Quyền nghiệp vụ vẫn phải
     * được kiểm tra ở server.
     */
    private void applySidebarPolicyByRole() {
        final User currentUser = AuthService.getInstance().getCurrentUser();
        final String roleName = currentUser != null ? currentUser.getRoleName() : null;
        sidebarController.applyPolicy(RoleUiPolicy.sidebarItemsForRole(roleName));
    }

    /**
     * Đồng bộ thông tin user lên khu vực profile.
     */
    private void setupUserProfile() {
        final User user = AuthService.getInstance().getCurrentUser();

        if (profileController != null && user != null) {
            profileController.setUserData(user);
        }
    }

    /**
     * Xử lý sự kiện đăng xuất khỏi hệ thống.
     */
    @FXML
    private void handleSignOut() {
        LOGGER.info("Thực hiện đăng xuất: gửi lệnh LOGOUT tới server.");

        AuthService.getInstance().logout(result -> Platform.runLater(() -> {
            if (!result.isSuccess()) {
                LOGGER.warn("Đăng xuất có lỗi: {}", result.getErrorMessage());
            }

            SceneManager.switchScene(
                    btnSignOut,
                    ViewConstants.LOGIN_VIEW,
                    loginWindowWidth,
                    loginWindowHeight);
        }));
    }

    /**
     * Chuyển sang giao diện đăng bán sản phẩm.
     */
    private void loadPublishItemView() {
        LOGGER.info("Chuyển sang giao diện đăng bán.");
        Router.navigateContent(contentArea, ViewConstants.PUBLISH_ITEM_VIEW);
    }

    /**
     * Hiển thị toàn bộ sản phẩm trên Dashboard.
     */
    @FXML
    private void handleShowItems() {
        if (sidebarController != null) {
            sidebarController.setActiveSidebarItem(AppConstants.UI_ID_CATEGORY_ALL);
        }

        loadItemList(AppConstants.CATEGORY_ALL);
    }

    /**
     * Nạp danh sách sản phẩm theo danh mục.
     *
     * @param category danh mục sản phẩm cần hiển thị
     */
    private void loadItemList(final String category) {
        LOGGER.info("Chuyển sang danh sách sản phẩm, danh mục: {}", category);

        final ItemListController controller = Router.navigateContentAndGetController(
                contentArea,
                ViewConstants.ITEM_LIST_VIEW);

        if (controller != null) {
            controller.setFilterCategory(category);
        }
    }

    /**
     * Nạp màn hình admin trong vùng content của Dashboard.
     */
    private void loadAdminView() {
        LOGGER.info("Chuyển sang giao diện admin trong Dashboard.");
        Router.navigateContent(contentArea, ViewConstants.ADMIN_DEMO_VIEW);
    }

    // Mục mới: hiện tại chỉ để hoàn thiện UI, chưa chuyển màn hình.
    private void loadMyAuctionManagementView() {
        LOGGER.info("Đã bấm mục 'Quản lý phiên của tôi' (chưa có chức năng).");
    }
}
