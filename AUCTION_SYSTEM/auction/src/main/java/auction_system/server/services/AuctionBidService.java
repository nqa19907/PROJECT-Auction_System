package auction_system.server.services;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.server.core.AuctionManager;
import auction_system.server.persistence.exceptions.DatabaseException;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Service xử lý nghiệp vụ đặt giá trong phiên đấu giá.
 *
 * <p>Lớp này là nơi duy nhất xử lý thao tác đặt giá thật sự trên server.
 * Command chỉ đọc request và trả response, còn service chịu trách nhiệm kiểm
 * tra quyền, kiểm tra dữ liệu, cập nhật auction, lưu lịch sử bid và đảm bảo
 * thao tác ghi database được thực hiện trong transaction.
 */
public class AuctionBidService {

    private static final Logger LOGGER =
            Logger.getLogger(AuctionBidService.class.getName());

    /** Database serialization dùng chung phía server. */
    private final SerializedDatabase database;
    private final BidRequestValidator bidRequestValidator;
    private final BidWalletService bidWalletService;
    private final BidRealtimeNotifier bidRealtimeNotifier;

    /**
     * Kết quả nội bộ sau khi server ghi nhận một lượt đặt giá.
     *
     * <p>Ngoài bid và auction, service giữ lại các user bị thay đổi số dư để
     * phát realtime cập nhật ví sau khi transaction đã lưu thành công.
     *
     * @param bid giao dịch đặt giá đã lưu
     * @param auction phiên đấu giá đã được cập nhật
     * @param bidder người vừa đặt giá mới
     * @param previousBidder người dẫn đầu cũ được hoàn tiền, có thể null
     */
    private record SavedBidResult(
            BidTransaction bid,
            Auction auction,
            Participant bidder,
            Participant previousBidder) {

    }

    /**
     * Khởi tạo service đặt giá.
     *
     * @param database database serialization của server
     * @param auctionManager manager runtime dùng để gửi realtime theo user
     */
    public AuctionBidService(
            final SerializedDatabase database,
            final AuctionManager auctionManager) {

        this.database = Objects.requireNonNull(database, "database");
        this.bidRequestValidator = new BidRequestValidator();
        this.bidWalletService = new BidWalletService(database);
        this.bidRealtimeNotifier = new BidRealtimeNotifier(auctionManager);
    }

    /**
     * Đặt giá cho một phiên đấu giá.
     *
     * <p>Phương thức này dùng transaction ở mức database để tránh trường hợp
     * nhiều client cùng đặt giá và ghi chéo dữ liệu. Trước khi nhận bid, service
     * cập nhật trạng thái phiên theo thời gian thật để phiên hết hạn không còn
     * nhận giá mới.
     *
     * @param auctionId mã phiên đấu giá
     * @param currentUser người dùng hiện tại trong session
     * @param amount số tiền đặt giá
     * @return giao dịch đặt giá đã được ghi nhận
     */
    public BidTransaction placeBid(
            final String auctionId,
            final User currentUser,
            final double amount) {

        bidRequestValidator.validatePlaceBidRequest(auctionId, currentUser, amount);

        SavedBidResult savedBidResult = database.executeInTransaction(() -> {
            Auction auction = findAuctionOrThrow(auctionId);
            refreshAuctionLifecycle(auction);

            Participant bidder = (Participant) currentUser;
            BidTransaction previousHighestBid = auction.getCurrentHighestBid();

            bidRequestValidator.validateAvailableBalance(bidder, previousHighestBid, amount);

            BidTransaction bidTransaction = new BidTransaction(bidder, amount, auction);
            auction.placeBid(bidTransaction);

            final Participant previousBidder = bidWalletService.applyBidHold(
                    bidder,
                    previousHighestBid,
                    amount);

            database.bidTransactions().save(bidTransaction);
            database.auctions().save(auction);
            database.flushAll();

            return new SavedBidResult(
                    bidTransaction,
                    auction,
                    bidder,
                    previousBidder);

        });

        // Sau khi bid transaction và auction đã được lưu, mới broadcast realtime.
        bidRealtimeNotifier.notifyBidSaved(
                savedBidResult.auction(),
                savedBidResult.bidder(),
                savedBidResult.previousBidder());

        BidTransaction savedBid = savedBidResult.bid();

        LOGGER.info(
                "Người dùng "
                        + currentUser.getUsername()
                        + " đặt giá "
                        + savedBid.getAmount()
                        + " cho phiên "
                        + auctionId);

        return savedBid;
    }

    /**
     * Tìm phiên đấu giá trong database.
     *
     * @param auctionId mã phiên đấu giá
     * @return phiên đấu giá tương ứng
     */
    private Auction findAuctionOrThrow(final String auctionId) {
        return database.auctions()
                .findById(auctionId)
                .orElseThrow(() -> new DatabaseException(
                        "Không tìm thấy phiên đấu giá: " + auctionId));
    }

    /**
     * Cập nhật trạng thái phiên theo thời gian thật trước khi xử lý đặt giá.
     *
     * @param auction phiên đấu giá cần kiểm tra
     */
    private void refreshAuctionLifecycle(final Auction auction) {
        final AuctionStatus oldStatus = auction.getStatus();
        auction.startAuction();
        auction.endAuction();

        if (oldStatus != auction.getStatus()) {
            database.auctions().save(auction);
            database.flushAll();
        }
    }

    /**
     * Lấy lịch sử đặt giá của một phiên theo thứ tự thời gian tăng dần.
     *
     * <p>Command phía network dùng dữ liệu này để dựng response BID_HISTORY;
     * service giữ quyền truy cập repository để command không phụ thuộc trực
     * tiếp vào tầng persistence.
     *
     * @param auctionId mã phiên đấu giá cần lấy lịch sử
     * @return danh sách giao dịch đặt giá của phiên
     */
    public List<BidTransaction> getBidHistory(final String auctionId) {
        bidRequestValidator.validateAuctionId(auctionId);

        return database.bidTransactions()
                .findByAuctionId(auctionId)
                .stream()
                .sorted(Comparator.comparing(BidTransaction::getTimestamp))
                .toList();
    }
}
