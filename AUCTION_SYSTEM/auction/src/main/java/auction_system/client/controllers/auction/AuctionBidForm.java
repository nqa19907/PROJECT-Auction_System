package auction_system.client.controllers.auction;

import auction_system.client.models.AuctionViewModel;
import auction_system.client.utils.CurrencyFormatter;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Điều phối trạng thái form đặt giá trên màn hình chi tiết.
 */
final class AuctionBidForm {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionBidForm.class);
    private static final String SUBMIT_TEXT = "Đặt giá ngay  →";
    private static final String SUBMITTING_TEXT = "Đang gửi...";

    private final TextField bidInput;
    private final Button placeBidButton;
    private final Label errorLabel;
    private final AuctionViewModel viewModel;

    AuctionBidForm(
            final TextField bidInput,
            final Button placeBidButton,
            final Label errorLabel,
            final AuctionViewModel viewModel) {
        this.bidInput = bidInput;
        this.placeBidButton = placeBidButton;
        this.errorLabel = errorLabel;
        this.viewModel = viewModel;
    }

    void registerInputListener() {
        bidInput.textProperty().addListener((obs, oldValue, newValue) -> hideError());
    }

    void submit() {
        // TODO: Chặn submit ở client nếu phiên không còn RUNNING,
        // trước khi gửi PLACE_BID lên server.
        hideError();

        final String rawAmount = bidInput.getText();
        if (rawAmount == null || rawAmount.trim().isEmpty()) {
            showError("Vui lòng nhập số tiền.");
            return;
        }

        setSubmitting(true);
        viewModel.submitBid(rawAmount, (success, message, newBalance) ->
                Platform.runLater(() -> handleSubmitResult(
                        rawAmount,
                        success,
                        message,
                        newBalance)));
    }

    void addToInput(final long delta) {
        final String raw = bidInput.getText().replaceAll("[^0-9]", "");
        final long base = raw.isEmpty() ? viewModel.getCurrentPriceValue() : Long.parseLong(raw);

        bidInput.setText(String.valueOf(base + delta));
    }

    private void handleSubmitResult(
            final String rawAmount,
            final boolean success,
            final String message,
            final double newBalance) {
        setSubmitting(false);
        keepFocusInBidInput();

        if (success) {
            handleSubmitSuccess(rawAmount, newBalance);
        } else {
            showError(message);
        }
    }

    private void handleSubmitSuccess(final String rawAmount, final double newBalance) {
        final long amount = Long.parseLong(rawAmount.replaceAll("[^0-9]", ""));

        // UI sẽ cập nhật qua UPDATE_PRICE rồi reload bid history để đồng bộ với server.
        bidInput.clear();
        hideError();

        LOGGER.info("Đặt giá thành công với số tiền: {}",
                CurrencyFormatter.formatAmount(amount));
        LOGGER.info("Số dư mới sau khi đặt giá: {}",
                CurrencyFormatter.formatAmount(newBalance));
    }

    private void setSubmitting(final boolean submitting) {
        placeBidButton.setDisable(submitting);
        placeBidButton.setText(submitting ? SUBMITTING_TEXT : SUBMIT_TEXT);
    }

    private void keepFocusInBidInput() {
        bidInput.requestFocus();
    }

    private void showError(final String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        if (!errorLabel.isVisible()) {
            return;
        }

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
