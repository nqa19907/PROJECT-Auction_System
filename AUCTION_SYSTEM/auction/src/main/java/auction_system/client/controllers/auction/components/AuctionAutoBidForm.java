package auction_system.client.controllers.auction.components;

import auction_system.client.models.AuctionViewModel;
import auction_system.client.services.AuctionService;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Điều phối trạng thái form đấu giá tự động trên màn hình chi tiết.
 *
 * <p>Lớp này xử lý hành vi UI của auto-bid: bật/tắt form theo checkbox, tải
 * trạng thái đã lưu từ server, gửi enable/disable và hiển thị lỗi nhập liệu.
 */
public final class AuctionAutoBidForm {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuctionAutoBidForm.class);
    private static final String ENABLE_BUTTON_TEXT = "Xác nhận đấu giá tự động";
    private static final String EDIT_BUTTON_TEXT = "Xác nhận chỉnh sửa";
    private static final String SUBMITTING_TEXT = "Đang gửi...";

    private final CheckBox autoBidToggle;
    private final TextField maxAmountInput;
    private final TextField stepAmountInput;
    private final Button enableAutoBidButton;
    private final Label errorLabel;
    private final AuctionViewModel viewModel;
    private boolean confirmedEnabled;
    private boolean loadingStatus;

    /**
     * Khởi tạo form đấu giá tự động bằng các control đã inject từ FXML.
     *
     * @param autoBidToggle checkbox bật/tắt chế độ auto-bid
     * @param maxAmountInput ô nhập giá tối đa người dùng sẵn sàng trả
     * @param stepAmountInput ô nhập bước tăng mỗi lần bị vượt giá
     * @param enableAutoBidButton nút xác nhận cấu hình auto-bid
     * @param errorLabel label hiển thị lỗi validate của form auto-bid
     * @param viewModel ViewModel của màn chi tiết phiên đấu giá
     */
    public AuctionAutoBidForm(
            final CheckBox autoBidToggle,
            final TextField maxAmountInput,
            final TextField stepAmountInput,
            final Button enableAutoBidButton,
            final Label errorLabel,
            final AuctionViewModel viewModel) {

        this.autoBidToggle = autoBidToggle;
        this.maxAmountInput = maxAmountInput;
        this.stepAmountInput = stepAmountInput;
        this.enableAutoBidButton = enableAutoBidButton;
        this.errorLabel = errorLabel;
        this.viewModel = viewModel;
    }

    /**
     * Đăng ký các handler UI cơ bản cho form auto-bid.
     *
     * <p>Ở bước này form chỉ bật/tắt các control theo checkbox. Khi người dùng
     * bỏ chọn auto-bid, dữ liệu nhập và lỗi cũ được xoá để form quay về trạng
     * thái ban đầu.
     */
    public void registerHandlers() {
        autoBidToggle.selectedProperty().addListener(
                (obs, oldValue, selected) -> handleToggleChanged(selected)
        );

        maxAmountInput.textProperty().addListener(
                (obs, oldValue, newValue) -> hideError()
        );
        stepAmountInput.textProperty().addListener(
                (obs, oldValue, newValue) -> hideError()
        );

        // Khi bấm xác nhận, kiểm tra dữ liệu trước rồi gửi cấu hình lên server.
        enableAutoBidButton.setOnAction(event -> submitAutoBidSettings());
        maxAmountInput.setOnAction(event -> {
            submitAutoBidSettings();
            event.consume();
        });
        stepAmountInput.setOnAction(event -> {
            submitAutoBidSettings();
            event.consume();
        });
        applyEnabledState(autoBidToggle.isSelected());
    }

    /**
     * Tải trạng thái auto-bid đã lưu của user hiện tại khi mở màn hình chi tiết.
     *
     * @param auctionId mã phiên đấu giá đang hiển thị
     */
    public void loadStatus(final String auctionId) {
        loadingStatus = true;
        AuctionService.getInstance().fetchAutoBidStatus(
                auctionId,
                (enabled, maxAmount, stepAmount) -> Platform.runLater(() ->
                        applyLoadedStatus(enabled, maxAmount, stepAmount)));
    }

    /**
     * Kiểm tra dữ liệu auto-bid trước khi gửi cấu hình lên server.
     *
     * <p>Validate ở client để phản hồi nhanh; server vẫn kiểm tra lại trước khi
     * lưu cấu hình auto-bid.
     */
    private void submitAutoBidSettings() {
        final long maxAmount = parseAmountOrZero(maxAmountInput.getText());
        final long stepAmount = parseAmountOrZero(stepAmountInput.getText());

        if (maxAmount <= 0) {
            showError("Giá tối đa phải là số dương.");
            return;
        }

        if (stepAmount <= 0) {
            showError("Bước tăng phải là số dương.");
            return;
        }

        if (maxAmount <= viewModel.getCurrentPriceValue()) {
            showError("Giá tối đa phải lớn hơn giá hiện tại.");
            return;
        }

        hideError();

        setSubmitting(true);

        AuctionService.getInstance().enableAutoBid(
                viewModel.auctionIdProperty().get(),
                maxAmount,
                stepAmount,
                (success, message) -> Platform.runLater(() ->
                        handleSubmitResult(success, message))
        );

        LOGGER.info(
                "Auto-bid hợp lệ. auctionId={}, maxAmount={}, stepAmount={}",
                viewModel.auctionIdProperty().get(),
                maxAmount,
                stepAmount
        );
    }

    /**
     * Xử lý kết quả bật auto-bid trả về từ service.
     *
     * @param success true nếu server chấp nhận cấu hình auto-bid
     * @param message thông báo từ service/server
     */
    private void handleSubmitResult(final boolean success, final String message) {
        setSubmitting(false);

        if (!success) {
            showError(message);
            return;
        }

        confirmedEnabled = true;
        updateButtonText(false);
        hideError();
        LOGGER.info(message);
    }

    /**
     * Khoá nút xác nhận trong lúc chờ phản hồi từ server.
     *
     * @param submitting true nếu request auto-bid đang được gửi
     */
    private void setSubmitting(final boolean submitting) {
        enableAutoBidButton.setDisable(submitting);
        updateButtonText(submitting);
    }

    /**
     * Parse chuỗi tiền tệ trong input thành số nguyên.
     *
     * @param rawValue chuỗi người dùng nhập
     * @return số tiền đã parse, hoặc 0 nếu chuỗi rỗng/không hợp lệ
     */
    private long parseAmountOrZero(final String rawValue) {
        if (rawValue == null) {
            return 0L;
        }

        final String sanitized = rawValue.replaceAll("[^0-9]", "");
        if (sanitized.isEmpty()) {
            return 0L;
        }

        try {
            return Long.parseLong(sanitized);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Bật hoặc khoá các control cấu hình auto-bid.
     *
     * @param enabled true nếu checkbox auto-bid đang được chọn
     */
    private void handleToggleChanged(final boolean enabled) {
        if (loadingStatus) {
            applyEnabledState(enabled);
            return;
        }

        applyEnabledState(enabled);

        if (!enabled) {
            if (confirmedEnabled) {
                disableConfirmedAutoBid();
            } else {
                clearForm();
            }
        }
    }

    private void applyEnabledState(final boolean enabled) {
        /*
        * JavaFX dùng setDisable(true) để khoá control, nhưng tham số của method này
        * lại là enabled. Vì vậy phải đảo ngược giá trị: enabled=true thì disable=false,
        * enabled=false thì disable=true.
        */
        maxAmountInput.setDisable(!enabled);
        stepAmountInput.setDisable(!enabled);
        enableAutoBidButton.setDisable(!enabled);
        updateButtonText(false);
    }

    private void applyLoadedStatus(
            final boolean enabled,
            final long maxAmount,
            final long stepAmount) {

        confirmedEnabled = enabled;
        loadingStatus = true;
        autoBidToggle.setSelected(enabled);

        if (enabled) {
            maxAmountInput.setText(Long.toString(maxAmount));
            stepAmountInput.setText(Long.toString(stepAmount));
            hideError();
        } else {
            clearForm();
        }

        applyEnabledState(enabled);
        loadingStatus = false;
    }

    private void disableConfirmedAutoBid() {
        applyEnabledState(false);
        AuctionService.getInstance().disableAutoBid(
                viewModel.auctionIdProperty().get(),
                (success, message) -> Platform.runLater(() ->
                        handleDisableResult(success, message)));
    }

    private void handleDisableResult(final boolean success, final String message) {
        if (!success) {
            confirmedEnabled = true;
            loadingStatus = true;
            autoBidToggle.setSelected(true);
            applyEnabledState(true);
            loadingStatus = false;
            showError(message);
            return;
        }

        confirmedEnabled = false;
        clearForm();
        applyEnabledState(false);
        LOGGER.info(message);
    }

    private void updateButtonText(final boolean submitting) {
        if (submitting) {
            enableAutoBidButton.setText(SUBMITTING_TEXT);
            return;
        }

        enableAutoBidButton.setText(confirmedEnabled ? EDIT_BUTTON_TEXT : ENABLE_BUTTON_TEXT);
    }

    /**
     * Xoá dữ liệu nhập và ẩn lỗi của form auto-bid.
     */
    private void clearForm() {
        maxAmountInput.clear();
        stepAmountInput.clear();
        hideError();
    }

    /**
     * Ẩn lỗi validate của form auto-bid.
     */
    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /**
     * Hiển thị lỗi validate của form auto-bid.
     *
     * @param message nội dung lỗi
     */
    private void showError(final String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
