package auction_system.client.controllers.auction.components;

import auction_system.client.models.AuctionViewModel;
import auction_system.client.utils.CurrencyFormatter;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Điều phối form đặt giá thường trên màn hình chi tiết đấu giá.
 */
public final class AuctionBidForm {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionBidForm.class);
    private static final String SUBMITTING_TEXT = "Đang gửi...";
    private static final String PLACE_BID_TEXT = "Đặt giá ngay  →";

    private final TextField bidInput;
    private final Button placeBidButton;
    private final Label errorLabel;
    private final AuctionViewModel viewModel;

    /**
     * Khởi tạo form đặt giá thường bằng các control đã inject từ FXML.
     *
     * @param bidInput ô nhập giá đặt
     * @param placeBidButton nút gửi giá đặt
     * @param errorLabel label hiển thị lỗi nhập liệu/server
     * @param viewModel ViewModel màn chi tiết phiên đấu giá
     */
    public AuctionBidForm(
            final TextField bidInput,
            final Button placeBidButton,
            final Label errorLabel,
            final AuctionViewModel viewModel) {

        this.bidInput = bidInput;
        this.placeBidButton = placeBidButton;
        this.errorLabel = errorLabel;
        this.viewModel = viewModel;
    }

    /**
     * Đăng ký listener cơ bản cho input đặt giá.
     */
    public void registerHandlers() {
        // Người dùng đã sửa input nên ẩn lỗi cũ để không gây nhiễu.
        bidInput.textProperty().addListener((obs, oldValue, newValue) -> hideError());
    }

    /**
     * Gửi giá đặt hiện tại lên ViewModel/service và cập nhật trạng thái UI theo kết quả.
     */
    public void submitBid() {
        hideError();

        final String rawAmount = bidInput.getText();
        if (rawAmount == null || rawAmount.trim().isEmpty()) {
            // Chặn request rỗng ngay tại UI để không gửi dữ liệu vô nghĩa lên ViewModel/server.
            showError("Vui lòng nhập số tiền.");
            return;
        }

        // Tạm khóa nút để tránh người dùng bấm nhiều lần trong lúc chờ server phản hồi.
        placeBidButton.setDisable(true);
        placeBidButton.setText(SUBMITTING_TEXT);

        // ViewModel xử lý validate nghiệp vụ và gọi service; callback có thể chạy từ luồng mạng.
        viewModel.submitBid(rawAmount, (success, message, newBalance) ->
                Platform.runLater(() ->
                        handleSubmitResult(success, message, newBalance, rawAmount)));
    }

    /**
     * Cộng nhanh một mức tiền vào ô nhập giá.
     *
     * @param delta số tiền cộng thêm
     */
    public void adjustInput(final long delta) {
        // Nếu ô nhập trống, lấy giá hiện tại làm mốc rồi cộng nhanh theo nút người dùng chọn.
        final String raw = bidInput.getText().replaceAll("[^0-9]", "");
        final long base = raw.isEmpty() ? viewModel.getCurrentPriceValue() : Long.parseLong(raw);

        bidInput.setText(String.valueOf(base + delta));
    }

    private void handleSubmitResult(
            final boolean success,
            final String message,
            final double newBalance,
            final String rawAmount) {

        // Luôn mở lại nút và giữ focus trong form sau khi server trả kết quả.
        placeBidButton.setDisable(false);
        placeBidButton.setText(PLACE_BID_TEXT);
        bidInput.requestFocus();

        if (!success) {
            // Khi thất bại, hiển thị lỗi ngay dưới ô nhập thay vì mở Alert.
            showError(message);
            return;
        }

        // Khi thành công, UI giá/bảng/chart sẽ đồng bộ qua broadcast realtime UPDATE_PRICE.
        final long amount = Long.parseLong(rawAmount.replaceAll("[^0-9]", ""));
        bidInput.clear();
        hideError();

        LOGGER.info(
                "Đặt giá thành công với số tiền: {}",
                CurrencyFormatter.formatAmount(amount));
        LOGGER.info(
                "Số dư mới sau khi đặt giá: {}",
                CurrencyFormatter.formatAmount(newBalance));
    }

    private void hideError() {
        if (!errorLabel.isVisible()) {
            return;
        }

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showError(final String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
