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
     * <p>Mặc định phóng to cửa sổ và nạp danh sách sản phẩm.
     */
    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            Stage stage = (Stage) contentArea.getScene().getWindow();
            if (stage != null) {
                stage.setMaximized(true);
            }
            handleShowItems();
        });

        if (sidebarController != null) {
            setupSidebarCallbacks();
            applySidebarPolicyByRole();
        }

        setupUserProfile();
    }

    /**
     * Gắn callback điều hướng cho Sidebar.
     */
    private void setupSidebarCallbacks() {
        sidebarController.setOnCategorySelected(this::loadItemList);
        sidebarController.setOnPublishItemSelected(this::loadPublishItemView);
        sidebarController.setOnAdminSelected(this::loadAdminView);
    }

    /**
     * Áp dụng chính sách hiển thị Sidebar theo vai trò user hiện tại.
     *
     * <p>Lưu ý: đây là kiểm soát giao diện (client-side).
     * Quyền nghiệp vụ vẫn cần kiểm tra ở server-side.
     */
    private void applySidebarPolicyByRole() {
        User currentUser = AuthService.getInstance().getCurrentUser();
        String roleName = currentUser != null ? currentUser.getRoleName() : null;
        sidebarController.applyPolicy(RoleUiPolicy.sidebarItemsForRole(roleName));
    }

    /**
     * Đồng bộ thông tin user lên khu vực profile.
     */
    private void setupUserProfile() {
        User user = AuthService.getInstance().getCurrentUser();
        if (profileController != null && user != null) {
            profileController.setUserData(user);
        }
    }

    /**
     * Xử lý đăng xuất.
     */
    @FXML
    private void handleSignOut() {
        LOGGER.info("Thực hiện đăng xuất: gọi service để logout khỏi server...");
        AuthService.getInstance().logout(result -> Platform.runLater(() -> {
            if (result.isSuccess()) {
                LOGGER.info("Đăng xuất thành công. Quay về màn hình đăng nhập.");
            } else {
                LOGGER.warn("Đăng xuất có lỗi: {}", result.getErrorMessage());
            }
            SceneManager.switchScene(btnSignOut, ViewConstants.LOGIN_VIEW, 900, 700);
        }));
    }

    /**
     * Điều hướng sang màn hình đăng bán.
     */
    private void loadPublishItemView() {
        LOGGER.info("Chuyển sang giao diện đăng bán.");
        Router.navigateContent(contentArea, ViewConstants.PUBLISH_ITEM_VIEW);
    }

    /**
     * Mở danh sách sản phẩm mặc định.
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
     * @param category danh mục cần lọc
     */
    private void loadItemList(final String category) {
        LOGGER.info("Chuyển sang danh sách, lọc theo danh mục: {}", category);
        ItemListController controller = Router.navigateContentAndGetController(
                contentArea, ViewConstants.ITEM_LIST_VIEW);
        if (controller != null) {
            controller.setFilterCategory(category);
        }
    }

    /**
     * Nạp màn hình admin demo trong vùng content của Dashboard.
     */
    private void loadAdminView() {
        LOGGER.info("Chuyển sang giao diện admin demo trong Dashboard.");
        Router.navigateContent(contentArea, ViewConstants.ADMIN_DEMO_VIEW);
    }
}
