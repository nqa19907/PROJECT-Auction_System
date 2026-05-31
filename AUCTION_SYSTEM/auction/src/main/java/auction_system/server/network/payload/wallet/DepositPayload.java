package auction_system.server.network.payload.wallet;

/**
 * Payload JSON cho request nạp tiền.
 */
public record DepositPayload(String amount) {

    /**
     * Kiểm tra payload thiếu số tiền cần nạp.
     *
     * @return true nếu thiếu số tiền
     */
    public boolean hasMissingAmount() {
        return amount == null;
    }
}
