package auction_system.client.controllers.components;

import auction_system.client.security.SidebarItem;
import auction_system.client.utils.CategoryUtil;
import java.util.Set;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Controller điều khiển giao diện thanh điều hướng bên trái (Sidebar).
 *
 * <p>Controller này hỗ trợ:
 * <ul>
 *   <li>Bật/tắt từng mục theo role thông qua policy.</li>
 *   <li>Điều hướng danh mục, đăng bán và admin demo.</li>
 * </ul>
 */
public class SidebarController {

    private static final String STYLE_CLASS_CATEGORY_OPTION = "category-option";
    private static final String STYLE_CLASS_ACTIVE = "active";
    private static final String UI_ID_PUBLISH_ITEM = "publishItem";
    /** ID của mục điều hướng admin demo trong Sidebar. */
    public static final String ADMIN_DEMO_VIEW = "testAdminView";

    @FXML
    private VBox categorySidebar;
    @FXML
    private HBox publishItem;
    @FXML
    private HBox testAdminView;

    /** Callback chọn danh mục. */
    private Consumer<String> onCategorySelected = category -> { };
    /** Callback mở màn hình đăng bán. */
    private Runnable onPublishItemSelected = () -> { };
    /** Callback mở màn hình admin demo. */
    private Runnable onAdminSelected = () -> { };

    /**
     * Đăng ký callback khi người dùng chọn danh mục.
     *
     * @param callback hàm xử lý danh mục được chọn
     */
    public void setOnCategorySelected(final Consumer<String> callback) {
        this.onCategorySelected = callback;
    }

    /**
     * Đăng ký callback khi người dùng chọn mục đăng bán.
     *
     * @param callback hàm xử lý mở màn hình đăng bán
     */
    public void setOnPublishItemSelected(final Runnable callback) {
        this.onPublishItemSelected = callback;
    }

    /**
     * Đăng ký callback khi người dùng chọn mục admin demo.
     *
     * @param callback hàm xử lý mở màn hình admin demo
     */
    public void setOnAdminSelected(final Runnable callback) {
        this.onAdminSelected = callback;
    }

    /**
     * Áp dụng policy hiển thị cho Sidebar theo tập quyền hiện tại.
     *
     * <p>Chỉ tác động hiển thị trên giao diện:
     * <ul>
     *   <li>{@code visible}: có nhìn thấy hay không.</li>
     *   <li>{@code managed}: có chiếm chỗ trong layout hay không.</li>
     * </ul>
     *
     * @param visibleItems tập mục được phép hiển thị
     */
    public void applyPolicy(final Set<SidebarItem> visibleItems) {
        if (visibleItems == null) {
            return;
        }
        setSidebarItemVisible(SidebarItem.PUBLISH_ITEM,
                visibleItems.contains(SidebarItem.PUBLISH_ITEM));
        setSidebarItemVisible(SidebarItem.ADMIN_DEMO,
                visibleItems.contains(SidebarItem.ADMIN_DEMO));
    }

    /**
     * Giữ tương thích với logic cũ: hiển thị/ẩn mục admin demo.
     *
     * @param visible true để hiển thị, false để ẩn
     */
    public void setAdminDemoVisible(final boolean visible) {
        setNodeVisibility(testAdminView, visible);
    }

    /**
     * Giữ tương thích với logic cũ: hiển thị/ẩn mục đăng bán.
     *
     * @param visible true để hiển thị, false để ẩn
     */
    public void setPublishItemVisible(final boolean visible) {
        setNodeVisibility(publishItem, visible);
    }

    /**
     * Xử lý click vào danh mục.
     *
     * @param event sự kiện click chuột
     */
    @FXML
    private void handleCategoryFilter(final MouseEvent event) {
        Node source = (Node) event.getSource();
        String id = source.getId();
        String category = CategoryUtil.getCategoryByUiId(id);
        setActiveSidebarItem(id);
        onCategorySelected.accept(category);
    }

    /**
     * Xử lý click vào mục đăng bán.
     */
    @FXML
    private void handlePublishItem() {
        setActiveSidebarItem(UI_ID_PUBLISH_ITEM);
        onPublishItemSelected.run();
    }

    /**
     * Xử lý click vào mục admin demo.
     */
    @FXML
    private void loadAdminView() {
        setActiveSidebarItem(ADMIN_DEMO_VIEW);
        onAdminSelected.run();
    }

    /**
     * Cập nhật trạng thái active cho mục đang được chọn.
     *
     * @param activeId id của mục cần active
     */
    public void setActiveSidebarItem(final String activeId) {
        if (categorySidebar == null || activeId == null) {
            return;
        }
        for (Node node : categorySidebar.getChildren()) {
            if (node.getStyleClass().contains(STYLE_CLASS_CATEGORY_OPTION)) {
                node.getStyleClass().remove(STYLE_CLASS_ACTIVE);
                if (activeId.equals(node.getId())) {
                    node.getStyleClass().add(STYLE_CLASS_ACTIVE);
                }
            }
        }
    }

    /**
     * Hiển thị/ẩn một mục sidebar theo loại mục.
     *
     * @param item loại mục cần đổi hiển thị
     * @param visible trạng thái hiển thị mong muốn
     */
    private void setSidebarItemVisible(final SidebarItem item, final boolean visible) {
        Node node = resolveNode(item);
        setNodeVisibility(node, visible);
    }

    /**
     * Ánh xạ loại mục sidebar sang node FXML tương ứng.
     *
     * @param item loại mục sidebar
     * @return node tương ứng, hoặc null nếu chưa hỗ trợ
     */
    private Node resolveNode(final SidebarItem item) {
        return switch (item) {
            case PUBLISH_ITEM -> publishItem;
            case ADMIN_DEMO -> testAdminView;
        };
    }

    /**
     * Đặt hiển thị cho node và đảm bảo layout không giữ khoảng trống khi ẩn.
     *
     * @param node node cần áp dụng
     * @param visible true để hiển thị, false để ẩn
     */
    private void setNodeVisibility(final Node node, final boolean visible) {
        if (node == null) {
            return;
        }
        node.setManaged(visible);
        node.setVisible(visible);
    }
}
