package auction_system.server.services;

import auction_system.common.exceptions.InvalidBidException;
import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.auctions.AutoBidSetting;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.server.core.AuctionManager;
import auction_system.server.persistence.exceptions.DatabaseException;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.util.ArrayList;
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
    private final AutoBidService autoBidService;
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
     * Kết quả nội bộ của toàn bộ transaction xử lý một request đặt giá.
     *
     * <p>Request đặt giá thủ công có thể kích hoạt thêm một bid tự động. Record
     * này giữ bid thủ công để trả cho caller và toàn bộ các bid đã lưu để phát
     * realtime sau khi transaction commit thành công.
     *
     * @param manualBid bid thủ công do client gửi lên
     * @param savedBidResults các lượt bid đã lưu trong transaction
     */
    private record BidTransactionResult(
            BidTransaction manualBid,
            List<SavedBidResult> savedBidResults) {

    }

    /**
     * Khởi tạo service đặt giá.
     *
     * @param database database serialization của server
     * @param auctionManager manager runtime dùng để gửi realtime theo user
     * @param autoBidService service quản lý cấu hình auto-bid
     */
    public AuctionBidService(
            final SerializedDatabase database,
            final AuctionManager auctionManager,
            final AutoBidService autoBidService) {

        this.database = Objects.requireNonNull(database, "database");
        this.bidRequestValidator = new BidRequestValidator();
        this.autoBidService = Objects.requireNonNull(autoBidService, "autoBidService");
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

        /*
         * Toàn bộ phần đọc auction, kiểm tra số dư, ghi bid, trừ/hoàn tiền và
         * flush database nằm trong một transaction để tránh ghi nửa chừng.
         */
        BidTransactionResult transactionResult = database.executeInTransaction(() -> {
            Auction auction = findAuctionOrThrow(auctionId);
            refreshAuctionLifecycle(auction);

            // Gom các bid đã lưu để notify sau khi transaction commit thành công.
            final List<SavedBidResult> savedBidResults = new ArrayList<>();

            // Ghi bid thủ công bằng helper dùng chung cho cả manual bid và auto-bid.
            final Participant bidder = (Participant) currentUser;
            final SavedBidResult result = saveBidInTransaction(auction, bidder, amount);
            savedBidResults.add(result);

            // Lấy candidate đã sort, rồi thử ghi tối đa một bid tự động hợp lệ.
            final SavedBidResult autoBidResult = trySaveOneAutoBidInTransaction(
                    auction,
                    autoBidService.findEligibleSettings(auction));
            if (autoBidResult != null) {
                savedBidResults.add(autoBidResult);
            }

            // Commit toàn bộ bid thủ công và bid tự động trong cùng transaction.
            database.flushAll();

            // Trả bid thủ công để giữ nguyên hợp đồng public của service.
            return new BidTransactionResult(result.bid(), savedBidResults);
        });

        // Sau khi transaction đã lưu thành công, mới broadcast từng bid đã ghi.
        for (SavedBidResult savedBidResult : transactionResult.savedBidResults()) {
            bidRealtimeNotifier.notifyBidSaved(
                    savedBidResult.auction(),
                    savedBidResult.bidder(),
                    savedBidResult.previousBidder());
        }

        BidTransaction savedBid = transactionResult.manualBid();

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
     * Ghi một lượt bid vào auction và cập nhật ví các user liên quan.
     *
     * <p>Helper này giả định caller đang ở trong database transaction. Cả bid
     * thủ công và auto-bid sau này sẽ dùng chung method này để tránh lệch logic
     * giữ tiền, hoàn tiền, lưu lịch sử và lưu auction.
     *
     * @param auction phiên đấu giá cần ghi bid
     * @param bidder người đặt giá
     * @param amount số tiền đặt giá
     * @return kết quả bid vừa lưu
     */
    private SavedBidResult saveBidInTransaction(
            final Auction auction,
            final Participant bidder,
            final double amount) {

        // Lấy bid dẫn đầu cũ để kiểm tra số dư và hoàn tiền nếu bị vượt.
        final BidTransaction previousHighestBid = auction.getCurrentHighestBid();

        // Kiểm tra bidder có đủ số dư khả dụng cho mức giá mới không.
        bidRequestValidator.validateAvailableBalance(
                bidder,
                previousHighestBid,
                amount);

        // Tạo bid mới và cập nhật giá cao nhất trong auction.
        final BidTransaction bidTransaction = new BidTransaction(bidder, amount, auction);
        auction.placeBid(bidTransaction);

        // Hoàn tiền người dẫn đầu cũ và giữ tiền người dẫn đầu mới.
        final Participant previousBidder = bidWalletService.applyBidHold(
                bidder,
                previousHighestBid,
                amount);

        // Lưu lịch sử bid và trạng thái auction, caller sẽ quyết định thời điểm flush.
        database.bidTransactions().save(bidTransaction);
        database.auctions().save(auction);

        return new SavedBidResult(
                bidTransaction,
                auction,
                bidder,
                previousBidder);
    }

    /**
     * Thử ghi tối đa một bid tự động từ danh sách candidate đã được sắp xếp.
     *
     * <p>Candidate có độ ưu tiên cao nhất vẫn được thử trước. Nếu candidate đó
     * không thể đặt giá vì thiếu số dư hoặc mức giá không còn hợp lệ, service bỏ
     * qua candidate đó và thử người kế tiếp. Nhờ vậy một cấu hình auto-bid lỗi
     * không chặn các candidate hợp lệ phía sau.
     *
     * @param auction phiên đấu giá đang xử lý trong transaction hiện tại
     * @param settings danh sách cấu hình auto-bid đã sắp xếp theo ưu tiên
     * @return bid tự động đầu tiên ghi thành công, hoặc null nếu không ai đặt được
     */
    private SavedBidResult trySaveOneAutoBidInTransaction(
            final Auction auction,
            final List<AutoBidSetting> settings) {

        // Không có candidate thì không phát sinh bid tự động.
        if (settings.isEmpty()) {
            return null;
        }

        // Duyệt từng candidate theo priority và dừng ngay khi ghi thành công một bid.
        for (int index = 0; index < settings.size(); index++) {
            final AutoBidSetting setting = settings.get(index);
            final AutoBidSetting secondSetting =
                    index + 1 < settings.size() ? settings.get(index + 1) : null;

            // Đọc lại bid cao nhất hiện tại để tính mức auto-bid dựa trên trạng thái mới nhất.
            final BidTransaction currentHighestBid = auction.getCurrentHighestBid();

            // Tính số tiền candidate hiện tại cần đặt để tạm thời dẫn đầu.
            final double nextAmount = autoBidService.calculateAutoBidAmount(
                    currentHighestBid,
                    setting,
                    secondSetting);

            try {
                // Ghi bid qua helper chung để dùng cùng validate, giữ tiền, hoàn tiền và lưu DB.
                return saveBidInTransaction(
                        auction,
                        setting.getParticipant(),
                        nextAmount);
            } catch (InvalidBidException exception) {
                // Candidate không đủ số dư cho nextAmount thì bỏ qua và thử candidate kế tiếp.
                LOGGER.fine(
                        "Bỏ qua auto-bid của participant "
                                + setting.getParticipant().getId()
                                + ": "
                                + exception.getMessage());
            }
        }

        // Không candidate nào ghi bid thành công.
        return null;
    }

    /**
     * Tìm phiên đấu giá trong database.
     *
     * @param auctionId mã phiên đấu giá
     * @return phiên đấu giá tương ứng
     */
    private Auction findAuctionOrThrow(final String auctionId) {
        /*
         * Đọc trực tiếp từ database để transaction làm việc với dữ liệu bền vững
         * mới nhất, thay vì chỉ dựa vào registry runtime.
         */
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

        /*
         * Nếu phiên vừa chuyển RUNNING/FINISHED do thời gian thực, lưu ngay trước
         * khi tiếp tục xử lý bid để các request sau thấy trạng thái đúng.
         */
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
