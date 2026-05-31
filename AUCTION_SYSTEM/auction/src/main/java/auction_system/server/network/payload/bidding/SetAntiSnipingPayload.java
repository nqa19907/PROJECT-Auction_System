package auction_system.server.network.payload.bidding;

/**
 * Payload JSON cho request cập nhật chống đặt giá phút chót.
 */
public record SetAntiSnipingPayload(String auctionId, Boolean enabled) {

    /**
     * Kiểm tra payload thiếu các field bắt buộc.
     *
     * @return true nếu thiếu field bắt buộc
     */
    public boolean hasMissingRequiredFields() {
        return auctionId == null || auctionId.isBlank() || enabled == null;
    }
}
