package auction_system.server.network.payload.bidding;

/**
 * Payload JSON cho request bật hoặc cập nhật auto-bid.
 */
public record AutoBidPayload(String auctionId, String maxAmount, String stepAmount) {

    /**
     * Kiểm tra payload thiếu các field bắt buộc.
     *
     * @return true nếu thiếu field bắt buộc
     */
    public boolean hasMissingRequiredFields() {
        return isBlank(auctionId) || maxAmount == null || stepAmount == null;
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
