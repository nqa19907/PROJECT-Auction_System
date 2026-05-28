package auction_system.server.services.bidding;

import auction_system.common.exceptions.InvalidBidException;
import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AutoBidSetting;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.users.Participant;
import auction_system.server.services.autobid.AutoBidService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Resolve các lượt auto-bid phát sinh từ bid thủ công, bật auto-bid hoặc nạp tiền.
 */
final class AutoBidProcessor {

    private static final Logger LOGGER =
            Logger.getLogger(AutoBidProcessor.class.getName());

    /** Chặn vòng lặp auto-bid chạy vô hạn nếu dữ liệu hoặc rule bị lỗi. */
    private static final int MAX_AUTO_BID_CHAIN_LENGTH = 100;

    private final AutoBidService autoBidService;
    private final BidRecorder bidRecorder;

    AutoBidProcessor(
            final AutoBidService autoBidService,
            final BidRecorder bidRecorder) {

        this.autoBidService = Objects.requireNonNull(autoBidService, "autoBidService");
        this.bidRecorder = Objects.requireNonNull(bidRecorder, "bidRecorder");
    }

    List<SavedBidResult> resolveAutoBidChainInTransaction(
            final Auction auction,
            final Participant preferredParticipant) {

        final List<SavedBidResult> results = new ArrayList<>();

        // Mỗi vòng chỉ ghi một auto-bid rồi đọc lại trạng thái auction mới nhất.
        for (int iteration = 0; iteration < MAX_AUTO_BID_CHAIN_LENGTH; iteration++) {
            final SavedBidResult autoBidResult =
                    trySaveNextAutoBidInTransaction(auction, preferredParticipant);

            // Không còn candidate ghi được bid nghĩa là chuỗi auto-bid đã ổn định.
            if (autoBidResult == null) {
                return results;
            }

            // Lưu kết quả để caller notify sau khi transaction commit.
            results.add(autoBidResult);
        }

        LOGGER.warning(
                "Dừng chuỗi auto-bid vì vượt quá "
                        + MAX_AUTO_BID_CHAIN_LENGTH
                        + " vòng cho auction "
                        + auction.getId());
        return results;
    }

