package auction_system.server.network.payload;

/**
 * Payload JSON cho request chỉ cần mã phiên đấu giá.
 */
public record AuctionIdPayload(String auctionId) {

    /**
     * Kiểm tra payload thiếu mã phiên đấu giá.
     *
     * @return true nếu thiếu mã phiên đấu giá
     */
    public boolean hasMissingAuctionId() {
        return auctionId == null || auctionId.isBlank();
    }
}
