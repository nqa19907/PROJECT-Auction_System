package auction_system.client.controllers.auction;

import auction_system.client.services.ItemPublishService;
import auction_system.client.utils.Router;
import auction_system.client.utils.ViewConstants;
import auction_system.common.constants.AppConstants;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller xử lý màn hình đăng bán sản phẩm.
 */
public class PublishItemController implements Initializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishItemController.class);

    @FXML
    private TextField fieldTenTaiSan;
    @FXML
    private ComboBox<String> comboCategory;
    @FXML
    private ComboBox<String> comboCondition;
    @FXML
    private TextArea fieldDescription;
    @FXML
    private TextField fieldStartingTime;
    @FXML
    private TextField fieldEndingTime;
    @FXML
    private TextField fieldStartingPrice;
    @FXML
    private TextField fieldBidStep;
    @FXML
    private Label lblError;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnConfirm;

    /**
     * Khởi tạo dữ liệu mặc định cho màn hình đăng bán.
     *
     * @param url đường dẫn tài nguyên
     * @param resourceBundle bundle tài nguyên
     */
    @Override
    public void initialize(final URL url, final ResourceBundle resourceBundle) {
        comboCategory.getItems().setAll(
                AppConstants.CATEGORY_ART,
                AppConstants.CATEGORY_ELECTRONIC,
                AppConstants.CATEGORY_VEHICLE);

        comboCondition.getItems().setAll(
                "Mới (100%)",
                "Như mới (95%+)",
                "Tốt (80–94%)",
                "Trung bình");

        clearError();
    }

    /**
     * Quay về màn hình danh sách sản phẩm.
     *
     * @param event sự kiện bấm nút
     */
    @FXML
    private void handleGoDashboard(final ActionEvent event) {
        Router.navigateContent(btnCancel, ViewConstants.ITEM_LIST_VIEW);
        Router.updateSidebarActive(btnCancel, AppConstants.UI_ID_CATEGORY_ALL);
    }

    /**
     * Hủy đăng bán và quay về danh sách sản phẩm.
     *
     * @param event sự kiện bấm nút
     */
    @FXML
    private void handleCancel(final ActionEvent event) {
        handleGoDashboard(event);
    }

    /**
     * Xử lý khi người dùng xác nhận đăng bán sản phẩm.
     *
     * @param event sự kiện bấm nút
     */
    @FXML
    private void handleConfirm(final ActionEvent event) {
        try {
            clearError();

            String itemName = readRequired(fieldTenTaiSan, "Tên tài sản không được để trống.");
            String category = readRequired(comboCategory, "Vui lòng chọn danh mục.");
            String condition = readRequired(comboCondition, "Vui lòng chọn tình trạng.");
            String description = readRequired(fieldDescription, "Mô tả không được để trống.");
            double startPrice = parsePositiveMoney(fieldStartingPrice.getText(), "giá khởi điểm");
            LocalDateTime startTime = parseDateTime(
                        fieldStartingTime.getText(), "thời gian bắt đầu");
            LocalDateTime endTime = parseDateTime(fieldEndingTime.getText(), "thời gian kết thúc");

            if (!endTime.isAfter(startTime)) {
                showError("Thời gian kết thúc phải sau thời gian bắt đầu.");
                return;
            }

            setLoadingState(true);
            ItemPublishService.getInstance().publishItem(
                    category,
                    itemName,
                    description,
                    condition,
                    startPrice,
                    startTime,
                    endTime,
                    this::handlePublishResult);
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    /**
     * Xử lý kết quả đăng bán trả về từ service.
     *
     * @param success true nếu đăng bán thành công
     * @param message thông báo kết quả
     */
    private void handlePublishResult(final boolean success, final String message) {
        setLoadingState(false);

        if (success) {
            LOGGER.info("Đăng bán sản phẩm thành công.");
            Router.navigateContent(btnConfirm, ViewConstants.ITEM_LIST_VIEW);
            Router.updateSidebarActive(btnConfirm, AppConstants.UI_ID_CATEGORY_ALL);
            return;
        }

        showError(message);
    }

    /**
     * Đọc dữ liệu bắt buộc từ ô nhập liệu.
     *
     * @param control ô nhập liệu cần đọc
     * @param errorMessage thông báo lỗi nếu dữ liệu rỗng
     * @return giá trị đã được trim
     */
    private String readRequired(final TextInputControl control, final String errorMessage) {
        String value = control.getText();
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    /**
     * Đọc dữ liệu bắt buộc từ hộp chọn.
     *
     * @param comboBox hộp chọn cần đọc
     * @param errorMessage thông báo lỗi nếu chưa chọn
     * @return giá trị đã chọn
     */
    private String readRequired(final ComboBox<String> comboBox, final String errorMessage) {
        String value = comboBox.getValue();
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    /**
     * Chuyển dữ liệu text thành số tiền dương.
     *
     * @param value giá trị người dùng nhập
     * @param fieldName tên trường dùng trong thông báo lỗi
     * @return số tiền hợp lệ
     */
    private double parsePositiveMoney(final String value, final String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Trường " + fieldName + " không được để trống.");
        }

        try {
            double amount = Double.parseDouble(value.trim());
            if (amount <= 0) {
                throw new IllegalArgumentException("Trường " + fieldName + " phải lớn hơn 0.");
            }
            return amount;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Trường " + fieldName + " phải là số hợp lệ.");
        }
    }

    /**
     * Chuyển dữ liệu text thành thời gian.
     *
     * <p>Định dạng hợp lệ: yyyy-MM-ddTHH:mm, ví dụ 2026-05-22T14:30.</p>
     *
     * @param value giá trị người dùng nhập
     * @param fieldName tên trường dùng trong thông báo lỗi
     * @return thời gian hợp lệ
     */
    private LocalDateTime parseDateTime(final String value, final String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Trường " + fieldName + " không được để trống.");
        }

        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Trường " + fieldName + " phải có dạng yyyy-MM-ddTHH:mm.");
        }
    }

    /**
     * Hiển thị thông báo lỗi trên giao diện.
     *
     * @param message nội dung lỗi
     */
    private void showError(final String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    /**
     * Xóa thông báo lỗi khỏi giao diện.
     */
    private void clearError() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    /**
     * Bật hoặc tắt trạng thái đang gửi request.
     *
     * @param loading true nếu đang gửi request
     */
    private void setLoadingState(final boolean loading) {
        btnConfirm.setDisable(loading);
        btnCancel.setDisable(loading);
    }
}