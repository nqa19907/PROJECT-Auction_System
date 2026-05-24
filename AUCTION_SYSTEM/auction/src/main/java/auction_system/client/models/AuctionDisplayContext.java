package auction_system.client.models;

import java.time.LocalDateTime;

/**
 * Dữ liệu hiển thị cần truyền từ ItemList sang BidHistory.
 *
 * @param auctionId mã phiên đấu giá
 * @param itemTitle tên vật phẩm
 * @param openingPrice giá khởi điểm
 * @param currentPrice giá hiện tại
 */
// TODO: Thêm auctionStatus vào context để AuctionDetail biết phiên có được đặt giá không.
public record AuctionDisplayContext(
        String auctionId,
        String itemTitle,
        long openingPrice,
        long currentPrice,
        String status,
        LocalDateTime endTime
) {
}
