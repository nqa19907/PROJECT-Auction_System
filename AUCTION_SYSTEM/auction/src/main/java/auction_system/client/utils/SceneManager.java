package auction_system.client.utils;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Lớp tiện ích quản lý việc chuyển đổi màn hình (Scene) trong hệ thống.
 */
public final class SceneManager {
    private static final Logger LOGGER = Logger.getLogger(SceneManager.class.getName());

    // Cố định đường dẫn mặc định tới thư mục FXML của Client
    private static final String FXML_BASE_PATH = "/client/fxml/";

    private SceneManager() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Chuyển đổi màn hình hiển thị dựa trên tên file FXML.
     *
     * @param triggerNode Một component bất kỳ trên màn hình hiện tại để tìm ra Stage gốc
     * @param fxmlName Tên file FXML cần đổi sang (Ví dụ: "Dashboard.fxml")
     */
    public static void switchScene(Node triggerNode, String fxmlName) {
        if (triggerNode == null) {
            LOGGER.warning("Không thể chuyển màn hình: Node kích hoạt bị null.");
            return;
        }

        Stage currentStage = (Stage) triggerNode.getScene().getWindow();
        switchScene(currentStage, fxmlName);
    }

    /**
     * Chuyển đổi màn hình hiển thị trên một Stage cụ thể.
     *
     * @param stage Stage cần thay đổi Scene
     * @param fxmlName Tên file FXML cần đổi sang
     */
    public static void switchScene(Stage stage, String fxmlName) {
        if (stage == null) {
            LOGGER.warning("Không thể chuyển màn hình: Stage bị null.");
            return;
        }

        try {
            String fxmlPath = fxmlName.startsWith("/") ? fxmlName : FXML_BASE_PATH + fxmlName;

            LOGGER.info("Đang tiến hành nạp giao diện: " + fxmlPath);

            URL fxmlLocation = SceneManager.class.getResource(fxmlPath);
            if (fxmlLocation == null) {
                // Fallback trường hợp dev gõ sai folder root hoặc muốn nạp từ /fxml/
                fxmlLocation = SceneManager.class.getResource("/fxml/" + fxmlName);
                if (fxmlLocation == null) {
                    throw new IOException("Đường dẫn file FXML không tồn tại: " + fxmlPath);
                }
            }

            Parent root = FXMLLoader.load(fxmlLocation);
            Scene newScene = new Scene(root);
            
            stage.setScene(newScene);
            stage.show();
            
            LOGGER.info("Đã chuyển màn hình sang " + fxmlName + " thành công.");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Lỗi nghiêm trọng: Không thể nạp file FXML: " + fxmlName, e);
        }
    }
}