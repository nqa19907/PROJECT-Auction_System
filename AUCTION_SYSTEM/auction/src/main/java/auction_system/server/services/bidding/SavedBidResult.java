package auction_system.server.services.bidding;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.users.Participant;

/**
 * Kết quả nội bộ sau khi server ghi nhận một lượt đặt giá.
 *
 * @param bid giao dịch đặt giá đã lưu
 * @param auction phiên đấu giá đã được cập nhật
 * @param bidder người vừa đặt giá mới
 * @param previousBidder người dẫn đầu cũ được hoàn tiền, có thể null
 */
record SavedBidResult(
        BidTransaction bid,
        Auction auction,
        Participant bidder,
        Participant previousBidder) {

}
