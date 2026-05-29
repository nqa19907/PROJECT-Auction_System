package auction_system.client.controllers.auction;

import auction_system.client.services.ItemPublishService;
import auction_system.client.services.MyAuctionRow;
import auction_system.client.utils.Router;
import auction_system.client.utils.ViewConstants;
import auction_system.common.constants.AppConstants;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ResourceBundle;
import javafx.application.Platform;
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
 * Controller xử lý màn hình đăng bán/chỉnh sửa phiên.
 */
public class PublishItemController implements Initializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishItemController.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label lblPageTitle;
    @FXML private TextField fieldTenTaiSan;
    @FXML private ComboBox<String> comboCategory;
    @FXML private ComboBox<String> comboCondition;
    @FXML private TextArea fieldDescription;
    @FXML private TextField fieldStartingTime;
    @FXML private TextField fieldEndingTime;
    @FXML private TextField fieldStartingPrice;
    @FXML private TextField fieldBidStep;
    @FXML private Label lblError;
    @FXML private Button btnCancel;
    @FXML private Button btnConfirm;

    /** Đánh dấu màn hình hiện tại đang ở chế độ chỉnh sửa phiên. */
    private boolean editMode;
    /** Lưu id phiên cần chỉnh sửa khi vào edit mode. */
    private String editingAuctionId;

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
     * Chuyển form sang chế độ chỉnh sửa và nạp sẵn dữ liệu cũ.
     *
     * @param row dữ liệu phiên do user đã đăng trước đó
     */
    public void startEditMode(final MyAuctionRow row) {
        this.editMode = true;
        this.editingAuctionId = row.getId();

        lblPageTitle.setText("Chỉnh sửa phiên đấu giá");
        btnConfirm.setText("Lưu thay đổi");

        fieldTenTaiSan.setText(row.getProductName());
        comboCategory.setValue(row.getCategory());
        comboCondition.setValue(row.getCondition());
        fieldDescription.setText(row.getDescription());

        // Theo yêu cầu: không cho sửa giá khởi điểm và thời gian bắt đầu.
        fieldStartingPrice.clear();
        fieldStartingPrice.setPromptText("Không chỉnh sửa ở màn hình này");
        fieldStartingPrice.setDisable(true);

        // Giữ ô thời gian bắt đầu bị khóa, nhưng hiển thị đúng thời gian ban đầu đã nhập.
        fieldStartingTime.setText(formatDateTimeForInput(row.getStartTime()));
        fieldStartingTime.setPromptText("dd/MM/yyyy HH:mm");
        fieldStartingTime.setDisable(true);

        fieldEndingTime.clear();
        fieldEndingTime.setPromptText("Không chỉnh sửa ở màn hình này");
        // Cho phép sửa thời gian kết thúc.
        // Hiển thị sẵn giá trị cũ để user chỉnh.
        fieldEndingTime.setText(formatDateTimeForInput(row.getEndTime()));
        fieldEndingTime.setPromptText("dd/MM/yyyy HH:mm");
        fieldEndingTime.setDisable(false);
        fieldBidStep.setDisable(true);
    }

    @FXML
    private void handleGoDashboard(final ActionEvent event) {
        Router.navigateContent(btnCancel, ViewConstants.ITEM_LIST_VIEW);
        Router.updateSidebarActive(btnCancel, AppConstants.UI_ID_CATEGORY_ALL);
    }

    @FXML
    private void handleCancel(final ActionEvent event) {
        handleGoDashboard(event);
    }

    @FXML
    private void handleConfirm(final ActionEvent event) {
        try {
            clearError();
            if (editMode) {
                submitEditAuction();
                return;
            }
            submitPublishAuction();
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    /**
     * Gửi request tạo phiên mới như luồng cũ.
     */
    private void submitPublishAuction() {
        final String itemName =
                readRequired(fieldTenTaiSan, "Tên tài sản không được để trống.");
        final String category = readRequired(comboCategory, "Vui lòng chọn danh mục.");
        final String condition = readRequired(comboCondition, "Vui lòng chọn tình trạng.");
        final String description =
                readRequired(fieldDescription, "Mô tả không được để trống.");
        final double startPrice =
                parsePositiveMoney(fieldStartingPrice.getText(), "giá khởi điểm");
        final LocalDateTime startTime =
                parseDateTime(fieldStartingTime.getText(), "thời gian bắt đầu");
        final LocalDateTime endTime =
                parseDateTime(fieldEndingTime.getText(), "thời gian kết thúc");

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
                (success, message) ->
                        Platform.runLater(() -> handlePublishResult(success, message)));
    }

    /**
     * Gửi request cập nhật phiên.
     */
    private void submitEditAuction() {
        final String itemName =
                readRequired(fieldTenTaiSan, "Tên tài sản không được để trống.");
        final String category = readRequired(comboCategory, "Vui lòng chọn danh mục.");
        final String condition = readRequired(comboCondition, "Vui lòng chọn tình trạng.");
        final String description =
                readRequired(fieldDescription, "Mô tả không được để trống.");

        setLoadingState(true);
        ItemPublishService.getInstance().updateMyAuction(
                editingAuctionId,
                category,
                itemName,
                description,
                condition,
                parseDateTime(fieldEndingTime.getText(), "thời gian kết thúc"),
                (success, message) ->
                        Platform.runLater(() -> handleUpdateResult(success, message)));
    }

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

    private String readRequired(final TextInputControl control, final String errorMessage) {
        final String value = control.getText();
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    private String readRequired(final ComboBox<String> comboBox, final String errorMessage) {
        final String value = comboBox.getValue();
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value.trim();
    }

    private double parsePositiveMoney(final String value, final String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Trường " + fieldName + " không được để trống.");
        }
        try {
            final double amount = Double.parseDouble(value.trim());
            if (amount <= 0) {
                throw new IllegalArgumentException(
                        "Trường " + fieldName + " phải lớn hơn 0.");
            }
            return amount;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Trường " + fieldName + " phải là số hợp lệ.");
        }
    }

    private LocalDateTime parseDateTime(final String value, final String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Trường " + fieldName + " không được để trống.");
        }
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Trường " + fieldName
                            + " phải có dạng dd/MM/yyyy HH:mm, ví dụ 22/05/2026 14:30.");
        }
    }

    private void showError(final String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void clearError() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    /**
     * Parse chuỗi thời gian từ server (ISO-8601) sang định dạng input trên form.
     *
     * @param rawDateTime chuỗi thời gian thô từ server
     * @return thời gian đã format theo dd/MM/yyyy HH:mm để đổ vào input
     */
    private String formatDateTimeForInput(final String rawDateTime) {
        if (rawDateTime == null || rawDateTime.trim().isEmpty()) {
            return "";
        }
        try {
            return LocalDateTime.parse(rawDateTime.trim()).format(DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            // Giữ nguyên giá trị gốc nếu không parse được để user tự xử lý.
            return rawDateTime;
        }
    }

    private void setLoadingState(final boolean loading) {
        btnConfirm.setDisable(loading);
        btnCancel.setDisable(loading);
    }
}
