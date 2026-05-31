package auction_system.server.network.payload.bidding;

/**
 * Payload JSON cho request đặt giá.
 */
public record PlaceBidPayload(String auctionId, String amount) {

    /**
     * Kiểm tra payload thiếu các field bắt buộc.
     *
     * @return true nếu thiếu field bắt buộc
     */
    public boolean hasMissingRequiredFields() {
        return auctionId == null || auctionId.isBlank() || amount == null;
    }
}
