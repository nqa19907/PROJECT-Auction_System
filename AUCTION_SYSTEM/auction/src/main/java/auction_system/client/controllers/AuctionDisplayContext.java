package auction_system.client.controllers;

/**
 * Dữ liệu hiển thị cần truyền từ ItemList sang BidHistory.
 *
 * @param auctionId mã phiên đấu giá
 * @param itemTitle tên vật phẩm
 * @param openingPrice giá khởi điểm
 * @param currentPrice giá hiện tại
 */
public record AuctionDisplayContext(
        String auctionId,
        String itemTitle,
        long openingPrice,
        long currentPrice
) {
}
