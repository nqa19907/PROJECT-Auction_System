package auction_system.client.controllers.auction;

import auction_system.client.network.NetworkClient;
import auction_system.client.services.ItemPublishService;
import auction_system.client.services.ItemPublishService.PublishItemCallback;
import auction_system.client.services.MyAuctionRow;
import auction_system.client.services.ProductImageStorage;
import auction_system.client.utils.CheckBoxIconUtil;
import auction_system.client.utils.Router;
import auction_system.client.utils.SceneManager;
import auction_system.client.utils.ViewConstants;
import auction_system.common.constants.AppConstants;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller xử lý màn hình đăng bán sản phẩm.
 */
public class PublishItemController implements Initializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishItemController.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
    private CheckBox chkAntiSniping;
    @FXML
    private Label lblError;
    @FXML
    private Label lblPageTitle;
    @FXML
    private Label lblSelectedImage;
    @FXML
    private Button btnChooseImage;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnConfirm;

    private Path selectedImageSource;
    private boolean editMode;
    private String editingAuctionId;

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

        CheckBoxIconUtil.apply(chkAntiSniping);
        clearError();
        updateSelectedImageLabel();
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
     * Chọn ảnh sản phẩm từ máy người dùng.
     *
     * @param event sự kiện bấm nút
     */
    @FXML
    private void handleChooseImage(final ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().setAll(
                new FileChooser.ExtensionFilter("Ảnh sản phẩm", "*.jpg", "*.jpeg", "*.png",
                        "*.gif"));

        // Mở hộp thoại chọn ảnh và lưu lại file nguồn để copy khi xác nhận.
        Window owner = btnChooseImage.getScene() == null
                ? null
                : btnChooseImage.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(owner);
        if (selectedFile == null) {
            return;
        }

        selectedImageSource = selectedFile.toPath();
        updateSelectedImageLabel();
        clearError();
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

            if (editMode) {
                submitEditAuction();
                return;
            }

            final String itemName = readRequired(
                    fieldTenTaiSan,
                    "Tên tài sản không được để trống.");
            final String category = readRequired(comboCategory, "Vui lòng chọn danh mục.");
            final String condition = readRequired(comboCondition, "Vui lòng chọn tình trạng.");
            final String description = readRequired(fieldDescription, "Mô tả không được để trống.");
            final double startPrice = parsePositiveMoney(
                    fieldStartingPrice.getText(),
                    "giá khởi điểm");

            LocalDateTime startTime = parseDateTime(
                    fieldStartingTime.getText(),
                    "thời gian bắt đầu");
            LocalDateTime endTime = parseDateTime(
                    fieldEndingTime.getText(),
                    "thời gian kết thúc");

            if (!endTime.isAfter(startTime)) {
                showError("Thời gian kết thúc phải sau thời gian bắt đầu.");
                return;
            }

            // Đảm bảo còn kết nối trước khi copy ảnh và gửi request đăng bán.
            if (!NetworkClient.getInstance().ensureConnected()) {
                showError("Đã mất kết nối tới Server. Vui lòng khởi động Server và thử lại.");
                return;
            }

            // Copy ảnh đã chọn vào thư mục dữ liệu app trước khi gửi request.
            String imagePath = storeSelectedImage();

            setLoadingState(true);
            ItemPublishService.getInstance().publishItem(
                category,
                itemName,
                description,
                condition,
                startPrice,
                startTime,
                endTime,
                imagePath,
                chkAntiSniping.isSelected(),
                (success, message) -> Platform.runLater(
                    () -> handlePublishResult(success, message)));
    
                
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        } catch (IOException exception) {
            LOGGER.warn("Không thể lưu ảnh sản phẩm đã chọn.", exception);
            showError("Không thể lưu ảnh sản phẩm. Vui lòng chọn ảnh khác.");
        }

    }

    /**
     * Chuyển form sang chế độ chỉnh sửa và nạp dữ liệu phiên hiện có.
     *
     * @param row dữ liệu phiên cần chỉnh sửa
     */
    public void startEditMode(final MyAuctionRow row) {
        editMode = true;
        editingAuctionId = row.getId();
        lblPageTitle.setText("Chỉnh sửa phiên đấu giá");
        btnConfirm.setText("Lưu thay đổi");

        fieldTenTaiSan.setText(row.getProductName());
        comboCategory.setValue(row.getCategory());
        comboCondition.setValue(row.getCondition());
        fieldDescription.setText(row.getDescription());

        fieldStartingPrice.clear();
        fieldStartingPrice.setPromptText("Không chỉnh sửa ở màn hình này");
        fieldStartingPrice.setDisable(true);
        fieldStartingTime.setText(formatDateTimeForInput(row.getStartTime()));
        fieldStartingTime.setDisable(true);
        fieldEndingTime.setText(formatDateTimeForInput(row.getEndTime()));
        fieldBidStep.setDisable(true);
        chkAntiSniping.setDisable(true);
        btnChooseImage.setDisable(true);
    }

    /**
     * Gửi request cập nhật phiên hiện tại.
     */
    private void submitEditAuction() {
        ItemPublishService.getInstance().updateMyAuction(
                editingAuctionId,
                readRequired(comboCategory, "Vui lòng chọn danh mục."),
                readRequired(fieldTenTaiSan, "Tên tài sản không được để trống."),
                readRequired(fieldDescription, "Mô tả không được để trống."),
                readRequired(comboCondition, "Vui lòng chọn tình trạng."),
                parseDateTime(fieldEndingTime.getText(), "thời gian kết thúc"),
                (success, message) -> Platform.runLater(
                        () -> handleUpdateResult(success, message)));
        setLoadingState(true);
    }

    /**
     * Lưu ảnh đã chọn vào thư mục dữ liệu ứng dụng.
     *
     * @return đường dẫn ảnh đã lưu hoặc chuỗi rỗng nếu chưa chọn ảnh
     * @throws IOException nếu không copy được ảnh
     */
    private String storeSelectedImage() throws IOException {
        if (selectedImageSource == null) {
            return "";
        }

        // Lưu ảnh ổn định để card sản phẩm không phụ thuộc file gốc.
        return ProductImageStorage.getInstance()
                .storeImage(selectedImageSource)
                .toString();
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
            handleGoDashboard(null);
            return;
        }

        showError(message);
    }

    private void handleUpdateResult(final boolean success, final String message) {
        setLoadingState(false);
        if (success) {
            LOGGER.info("Cập nhật phiên đấu giá thành công.");
            Router.navigateContent(btnConfirm, ViewConstants.MY_AUCTION_MANAGEMENT_VIEW);
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
     * <p>Định dạng hợp lệ: {@code dd/MM/yyyy HH:mm}, ví dụ {@code 22/05/2026 14:30}.
     *
     * @param value giá trị người dùng nhập
     * @param fieldName tên trường dùng trong thông báo lỗi
     * @return thời gian hợp lệ
     */
    private LocalDateTime parseDateTime(final String value, final String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Trường " + fieldName + " không được để trống.");
        }

        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Trường "
                            + fieldName
                            + " phải có dạng dd/MM/yyyy HH:mm, ví dụ 22/05/2026 14:30.");
        }
    }

    private String formatDateTimeForInput(final String rawDateTime) {
        if (rawDateTime == null || rawDateTime.trim().isEmpty()) {
            return "";
        }
        try {
            return LocalDateTime.parse(rawDateTime.trim()).format(DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            return rawDateTime;
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
     * Cập nhật nhãn tên ảnh đã chọn.
     */
    private void updateSelectedImageLabel() {
        if (lblSelectedImage == null) {
            return;
        }

        // Hiển thị tên file ảnh để người dùng biết ảnh nào đang được chọn.
        String imageName = selectedImageSource == null
                ? "Chưa chọn ảnh"
                : selectedImageSource.getFileName().toString();
        lblSelectedImage.setText(imageName);
    }

    /**
     * Bật hoặc tắt trạng thái đang gửi request.
     *
     * @param loading true nếu đang gửi request
     */
    private void setLoadingState(final boolean loading) {
        btnConfirm.setDisable(loading);
        btnCancel.setDisable(loading);
        btnChooseImage.setDisable(loading || editMode);
    }
}
