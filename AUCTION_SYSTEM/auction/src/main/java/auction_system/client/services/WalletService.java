package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service phía client xử lý các thao tác ví.
 */
public final class WalletService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalletService.class);
    private static final WalletService INSTANCE = new WalletService();
    private static final int MIN_DEPOSIT_OK_PARTS = 2;
    private static final int IDX_DEPOSIT_BALANCE = 1;
    private static final int MIN_DEPOSIT_FAIL_PARTS = 2;
    private static final int IDX_DEPOSIT_FAIL_MESSAGE = 1;

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

        String request = Protocol.Command.DEPOSIT.name()
                + Protocol.SEPARATOR
                + amount;

        boolean sent = NetworkClient.getInstance().sendCommand(request);
        if (!sent && currentDepositCallback != null) {
            currentDepositCallback.onResult(false, "Mất kết nối tới máy chủ.", 0);
            currentDepositCallback = null;
        }
    }

    /**
     * Xử lý phản hồi nạp tiền thành công.
     *
     * @param response chuỗi phản hồi từ server theo format DEPOSIT_OK|balance
     */
    private void handleDepositSuccess(final String response) {
        if (currentDepositCallback == null) {
            return;
        }

        LOGGER.info("Nạp tiền thành công: {}", response);
        String[] parts = response.split(Protocol.SEPARATOR_REGEX);
        double balance = parseBalance(parts);
        UserSessionService.getInstance().updateCurrentUserBalance(balance);

        currentDepositCallback.onResult(true, "Nạp tiền thành công.", balance);
        currentDepositCallback = null;
    }

    /**
     * Xử lý phản hồi nạp tiền thất bại.
     *
     * @param response chuỗi phản hồi từ server theo format DEPOSIT_FAIL|message
     */
    private void handleDepositFailure(final String response) {
        if (currentDepositCallback == null) {
            return;
        }

        LOGGER.warn("Nạp tiền thất bại: {}", response);
        String[] parts = response.split(Protocol.SEPARATOR_REGEX);
        String message = parts.length >= MIN_DEPOSIT_FAIL_PARTS
                ? parts[IDX_DEPOSIT_FAIL_MESSAGE] : "Nạp tiền thất bại.";

        currentDepositCallback.onResult(false, message, 0);
        currentDepositCallback = null;
    }

    /**
     * Đọc số dư mới từ phản hồi nạp tiền.
     *
     * @param parts response đã tách theo ký tự phân cách protocol
     * @return số dư mới, hoặc 0 nếu response không hợp lệ
     */
    private double parseBalance(final String[] parts) {
        if (parts.length < MIN_DEPOSIT_OK_PARTS) {
            return 0;
        }

        try {
            return Double.parseDouble(parts[IDX_DEPOSIT_BALANCE]);
        } catch (NumberFormatException e) {
            LOGGER.warn(
                    "Không thể đọc số dư từ phản hồi nạp tiền: {}",
                    parts[IDX_DEPOSIT_BALANCE]);
            return 0;
        }
    }
}
