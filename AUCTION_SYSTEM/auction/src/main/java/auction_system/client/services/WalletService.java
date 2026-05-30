package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service phía client xử lý các thao tác ví.
 */
public final class WalletService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalletService.class);
    private static final WalletService INSTANCE = new WalletService();

    private WalletCallback currentDepositCallback;

    private WalletService() {
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.DEPOSIT_OK.name(), this::handleDepositSuccess);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.DEPOSIT_FAIL.name(), this::handleDepositFailure);
    }

    /**
     * Lấy instance duy nhất của WalletService.
     *
     * @return instance duy nhất
     */
    public static WalletService getInstance() {
        return INSTANCE;
    }

    /**
     * Callback trả kết quả thao tác ví.
     */
    @FunctionalInterface
    public interface WalletCallback {
        void onResult(boolean isSuccess, String message, double balance);
    }

    /**
     * Gửi yêu cầu nạp tiền lên server.
     *
     * @param amount số tiền cần nạp
     * @param callback callback nhận kết quả
     */
    public void deposit(final double amount, final WalletCallback callback) {
        this.currentDepositCallback = callback;

        String request = buildDepositRequest(amount);

        boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent && currentDepositCallback != null) {
            currentDepositCallback.onResult(false, "Mất kết nối tới máy chủ.", 0);
            currentDepositCallback = null;
        }
    }

    /**
     * Tạo request JSON cho lệnh nạp tiền.
     *
     * @param amount số tiền cần nạp
     * @return request JSON
     */
    private String buildDepositRequest(final double amount) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            null,
                            Protocol.Command.DEPOSIT.name(),
                            null,
                            JsonProtocol.payloadOf(amount),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON request nạp tiền: {}", exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON DEPOSIT.", exception);
        }
    }

    /**
     * Xử lý phản hồi nạp tiền thành công.
     *
     * @param response phản hồi JSON từ server
     */
    private void handleDepositSuccess(final String response) {
        if (currentDepositCallback == null) {
            return;
        }

        LOGGER.info("Nạp tiền thành công: {}", response);
        double balance = parseBalance(response);
        UserSessionService.getInstance().updateCurrentUserBalance(balance);

        currentDepositCallback.onResult(true, "Nạp tiền thành công.", balance);
        currentDepositCallback = null;
    }

    /**
     * Xử lý phản hồi nạp tiền thất bại.
     *
     * @param response phản hồi JSON từ server
     */
    private void handleDepositFailure(final String response) {
        if (currentDepositCallback == null) {
            return;
        }

        LOGGER.warn("Nạp tiền thất bại: {}", response);
        String message = parseFailureMessage(response);

        currentDepositCallback.onResult(false, message, 0);
        currentDepositCallback = null;
    }

    /**
     * Đọc số dư mới từ phản hồi nạp tiền.
     *
     * @param response response từ server
     * @return số dư mới, hoặc 0 nếu response không hợp lệ
     */
    private double parseBalance(final String response) {
        try {
            final JsonMessage message = JsonProtocol.parse(response);
            return message.payload().path("balance").asDouble(0);
        } catch (IOException exception) {
            LOGGER.warn("Không thể đọc JSON phản hồi nạp tiền: {}", exception.getMessage());
            return 0;
        }
    }

    /**
     * Đọc thông báo lỗi từ phản hồi nạp tiền.
     *
     * @param response response từ server
     * @return thông báo lỗi
     */
    private String parseFailureMessage(final String response) {
        try {
            final JsonMessage message = JsonProtocol.parse(response);
            if (message.message() == null || message.message().isBlank()) {
                return "Nạp tiền thất bại.";
            }
            return message.message();
        } catch (IOException exception) {
            LOGGER.warn("Không thể đọc JSON lỗi nạp tiền: {}", exception.getMessage());
            return "Nạp tiền thất bại.";
        }
    }
}
