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

    /** Chặn vòng lặp auto-bid chạy vô hạn nếu dữ liệu hoặc rule bị lỗi. */
    private static final int MAX_AUTO_BID_CHAIN_LENGTH = 100;

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

            // Resolve toàn bộ chuỗi auto-bid phản ứng sau bid thủ công.
            savedBidResults.addAll(resolveAutoBidChainInTransaction(auction, null));

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
            refreshAuctionLifecycle(auction);

            if (auction.getStatus() != AuctionStatus.RUNNING) {
                return List.of();
            }

            // Lấy đúng setting của user vừa bật/cập nhật để không kích hoạt nhầm user khác.
            final AutoBidSetting enabledSetting =
                    autoBidService.findSetting(auctionId, participant.getId()).orElse(null);

            final SavedBidResult autoBidResult;
            if (auction.getCurrentHighestBid() == null) {
                autoBidResult = trySaveOpeningAutoBidInTransaction(
                        auction,
                        enabledSetting,
                        participant);
            } else {
                autoBidResult = trySaveSpecificAutoBidInTransaction(
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
            results.addAll(resolveAutoBidChainInTransaction(auction, participant));

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
                refreshAuctionLifecycle(auction);
                if (auction.getStatus() != AuctionStatus.RUNNING) {
                    continue;
                }

                // Thử đúng setting của user vừa nạp trước, tránh kích hoạt nhầm user khác.
                final SavedBidResult autoBidResult;
                if (auction.getCurrentHighestBid() == null) {
                    autoBidResult = trySaveOpeningAutoBidInTransaction(
                            auction,
                            setting,
                            participant);
                } else {
                    autoBidResult = trySaveSpecificAutoBidInTransaction(
                            auction,
                            setting,
                            participant);
                }

                if (autoBidResult != null) {
                    results.add(autoBidResult);

                    // Sau bid mới do nạp tiền, cho các auto-bidder khác phản ứng dây chuyền.
                    results.addAll(resolveAutoBidChainInTransaction(auction, participant));
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
     * Ghi một lượt bid vào auction và cập nhật ví các user liên quan.
     *
     * <p>Helper này giả định caller đang ở trong database transaction. Cả bid
     * thủ công và auto-bid dùng chung method này để tránh lệch logic
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
     * Resolve chuỗi auto-bid cho tới khi phiên ổn định.
     *
     * <p>Mỗi vòng chỉ ghi tối đa một bid tự động. Sau khi bid đó trở thành giá
     * cao nhất mới, vòng kế tiếp đọc lại trạng thái auction để xem auto-bidder
     * khác có cần phản ứng tiếp không. Cách này xử lý các case proxy bidding
     * như hai maxAmount sát nhau hoặc người dẫn đầu cũ có max cao hơn.
     *
     * @param auction phiên đấu giá đang xử lý trong transaction hiện tại
     * @param preferredParticipant participant có object số dư mới nhất, có thể null
     * @return danh sách bid tự động đã lưu trong chuỗi phản ứng
     */
    private List<SavedBidResult> resolveAutoBidChainInTransaction(
            final Auction auction,
            final Participant preferredParticipant) {

        final List<SavedBidResult> results = new ArrayList<>();

        // Lặp có giới hạn để tránh vòng lặp vô hạn nếu rule hoặc dữ liệu bất thường.
        for (int iteration = 0; iteration < MAX_AUTO_BID_CHAIN_LENGTH; iteration++) {
            final SavedBidResult autoBidResult =
                    trySaveNextAutoBidInTransaction(auction, preferredParticipant);

            // Không ghi được bid mới nghĩa là phiên đã ổn định với các setting hiện tại.
            if (autoBidResult == null) {
                return results;
            }

            // Ghi nhận bid vừa tạo rồi tiếp tục kiểm tra phản ứng kế tiếp.
            results.add(autoBidResult);
        }

        LOGGER.warning(
                "Dừng chuỗi auto-bid vì vượt quá "
                        + MAX_AUTO_BID_CHAIN_LENGTH
                        + " vòng cho auction "
                        + auction.getId());
        return results;
    }

    /**
     * Thử ghi một bid tự động tiếp theo từ trạng thái auction hiện tại.
     *
     * <p>Candidate có độ ưu tiên cao nhất vẫn được thử trước. Nếu candidate đó
     * không thể đặt giá vì thiếu số dư hoặc mức giá không còn hợp lệ, service bỏ
     * qua candidate đó và thử người kế tiếp. Nhờ vậy một cấu hình auto-bid lỗi
     * không chặn các candidate hợp lệ phía sau.
     *
     * @param auction phiên đấu giá đang xử lý trong transaction hiện tại
     * @param preferredParticipant participant có object số dư mới nhất, có thể null
     * @return bid tự động đầu tiên ghi thành công, hoặc null nếu không ai đặt được
     */
    private SavedBidResult trySaveNextAutoBidInTransaction(
            final Auction auction,
            final Participant preferredParticipant) {

        // Lấy candidate mới nhất theo giá hiện tại sau mỗi lượt bid trong chuỗi.
        final List<AutoBidSetting> settings = autoBidService.findEligibleSettings(auction);
        if (settings.isEmpty()) {
            return null;
        }

        // Duyệt từng candidate theo priority và dừng ngay khi ghi thành công một bid.
        for (int index = 0; index < settings.size(); index++) {
            final AutoBidSetting setting = settings.get(index);
            final SavedBidResult autoBidResult =
                    trySaveAutoBidCandidateInTransaction(
                            auction,
                            setting,
                            index + 1 < settings.size() ? settings.get(index + 1) : null,
                            preferredParticipant);

            // Candidate hiện tại ghi thành công thì trả ngay để vòng ngoài đọc lại trạng thái.
            if (autoBidResult != null) {
                return autoBidResult;
            }
        }

        // Không candidate nào ghi bid thành công.
        return null;
    }

    /**
     * Thử ghi bid tự động cho một candidate cụ thể trong danh sách ưu tiên.
     *
     * <p>Method này được dùng bởi resolver dây chuyền. Nó chỉ xử lý một
     * candidate, tính mức cần đặt theo đối thủ cạnh tranh mạnh nhất rồi ghi bid
     * qua helper chung để đồng nhất validate ví, hoàn tiền và persistence.
     *
     * @param auction phiên đấu giá đang xử lý trong transaction hiện tại
     * @param setting candidate đang được thử
     * @param nextCandidateSetting candidate tiếp theo trong danh sách ưu tiên, có thể null
     * @param preferredParticipant participant có object số dư mới nhất, có thể null
     * @return bid tự động đã lưu, hoặc null nếu setting không cần/không thể bid
     */
    private SavedBidResult trySaveAutoBidCandidateInTransaction(
            final Auction auction,
            final AutoBidSetting setting,
            final AutoBidSetting nextCandidateSetting,
            final Participant preferredParticipant) {

        if (setting == null || !setting.isActive()) {
            return null;
        }

        final BidTransaction currentHighestBid = auction.getCurrentHighestBid();
        if (currentHighestBid == null || currentHighestBid.getParticipant() == null) {
            return null;
        }

        if (setting.getMaxAmount() <= currentHighestBid.getAmount()) {
            return null;
        }

        // Tìm mức cạnh tranh mạnh nhất để tính giá candidate cần đặt.
        final AutoBidSetting competitorSetting = findStrongestCompetitorSetting(
                auction,
                setting,
                nextCandidateSetting);
        final double nextAmount = autoBidService.calculateAutoBidAmount(
                currentHighestBid,
                setting,
                competitorSetting);

        if (nextAmount <= currentHighestBid.getAmount()) {
            return null;
        }

        try {
            // Dùng participant mới nhất nếu caller vừa nạp tiền cho đúng số dư runtime.
            return saveBidInTransaction(
                    auction,
                    resolveBidder(setting, preferredParticipant),
                    nextAmount);
        } catch (InvalidBidException exception) {
            LOGGER.fine(
                    "Bỏ qua auto-bid của participant "
                            + setting.getParticipant().getId()
                            + ": "
                            + exception.getMessage());
            return null;
        }
    }

    /**
     * Thử ghi bid tự động cho đúng setting vừa được bật, chỉnh sửa hoặc nạp tiền.
     *
     * <p>Luồng này không cho candidate khác tự bid chỉ vì user hiện tại chỉnh
     * cấu hình hoặc nạp tiền. Nếu user hiện tại đang dẫn đầu và không có đối thủ
     * auto-bid nào đang cạnh tranh, service không tự nâng giá thêm.
     *
     * @param auction phiên đấu giá đang xử lý trong transaction hiện tại
     * @param setting cấu hình vừa được trigger
     * @param participant user vừa trigger, có thể là object số dư mới nhất
     * @return bid tự động đã lưu, hoặc null nếu chưa đủ điều kiện
     */
    private SavedBidResult trySaveSpecificAutoBidInTransaction(
            final Auction auction,
            final AutoBidSetting setting,
            final Participant participant) {

        if (setting == null || !setting.isActive()) {
            return null;
        }

        final BidTransaction currentHighestBid = auction.getCurrentHighestBid();
        if (currentHighestBid == null || currentHighestBid.getParticipant() == null) {
            return null;
        }

        // MaxAmount phải còn vượt giá hiện tại thì auto-bid mới có ý nghĩa.
        if (setting.getMaxAmount() <= currentHighestBid.getAmount()) {
            return null;
        }

        final boolean currentUserIsHighestBidder =
                currentHighestBid.getParticipant().getId().equals(participant.getId());
        final AutoBidSetting strongestActiveCompetitor =
                findStrongestActiveSettingExcept(auction, setting);

        // Người đang dẫn đầu chỉ tự nâng giá nếu có đối thủ auto-bid thực sự cạnh tranh.
        if (currentUserIsHighestBidder && strongestActiveCompetitor == null) {
            return null;
        }

        // Tìm mức cạnh tranh mạnh nhất để tính giá auto-bid cần đặt.
        final AutoBidSetting competitorSetting = findStrongestCompetitorSetting(
                auction,
                setting,
                findStrongestActiveSettingExcept(auction, setting));
        final double nextAmount = autoBidService.calculateAutoBidAmount(
                currentHighestBid,
                setting,
                competitorSetting);

        if (nextAmount <= currentHighestBid.getAmount()) {
            return null;
        }

        try {
            // Ghi bid bằng participant vừa trigger để validate đúng số dư mới nhất.
            return saveBidInTransaction(
                    auction,
                    participant,
                    nextAmount);
        } catch (InvalidBidException exception) {
            LOGGER.fine(
                    "Bỏ qua auto-bid vừa trigger của participant "
                            + participant.getId()
                            + ": "
                            + exception.getMessage());
            return null;
        }
    }

    private Participant resolveBidder(
            final AutoBidSetting setting,
            final Participant preferredParticipant) {

        // Ưu tiên object participant mới nhất khi caller vừa nạp tiền trong session hiện tại.
        if (preferredParticipant != null
                && preferredParticipant.getId().equals(setting.getParticipant().getId())) {
            return preferredParticipant;
        }

        // Nếu không có participant mới hơn, dùng participant đang nằm trong setting.
        return setting.getParticipant();
    }

    /**
     * Tìm setting cạnh tranh mạnh nhất với candidate đang được thử.
     *
     * <p>Danh sách eligible không chứa người đang dẫn đầu, nhưng nếu người đó
     * có auto-bid active thì maxAmount của họ vẫn là mức cạnh tranh thực tế.
     *
     * @param auction phiên đấu giá đang xử lý
     * @param leadingSetting candidate đang được thử ghi bid
     * @param nextCandidateSetting candidate tiếp theo trong danh sách ưu tiên
     * @return setting cạnh tranh mạnh nhất, hoặc null nếu không có
     */
    private AutoBidSetting findStrongestCompetitorSetting(
            final Auction auction,
            final AutoBidSetting leadingSetting,
            final AutoBidSetting nextCandidateSetting) {

        final AutoBidSetting currentLeaderSetting = findCurrentLeaderAutoBidSetting(
                auction,
                leadingSetting);

        if (currentLeaderSetting == null) {
            return nextCandidateSetting;
        }

        if (nextCandidateSetting == null) {
            return currentLeaderSetting;
        }

        return currentLeaderSetting.getMaxAmount() > nextCandidateSetting.getMaxAmount()
                ? currentLeaderSetting
                : nextCandidateSetting;
    }

    private AutoBidSetting findCurrentLeaderAutoBidSetting(
            final Auction auction,
            final AutoBidSetting leadingSetting) {

        final BidTransaction currentHighestBid = auction.getCurrentHighestBid();
        if (currentHighestBid == null || currentHighestBid.getParticipant() == null) {
            return null;
        }

        final String currentLeaderId = currentHighestBid.getParticipant().getId();
        if (currentLeaderId.equals(leadingSetting.getParticipant().getId())) {
            return null;
        }

        return autoBidService
                .findSetting(auction.getId(), currentLeaderId)
                .filter(AutoBidSetting::isActive)
                .filter(setting -> setting.getMaxAmount() > currentHighestBid.getAmount())
                .orElse(null);
    }

    private AutoBidSetting findStrongestActiveSettingExcept(
            final Auction auction,
            final AutoBidSetting excludedSetting) {

        AutoBidSetting strongestSetting = null;

        for (AutoBidSetting setting : autoBidService.findActiveSettings(auction.getId())) {
            if (setting.getParticipant().getId()
                    .equals(excludedSetting.getParticipant().getId())) {
                continue;
            }

            if (strongestSetting == null
                    || setting.getMaxAmount() > strongestSetting.getMaxAmount()) {
                strongestSetting = setting;
            }
        }

        return strongestSetting;
    }

    /**
     * Thử ghi bid mở đầu cho người vừa bật auto-bid.
     *
     * <p>Bid mở đầu dùng đúng giá khởi điểm của item. Nếu maxAmount của setting
     * không đủ chạm giá khởi điểm, service không tạo bid và vẫn giữ setting đã bật.
     *
     * @param auction phiên đấu giá đang xử lý trong transaction hiện tại
     * @param setting cấu hình của người vừa bật auto-bid, có thể null
     * @return bid mở đầu đã lưu, hoặc null nếu chưa đủ điều kiện
     */
    private SavedBidResult trySaveOpeningAutoBidInTransaction(
            final Auction auction,
            final AutoBidSetting setting) {

        if (setting == null || !setting.isActive()
                || auction.getCurrentHighestBid() != null) {
            return null;
        }

        final double openingAmount = auction.getItem().getStartPrice();
        if (setting.getMaxAmount() < openingAmount) {
            return null;
        }

        try {
            return saveBidInTransaction(
                    auction,
                    setting.getParticipant(),
                    openingAmount);
        } catch (InvalidBidException exception) {
            LOGGER.fine(
                    "Bỏ qua auto-bid mở đầu của participant "
                            + setting.getParticipant().getId()
                            + ": "
                            + exception.getMessage());
            return null;
        }
    }

    /**
     * Thử ghi bid mở đầu bằng participant có số dư mới nhất.
     *
     * <p>Luồng nạp tiền không thể luôn dùng participant nằm trong setting vì
     * setting có thể được nạp từ persistence và không cùng object với session
     * hiện tại. Method này giữ luật mở đầu cũ nhưng ghi bid bằng user vừa nạp.
     *
     * @param auction phiên đấu giá đang xử lý trong transaction hiện tại
     * @param setting cấu hình auto-bid đang active
     * @param participant user vừa nạp tiền
     * @return bid mở đầu đã lưu, hoặc null nếu chưa đủ điều kiện
     */
    private SavedBidResult trySaveOpeningAutoBidInTransaction(
            final Auction auction,
            final AutoBidSetting setting,
            final Participant participant) {

        if (setting == null || !setting.isActive()
                || auction.getCurrentHighestBid() != null) {
            return null;
        }

        // Bid mở đầu chỉ được tạo nếu maxAmount đủ chạm giá khởi điểm.
        final double openingAmount = auction.getItem().getStartPrice();
        if (setting.getMaxAmount() < openingAmount) {
            return null;
        }

        try {
            // Ghi bid bằng participant vừa nạp tiền để kiểm tra đúng số dư mới.
            return saveBidInTransaction(
                    auction,
                    participant,
                    openingAmount);
        } catch (InvalidBidException exception) {
            LOGGER.fine(
                    "Bỏ qua auto-bid mở đầu sau nạp tiền của participant "
                            + participant.getId()
                            + ": "
                            + exception.getMessage());
            return null;
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
