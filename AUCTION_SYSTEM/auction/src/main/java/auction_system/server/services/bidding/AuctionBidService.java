package auction_system.server.services.bidding;

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
import auction_system.server.services.auction.AuctionLifecycleRefresher;
import auction_system.server.services.autobid.AutoBidService;
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
    private final BidRealtimeNotifier bidRealtimeNotifier;
    private final BidRecorder bidRecorder;
    private final AutoBidProcessor autoBidProcessor;
    private final AuctionLifecycleRefresher lifecycleRefresher;

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
        final BidWalletService bidWalletService = new BidWalletService(database);
        this.bidRecorder = new BidRecorder(
                database,
                bidRequestValidator,
                bidWalletService);
        this.autoBidProcessor = new AutoBidProcessor(autoBidService, bidRecorder);
        this.lifecycleRefresher = new AuctionLifecycleRefresher(database, auctionManager);
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
            lifecycleRefresher.refreshAuctionLifecycle(auction);

            // Gom các bid đã lưu để notify sau khi transaction commit thành công.
            final List<SavedBidResult> savedBidResults = new ArrayList<>();

            // Ghi bid thủ công bằng helper dùng chung cho cả manual bid và auto-bid.
            final Participant bidder = (Participant) currentUser;
            final SavedBidResult result =
                    bidRecorder.saveBidInTransaction(auction, bidder, amount);
            savedBidResults.add(result);

            // Resolve toàn bộ chuỗi auto-bid phản ứng sau bid thủ công.
            savedBidResults.addAll(
                    autoBidProcessor.resolveAutoBidChainInTransaction(auction, null));

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
     * Bật hoặc cập nhật auto-bid cho một phiên đấu giá sau khi kiểm tra quyền server-side.
     *
     * <p>Command phía client không được tự lưu setting auto-bid vì server phải đọc phiên mới nhất
     * trong database để chặn người bán tự bật auto-bid cho sản phẩm của chính mình.
     *
     * @param auctionId mã phiên đấu giá
     * @param currentUser người dùng đang đăng nhập
     * @param maxAmount giá tối đa user cho phép auto-bid
     * @param stepAmount bước tăng mỗi lần bị vượt giá
     */
    public void enableAutoBid(
            final String auctionId,
            final User currentUser,
            final long maxAmount,
            final long stepAmount) {

        bidRequestValidator.validateAuctionId(auctionId);

        if (!(currentUser instanceof Participant participant)) {
            throw new InvalidBidException("Chỉ người mua mới có thể bật auto-bid.");
        }

        database.executeInTransaction(() -> {
            final Auction auction = findAuctionOrThrow(auctionId);
            lifecycleRefresher.refreshAuctionLifecycle(auction);
            validateNotSellerAutoBidder(auction, participant);

            autoBidService.enableAutoBid(
                    auctionId,
                    participant,
                    maxAmount,
                    stepAmount);
            database.flushAll();
            return null;
        });

        triggerAutoBidAfterEnable(auctionId, participant);
    }

    /**
     * Kích hoạt auto-bid ngay sau khi một participant bật hoặc cập nhật setting.
     *
     * <p>Nếu phiên chưa có bid nào, service thử tạo bid mở đầu bằng giá khởi
     * điểm cho chính người vừa bật auto-bid. Nếu phiên đã có bid, service thử
     * chọn một candidate hợp lệ theo thứ tự ưu tiên hiện tại.
     *
     * @param auctionId mã phiên đấu giá
     * @param currentUser người vừa bật auto-bid
     */
    public void triggerAutoBidAfterEnable(
            final String auctionId,
            final User currentUser) {

        bidRequestValidator.validateAuctionId(auctionId);

        if (!(currentUser instanceof Participant participant)) {
            throw new InvalidBidException("Chỉ người mua mới có thể bật auto-bid.");
        }

        final List<SavedBidResult> savedBidResults = database.executeInTransaction(() -> {
            final Auction auction = findAuctionOrThrow(auctionId);
            lifecycleRefresher.refreshAuctionLifecycle(auction);

            // Không tạo auto-bid mới cho phiên đã hết trạng thái RUNNING.
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                return List.of();
            }

            // Lấy đúng setting của user vừa bật/cập nhật để không kích hoạt nhầm user khác.
            final AutoBidSetting enabledSetting =
                    autoBidService.findSetting(auctionId, participant.getId()).orElse(null);

            final SavedBidResult autoBidResult;
            if (auction.getCurrentHighestBid() == null) {
                autoBidResult = autoBidProcessor.trySaveOpeningAutoBidInTransaction(
                        auction,
                        enabledSetting,
                        participant);
            } else {
                autoBidResult = autoBidProcessor.trySaveSpecificAutoBidInTransaction(
                        auction,
                        enabledSetting,
                        participant);
            }

            if (autoBidResult == null) {
                return List.of();
            }

            final List<SavedBidResult> results = new ArrayList<>();
            results.add(autoBidResult);

            // Sau bid của user vừa enable, cho các auto-bidder khác phản ứng dây chuyền.
            results.addAll(
                    autoBidProcessor.resolveAutoBidChainInTransaction(auction, participant));

            database.flushAll();
            return results;
        });

        for (SavedBidResult savedBidResult : savedBidResults) {
            bidRealtimeNotifier.notifyBidSaved(
                    savedBidResult.auction(),
                    savedBidResult.bidder(),
                    savedBidResult.previousBidder());
        }
    }

    /**
     * Kích hoạt lại các auto-bid của user sau khi số dư ví thay đổi.
     *
     * <p>Luồng này dành cho trường hợp auto-bid đã active nhưng trước đó bị bỏ
     * qua vì thiếu số dư. Sau khi nạp tiền, service thử lại các setting active
     * của đúng user vừa nạp trên những phiên vẫn đang RUNNING.
     *
     * @param currentUser user vừa được cập nhật số dư
     */
    public void triggerAutoBidsAfterBalanceChange(final User currentUser) {
        // Chỉ participant mới có ví và setting auto-bid cần retry.
        if (!(currentUser instanceof Participant participant)) {
            return;
        }

        final List<SavedBidResult> savedBidResults = database.executeInTransaction(() -> {
            final List<SavedBidResult> results = new ArrayList<>();

            // Duyệt các setting active của chính user vừa nạp tiền theo thứ tự ổn định.
            for (AutoBidSetting setting
                    : autoBidService.findActiveSettingsForParticipant(participant.getId())) {
                final Auction auction = findAuctionOrThrow(setting.getAuctionId());

                // Cập nhật lifecycle để không retry auto-bid trên phiên đã hết hạn.
                lifecycleRefresher.refreshAuctionLifecycle(auction);
                if (auction.getStatus() != AuctionStatus.RUNNING) {
                    continue;
                }

                // Thử đúng setting của user vừa nạp trước, tránh kích hoạt nhầm user khác.
                final SavedBidResult autoBidResult;
                if (auction.getCurrentHighestBid() == null) {
                    autoBidResult = autoBidProcessor.trySaveOpeningAutoBidInTransaction(
                            auction,
                            setting,
                            participant);
                } else {
                    autoBidResult = autoBidProcessor.trySaveSpecificAutoBidInTransaction(
                            auction,
                            setting,
                            participant);
                }

                if (autoBidResult != null) {
                    results.add(autoBidResult);

                    // Sau bid mới do nạp tiền, cho các auto-bidder khác phản ứng dây chuyền.
                    results.addAll(
                            autoBidProcessor.resolveAutoBidChainInTransaction(
                                    auction,
                                    participant));
                }
            }

            // Chỉ flush khi thật sự có bid mới để tránh ghi file không cần thiết.
            if (!results.isEmpty()) {
                database.flushAll();
            }

            return results;
        });

        // Broadcast sau transaction để client thấy giá và ví cuối cùng đã commit.
        for (SavedBidResult savedBidResult : savedBidResults) {
            bidRealtimeNotifier.notifyBidSaved(
                    savedBidResult.auction(),
                    savedBidResult.bidder(),
                    savedBidResult.previousBidder());
        }
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
     * Lấy lịch sử đặt giá của một phiên theo thứ tự thời gian tăng dần.
     *
     * <p>Command phía network dùng dữ liệu này để dựng response BID_HISTORY;
     * service giữ quyền truy cập repository để command không phụ thuộc trực
     * tiếp vào tầng persistence.
     *
     * @param auction phiên đấu giá cần kiểm tra
     * @param participant người đang yêu cầu bật auto-bid
     */
    private void validateNotSellerAutoBidder(
            final Auction auction,
            final Participant participant) {

        final String sellerIdFromAuction = auction.getParticipant() != null
                ? auction.getParticipant().getId()
                : null;
        final String sellerIdFromItem = auction.getItem() != null
                ? auction.getItem().getSellerId()
                : null;

        if (participant.getId().equals(sellerIdFromAuction)
                || participant.getId().equals(sellerIdFromItem)) {
            throw new InvalidBidException(
                    "Người bán không được bật auto-bid cho sản phẩm của chính mình.");
        }
    }

    /**
     * Lấy lịch sử đặt giá của một phiên theo thứ tự thời gian tăng dần.
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
