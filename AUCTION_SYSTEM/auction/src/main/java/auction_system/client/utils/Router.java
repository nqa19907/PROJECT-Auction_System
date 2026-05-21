package auction_system.client.utils;

import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp tiện ích hỗ trợ điều hướng giao diện (chuyển trang) bên trong khu vực Content của Dashboard.
 */
public class Router {
    private static final Logger LOGGER = LoggerFactory.getLogger(Router.class);

    /**
     * Chuyển đổi nội dung giao diện bình thường.
     *
     * @param sourceNode Node hiện tại để lấy Scene.
     * @param fxmlPath Đường dẫn tới file FXML.
     */
    public static void navigateContent(Node sourceNode, String fxmlPath) {
        navigateContentAndGetController(sourceNode, fxmlPath);
    }

    /**
     * Chuyển đổi nội dung và trả về Controller của màn hình mới (Dùng khi cần truyền dữ liệu).
     *
     * @param <T> Kiểu của Controller.
     * @param sourceNode Node hiện tại để lấy Scene.
     * @param fxmlPath Đường dẫn tới file FXML.
     * @return Controller của màn hình được nạp, hoặc null nếu có lỗi.
     */
    public static <T> T navigateContentAndGetController(Node sourceNode, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(Router.class.getResource(fxmlPath));
            Node view = loader.load();
            
            Scene scene = sourceNode.getScene();
            if (scene != null) {
                // Tra cứu vùng hiển thị nội dung chính của Dashboard
                StackPane contentArea = (StackPane) scene.lookup("#contentArea");
                if (contentArea != null) {
                    // Thay thế toàn bộ giao diện cũ bằng giao diện mới
                    contentArea.getChildren().setAll(view);
                } else {
                    LOGGER.warn("Không tìm thấy #contentArea trong Scene.");
                }
            }
            return loader.getController();
        } catch (IOException e) {
            LOGGER.error("Lỗi khi chuyển trang đến: " + fxmlPath, e);
            return null;
        }
    }

    /**
     * Cập nhật CSS trạng thái đang chọn ('active') cho Sidebar.
     *
     * @param sourceNode Node hiện tại để lấy Scene.
     * @param activeId ID của phần tử sidebar cần được đánh dấu là active.
     */
    public static void updateSidebarActive(Node sourceNode, String activeId) {
        Scene scene = sourceNode.getScene();
        if (scene == null) {
            return;
        }

        // Tìm Sidebar trong Scene hiện tại
        VBox sidebar = (VBox) scene.lookup("#categorySidebar");
        if (sidebar != null) {
            for (Node node : sidebar.getChildren()) {
                // Xóa highlight cũ và bật highlight cho mục được chọn
                node.getStyleClass().remove("active");
                if (activeId.equals(node.getId())) {
                    node.getStyleClass().add("active");
                }
            }
        }
    }
}