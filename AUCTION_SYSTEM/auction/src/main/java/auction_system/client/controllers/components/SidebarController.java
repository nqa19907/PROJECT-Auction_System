package auction_system.client.controllers.components;

import auction_system.client.utils.CategoryUtil;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

/**
 * Controller điều khiển giao diện thanh điều hướng bên trái (Sidebar).
 */
public class SidebarController {

    // Các hằng số (constants) để tránh hardcode (magic strings)
    private static final String STYLE_CLASS_CATEGORY_OPTION = "category-option";
    private static final String STYLE_CLASS_ACTIVE = "active";
    private static final String UI_ID_PUBLISH_ITEM = "publishItem";

    @FXML
    private VBox categorySidebar;

    // Áp dụng Null Object Pattern (empty lambda) để tránh phải check null mỗi khi gọi
    private Consumer<String> onCategorySelected = (c) -> {};
    private Runnable onPublishItemSelected = () -> {};

    public void setOnCategorySelected(Consumer<String> callback) {
        this.onCategorySelected = callback;
    }

    public void setOnPublishItemSelected(Runnable callback) {
        this.onPublishItemSelected = callback;
    }

    @FXML
    private void handleCategoryFilter(MouseEvent event) {
        Node source = (Node) event.getSource();
        String id = source.getId();
        
        // Tra cứu 1 dòng duy nhất: lấy category tương ứng, 
        // nếu không thấy thì về mặc định CATEGORY_ALL
        String category = CategoryUtil.getCategoryByUiId(id);

        setActiveSidebarItem(id);
        
        // Báo cho DashboardController tải danh mục này
        onCategorySelected.accept(category);
    }

    @FXML
    private void handlePublishItem() {
        setActiveSidebarItem(UI_ID_PUBLISH_ITEM);
        // Báo cho DashboardController hiển thị màn hình Đăng Bán
        onPublishItemSelected.run();
    }

    /**
     * Cập nhật trạng thái "active" (đang được chọn) cho một mục trên sidebar.
     *
     * @param activeId ID của thẻ UI cần làm nổi bật.
     */
    public void setActiveSidebarItem(String activeId) {
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
}
