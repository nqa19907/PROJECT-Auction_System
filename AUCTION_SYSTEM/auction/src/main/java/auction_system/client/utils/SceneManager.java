package auction_system.client.utils;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lớp tiện ích quản lý việc chuyển đổi màn hình (Scene) trong hệ thống.
 */
public final class SceneManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SceneManager.class);

    // Cố định đường dẫn mặc định tới thư mục FXML của Client
    private static final String FXML_BASE_PATH = "/client/fxml/";

    private SceneManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Chuyển đổi màn hình hiển thị dựa trên tên file FXML.
     *
     * @param triggerNode Một component bất kỳ trên màn hình hiện tại để tìm ra Stage gốc
     * @param fxmlName    Tên file FXML cần đổi sang (Ví dụ: "Dashboard.fxml")
     */
    public static void switchScene(Node triggerNode, String fxmlName) {
        switchScene(triggerNode, fxmlName, -1, -1);
    }

    /**
     * Chuyển đổi màn hình hiển thị dựa trên tên file FXML, có tuỳ chỉnh kích thước.
     *
     * @param triggerNode Một component bất kỳ trên màn hình hiện tại để tìm ra Stage gốc
     * @param fxmlName    Tên file FXML cần đổi sang
     * @param width       Chiều rộng cửa sổ (-1 nếu muốn giữ nguyên mặc định)
     * @param height      Chiều cao cửa sổ (-1 nếu muốn giữ nguyên mặc định)
     */
    public static void switchScene(Node triggerNode, String fxmlName, double width, double height) {
        if (triggerNode == null) {
            LOGGER.warn("Không thể chuyển màn hình: Node kích hoạt bị null.");
            return;
        }

        try {
            Stage currentStage = (Stage) triggerNode.getScene().getWindow();
            switchScene(currentStage, fxmlName, width, height);
        } catch (Exception e) {
            LOGGER.error(
                        "Lỗi khi lấy cửa sổ từ Node kích hoạt chuyển cảnh: " + fxmlName, e);
        }
    }

    /**
     * Chuyển đổi màn hình hiển thị trên một Stage cụ thể.
     *
     * @param stage    Stage cần thay đổi Scene
     * @param fxmlName Tên file FXML cần đổi sang
     */
    public static void switchScene(Stage stage, String fxmlName) {
        switchScene(stage, fxmlName, -1, -1);
    }

    /**
     * Chuyển đổi màn hình hiển thị trên một Stage cụ thể, có tuỳ chỉnh kích thước.
     *
     * @param currentStage Stage cần thay đổi Scene
     * @param fxmlName     Tên file FXML cần đổi sang
     * @param width        Chiều rộng cửa sổ mới
     * @param height       Chiều cao cửa sổ mới
     */
    public static void switchScene(Stage currentStage, String fxmlName,
                                    double width, double height) {
        if (currentStage == null) {
            LOGGER.warn("Không thể chuyển màn hình: Stage bị null.");
            return;
        }

        try {
            String fxmlPath = fxmlName.startsWith("/") ? fxmlName : FXML_BASE_PATH + fxmlName;

            LOGGER.info("Đang tiến hành nạp giao diện: " + fxmlPath);

            URL fxmlLocation = SceneManager.class.getResource(fxmlPath);
            if (fxmlLocation == null) {
                throw new IOException("Đường dẫn file FXML không tồn tại: " + fxmlPath);
            }

            Parent root = FXMLLoader.load(fxmlLocation);
            Scene newScene;
            
            // Nếu có truyền kích thước cụ thể, set ngay lúc tạo Scene
            if (width > 0 && height > 0) {
                newScene = new Scene(root, width, height);
            } else {
                newScene = new Scene(root);
            }

            // Tự động nhúng global.css vào mọi Scene mới được tạo ra
            URL cssLocation = SceneManager.class.getResource("/client/css/global.css");
            if (cssLocation != null) {
                newScene.getStylesheets().add(cssLocation.toExternalForm());
                LOGGER.info("Đã nạp thành công global.css cho Scene: " + fxmlName);
            }

            // 1. Tạo một Stage hoàn toàn mới
            Stage newStage = new Stage();
            newStage.setScene(newScene);

            // 2. Đặt tiêu đề theo màn hình mới.
            WindowTitleUtil.applyTitle(newStage, fxmlName);

            // 3. Xử lý kích thước cho cửa sổ mới
            if (width > 0 && height > 0) {
                newStage.setWidth(width);
                newStage.setHeight(height);
                newStage.centerOnScreen(); // Căn giữa màn hình
            }

            // 4. Hiển thị cửa sổ mới và đống cửa sổ cũ
            newStage.show();
            currentStage.close();

            LOGGER.info("Đã chuyển màn hình sang " + fxmlName + " thành công.");
            
        } catch (IOException e) {
            LOGGER.error("Lỗi nghiêm trọng: Không thể nạp file FXML: " + fxmlName, e);
        }
    }
}