    SavedBidResult trySaveSpecificAutoBidInTransaction(
            final Auction auction,
            final AutoBidSetting setting,
            final Participant participant) {

        // Bỏ qua setting không tồn tại hoặc đã tắt trước khi tính auto-bid.
        if (setting == null || !setting.isActive()) {
            return null;
        }

        // Auto-bid chỉ phản ứng khi phiên đã có một bid cao nhất hợp lệ.
        final BidTransaction currentHighestBid = auction.getCurrentHighestBid();
        if (currentHighestBid == null || currentHighestBid.getParticipant() == null) {
            return null;
        }

        // MaxAmount phải còn vượt giá hiện tại thì setting mới có thể đặt tiếp.
        if (setting.getMaxAmount() <= currentHighestBid.getAmount()) {
            return null;
        }

        // Nếu user đang dẫn đầu, chỉ tự nâng giá khi có auto-bidder khác cạnh tranh.
        final boolean currentUserIsHighestBidder =
                currentHighestBid.getParticipant().getId().equals(participant.getId());
        final AutoBidSetting strongestActiveCompetitor =
                findStrongestActiveSettingExcept(auction, setting);

        if (currentUserIsHighestBidder && strongestActiveCompetitor == null) {
            return null;
        }

        // Tính mức giá cần đặt dựa trên đối thủ auto-bid mạnh nhất hiện tại.
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
            // Ghi bid bằng participant vừa trigger để dùng đúng object số dư mới nhất.
            return bidRecorder.saveBidInTransaction(
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

    SavedBidResult trySaveOpeningAutoBidInTransaction(
            final Auction auction,
            final AutoBidSetting setting,
            final Participant participant) {

        // Bid mở đầu chỉ áp dụng cho setting active và auction chưa có giá nào.
        if (setting == null || !setting.isActive()
                || auction.getCurrentHighestBid() != null) {
            return null;
        }

        // Giá mở đầu dùng startPrice, setting phải đủ maxAmount để chạm mức này.
        final double openingAmount = auction.getItem().getStartPrice();
        if (setting.getMaxAmount() < openingAmount) {
            return null;
        }

        try {
            // Ghi bid mở đầu qua recorder để dùng chung validate ví và persistence.
            return bidRecorder.saveBidInTransaction(
                    auction,
                    participant,
                    openingAmount);
        } catch (InvalidBidException exception) {
            LOGGER.fine(
                    "Bỏ qua auto-bid mở đầu của participant "
                            + participant.getId()
                            + ": "
                            + exception.getMessage());
            return null;
        }
    }

    private SavedBidResult trySaveNextAutoBidInTransaction(
            final Auction auction,
            final Participant preferredParticipant) {

        // Lấy danh sách candidate còn đủ điều kiện phản ứng với giá hiện tại.
        final List<AutoBidSetting> settings = autoBidService.findEligibleSettings(auction);
        if (settings.isEmpty()) {
            return null;
        }

        // Thử theo priority và dừng ngay khi ghi thành công một auto-bid.
        for (int index = 0; index < settings.size(); index++) {
            final AutoBidSetting setting = settings.get(index);
            final SavedBidResult autoBidResult =
                    trySaveAutoBidCandidateInTransaction(
                            auction,
                            setting,
                            index + 1 < settings.size() ? settings.get(index + 1) : null,
                            preferredParticipant);

            if (autoBidResult != null) {
                return autoBidResult;
            }
        }

        return null;
    }

    private SavedBidResult trySaveAutoBidCandidateInTransaction(
            final Auction auction,
            final AutoBidSetting setting,
            final AutoBidSetting nextCandidateSetting,
            final Participant preferredParticipant) {

        // Bỏ qua candidate không còn active trong cache auto-bid.
        if (setting == null || !setting.isActive()) {
            return null;
        }

        // Candidate chỉ có thể phản ứng khi auction đã có người dẫn đầu.
        final BidTransaction currentHighestBid = auction.getCurrentHighestBid();
        if (currentHighestBid == null || currentHighestBid.getParticipant() == null) {
            return null;
        }

        // Candidate không cần đặt nếu maxAmount không vượt được giá hiện tại.
        if (setting.getMaxAmount() <= currentHighestBid.getAmount()) {
            return null;
        }

        // Xác định đối thủ cạnh tranh mạnh nhất để tính giá proxy-bid.
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
            // Dùng participant mới nhất nếu caller vừa nạp tiền cho user này.
            return bidRecorder.saveBidInTransaction(
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

    private Participant resolveBidder(
            final AutoBidSetting setting,
            final Participant preferredParticipant) {

        // Ưu tiên object participant mới nhất để validate đúng số dư runtime.
        if (preferredParticipant != null
                && preferredParticipant.getId().equals(setting.getParticipant().getId())) {
            return preferredParticipant;
        }

        // Không có object mới hơn thì dùng participant đang nằm trong setting.
        return setting.getParticipant();
    }

    private AutoBidSetting findStrongestCompetitorSetting(
            final Auction auction,
            final AutoBidSetting leadingSetting,
            final AutoBidSetting nextCandidateSetting) {

        // Người đang dẫn đầu vẫn là đối thủ nếu họ có auto-bid active đủ cao.
        final AutoBidSetting currentLeaderSetting = findCurrentLeaderAutoBidSetting(
                auction,
                leadingSetting);

        // Nếu leader hiện tại không có setting cạnh tranh, dùng candidate kế tiếp.
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

        // Chỉ tìm setting của leader khi auction đang có bid cao nhất hợp lệ.
        final BidTransaction currentHighestBid = auction.getCurrentHighestBid();
        if (currentHighestBid == null || currentHighestBid.getParticipant() == null) {
            return null;
        }

        // Candidate đang được thử không cạnh tranh với chính setting của mình.
        final String currentLeaderId = currentHighestBid.getParticipant().getId();
        if (currentLeaderId.equals(leadingSetting.getParticipant().getId())) {
            return null;
        }

        // Setting của leader chỉ được tính nếu còn active và maxAmount vượt giá hiện tại.
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

        // Quét các setting active khác user hiện tại để tìm maxAmount cao nhất.
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
}
