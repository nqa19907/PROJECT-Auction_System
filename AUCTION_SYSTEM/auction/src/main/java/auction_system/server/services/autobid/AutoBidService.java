package auction_system.server.services.autobid;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AutoBidSetting;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.server.services.bidding.AuctionBidService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service quản lý cấu hình đấu giá tự động ở phía server.
 *
 * <p>Service giữ cache trong RAM để xử lý nhanh và có thể gắn repository để
 * lưu cấu hình xuống file serialization.
 */
public final class AutoBidService {
    /**
     * settingsByAuction[auctionId][userId]: setting.
     * Lưu cấu hình của userId trong auctionId.
     */
    private final Map<String, Map<String, AutoBidSetting>> settingsByAuction =
            new ConcurrentHashMap<>();

    /** Repository lưu cấu hình auto-bid, null nếu chạy in-memory. */
    private final AutoBidSettingStore autoBidSettingRepository;

    /**
     * Khởi tạo service auto-bid chỉ lưu trong RAM.
     */
    public AutoBidService() {
        this.autoBidSettingRepository = null;
    }

    /**
     * Khởi tạo service auto-bid có lưu cấu hình xuống database.
     *
     * @param autoBidSettingRepository repository lưu cấu hình auto-bid
     */
    public AutoBidService(final AutoBidSettingStore autoBidSettingRepository) {
        this.autoBidSettingRepository = Objects.requireNonNull(
                autoBidSettingRepository,
                "autoBidSettingRepository");
        loadPersistedSettings();
    }

    /**
     * Bật hoặc cập nhật cấu hình auto-bid của một participant trong một phiên.
     *
     * @param auctionId mã phiên đấu giá
     * @param currentUser user đang đăng nhập
     * @param maxAmount giá tối đa user cho phép auto-bid
     * @param stepAmount bước tăng mỗi lần bị vượt giá
     * @return cấu hình auto-bid đã được lưu
     */
    public AutoBidSetting enableAutoBid(
            final String auctionId,
            final User currentUser,
            final long maxAmount,
            final long stepAmount) {

        validateRequest(auctionId, currentUser, maxAmount, stepAmount);

        final Participant participant = (Participant) currentUser;
        final AutoBidSetting setting = new AutoBidSetting(
                auctionId,
                participant,
                maxAmount,
                stepAmount
        );

        // Cập nhật cache runtime để các bid sau có thể phản ứng ngay.
        settingsByAuction
                .computeIfAbsent(auctionId, key -> new ConcurrentHashMap<>())
                .put(participant.getId(), setting);

        saveSetting(setting);

        return setting;
    }

    /**
     * Tắt cấu hình auto-bid của một participant trong một phiên.
     *
     * @param auctionId mã phiên đấu giá
     * @param currentUser user đang đăng nhập
     * @return cấu hình auto-bid sau khi đã tắt
     */
    public AutoBidSetting disableAutoBid(
            final String auctionId,
            final User currentUser) {

        validateParticipantRequest(auctionId, currentUser);

        final Participant participant = (Participant) currentUser;
        final AutoBidSetting setting = findSetting(auctionId, participant.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bạn chưa bật auto-bid cho phiên này."));

        // Giữ lại setting nhưng chuyển inactive để restart không tự bật lại.
        setting.deactivate();
        saveSetting(setting);

        return setting;
    }

    /**
     * Lấy danh sách cấu hình auto-bid đang active của một phiên.
     *
     * @param auctionId mã phiên đấu giá
     * @return danh sách cấu hình active
     */
    public List<AutoBidSetting> findActiveSettings(final String auctionId) {
        final Map<String, AutoBidSetting> settings = settingsByAuction.get(auctionId);

        if (settings == null) {
            return List.of();
        }

        return settings.values()
                .stream()
                .filter(AutoBidSetting::isActive)
                .toList();
    }

    /**
     * Lấy toàn bộ cấu hình auto-bid đang active của một participant.
     *
     * <p>Luồng nạp tiền dùng method này để retry những auto-bid từng bị bỏ qua
     * vì thiếu số dư. Service chỉ trả setting, còn việc đọc auction, kiểm tra
     * trạng thái và ghi bid vẫn thuộc về {@link AuctionBidService}.
     *
     * @param participantId mã participant cần tìm cấu hình
     * @return danh sách setting active của participant, sắp xếp ổn định theo thời điểm tạo
     */
    public List<AutoBidSetting> findActiveSettingsForParticipant(final String participantId) {
        // Không có participantId hợp lệ thì không có khóa nghiệp vụ để tìm setting.
        if (participantId == null || participantId.isBlank()) {
            return List.of();
        }

        // Quét cache theo tất cả auction vì một user có thể bật auto-bid ở nhiều phiên.
        return settingsByAuction.values()
                .stream()
                .flatMap(settings -> settings.values().stream())
                .filter(AutoBidSetting::isActive)
                .filter(setting -> participantId.equals(setting.getParticipant().getId()))
                .sorted(Comparator
                        .comparing(AutoBidSetting::getCreatedAt)
                        .thenComparing(AutoBidSetting::getAuctionId))
                .toList();
    }

    /**
     * Tìm cấu hình auto-bid của một participant trong một phiên đấu giá.
     *
     * <p>Method này dùng cho luồng vừa bật auto-bid: nếu phiên chưa có bid nào,
     * tầng bid cần lấy đúng setting của user vừa bật để tạo bid mở đầu.
     *
     * @param auctionId mã phiên đấu giá
     * @param participantId mã participant cần tìm setting
     * @return setting nếu tồn tại, ngược lại là Optional rỗng
     */
    public Optional<AutoBidSetting> findSetting(
            final String auctionId,
            final String participantId) {

        // Không có auctionId hoặc participantId hợp lệ thì không thể tìm setting.
        if (auctionId == null || auctionId.isBlank()
                || participantId == null || participantId.isBlank()) {
            return Optional.empty();
        }

        // Lấy map setting của phiên đấu giá cần tìm.
        final Map<String, AutoBidSetting> settings = settingsByAuction.get(auctionId);
        if (settings == null) {
            return Optional.empty();
        }

        // Trả về setting theo participantId nếu đã được lưu trước đó.
        return Optional.ofNullable(settings.get(participantId));
    }

    /**
     * Tìm các cấu hình auto-bid có thể phản ứng với giá hiện tại.
     *
     * <p>Method này chỉ làm nhiệm vụ lọc và sắp xếp candidate. Nó không ghi bid,
     * không kiểm tra ví và không cập nhật database. Phần ghi bid vẫn thuộc về
     * {@link AuctionBidService}.
     *
     * @param auction phiên đấu giá vừa có bid mới
     * @return danh sách cấu hình auto-bid hợp lệ, đã sắp xếp theo độ ưu tiên
     */
    public List<AutoBidSetting> findEligibleSettings(final Auction auction) {
        // Nếu auction chưa có bid cao nhất thì auto-bid chưa có gì để phản ứng.
        final BidTransaction currentHighestBid = auction.getCurrentHighestBid();
        if (currentHighestBid == null || currentHighestBid.getParticipant() == null) {
            return List.of();
        }

        // Lọc setting active, còn đủ maxAmount để vượt giá hiện tại, rồi sort theo priority.
        return findActiveSettings(auction.getId())
                .stream()
                .filter(setting -> canAutoBid(setting, currentHighestBid))
                .sorted(this::compareAutoBidPriority)
                .toList();
    }

    /**
     * Kiểm tra một cấu hình auto-bid có thể phản ứng với giá hiện tại không.
     *
     * @param setting cấu hình auto-bid cần kiểm tra
     * @param currentHighestBid bid đang dẫn đầu hiện tại
     * @return true nếu setting có thể tạo bid mới
     */
    private boolean canAutoBid(
            final AutoBidSetting setting,
            final BidTransaction currentHighestBid) {

        // Người đang dẫn đầu không cần tự vượt chính mình.
        if (setting.getParticipant().getId()
                .equals(currentHighestBid.getParticipant().getId())) {
            return false;
        }

        // MaxAmount phải còn vượt giá hiện tại thì mới có thể phản ứng.
        return setting.getMaxAmount() > currentHighestBid.getAmount();
    }

    /**
     * So sánh độ ưu tiên giữa hai cấu hình auto-bid.
     *
     * <p>Thứ tự ưu tiên là: maxAmount cao hơn, thời điểm bật auto-bid sớm hơn,
     * rồi participantId để kết quả luôn ổn định nếu dữ liệu bằng nhau.
     *
     * @param left cấu hình thứ nhất
     * @param right cấu hình thứ hai
     * @return số âm nếu left nên đứng trước right
     */
    private int compareAutoBidPriority(
            final AutoBidSetting left,
            final AutoBidSetting right) {

        // Ưu tiên người có giá tối đa cao hơn.
        final int maxAmountComparison = Long.compare(
                right.getMaxAmount(),
                left.getMaxAmount()
        );
        if (maxAmountComparison != 0) {
            return maxAmountComparison;
        }

        // Nếu giá tối đa bằng nhau, ưu tiên người bật auto-bid sớm hơn.
        final int createdAtComparison = left.getCreatedAt().compareTo(right.getCreatedAt());
        if (createdAtComparison != 0) {
            return createdAtComparison;
        }

        // Nếu vẫn bằng nhau, dùng participantId để thứ tự luôn deterministic.
        return left.getParticipant().getId().compareTo(right.getParticipant().getId());
    }

    /**
     * Tính số tiền mà auto-bid cần đặt để người được chọn dẫn đầu tạm thời.
     *
     * <p>Nếu có đối thủ auto-bid đứng thứ hai, người dẫn đầu chỉ cần vượt đối
     * thủ đó một bước giá nhưng không vượt quá maxAmount. Nếu không có đối thủ
     * thứ hai, giá chỉ cần vượt giá hiện tại một bước.
     *
     * @param currentHighestBid bid đang dẫn đầu hiện tại
     * @param leadingSetting cấu hình auto-bid được chọn dẫn đầu tạm thời
     * @param secondSetting cấu hình đứng thứ hai, có thể null
     * @return số tiền auto-bid cần đặt
     */
    public double calculateAutoBidAmount(
            final BidTransaction currentHighestBid,
            final AutoBidSetting leadingSetting,
            final AutoBidSetting secondSetting) {

        // Nếu có người thứ hai, lấy maxAmount của họ làm mốc cạnh tranh.
        final double competitionBase = secondSetting == null
                ? currentHighestBid.getAmount()
                : secondSetting.getMaxAmount();

        // Giá đề xuất chỉ cần vượt mốc cạnh tranh một bước của người dẫn đầu.
        final double proposedAmount = competitionBase + leadingSetting.getStepAmount();

        // Không bao giờ đặt vượt quá mức tối đa người dùng đã cấu hình.
        return Math.min(leadingSetting.getMaxAmount(), proposedAmount);
    }

    private void validateRequest(
            final String auctionId,
            final User currentUser,
            final long maxAmount,
            final long stepAmount) {

        validateParticipantRequest(auctionId, currentUser);

        if (maxAmount <= 0) {
            throw new IllegalArgumentException("Giá tối đa phải là số dương.");
        }

        if (stepAmount <= 0) {
            throw new IllegalArgumentException("Bước tăng phải là số dương.");
        }
    }

    private void validateParticipantRequest(
            final String auctionId,
            final User currentUser) {

        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("Mã phiên đấu giá không được rỗng.");
        }

        if (!(currentUser instanceof Participant)) {
            throw new IllegalArgumentException("Chỉ người tham gia mới được bật auto-bid.");
        }
    }

    private void loadPersistedSettings() {
        // Nạp toàn bộ setting đã lưu vào cache runtime khi server/service khởi động.
        for (AutoBidSetting setting : autoBidSettingRepository.findAll()) {
            settingsByAuction
                    .computeIfAbsent(setting.getAuctionId(), key -> new ConcurrentHashMap<>())
                    .merge(
                            setting.getParticipant().getId(),
                            setting,
                            this::chooseLatestSetting);
        }
    }

    private AutoBidSetting chooseLatestSetting(
            final AutoBidSetting existing,
            final AutoBidSetting loaded) {

        // Nếu file có nhiều setting cùng user/auction, giữ bản được tạo mới hơn.
        return loaded.getCreatedAt().isAfter(existing.getCreatedAt())
                ? loaded
                : existing;
    }

    private void saveSetting(final AutoBidSetting setting) {
        // Constructor in-memory không có repository nên chỉ cập nhật cache.
        if (autoBidSettingRepository == null) {
            return;
        }

        // Repository lưu theo id, nên xóa setting cũ cùng user/auction trước khi lưu bản mới.
        findExistingPersistedSetting(
                setting.getAuctionId(),
                setting.getParticipant().getId())
                .ifPresent(existing -> autoBidSettingRepository.deleteById(existing.getId()));

        // Lưu cả setting active và inactive để trạng thái disable sống qua restart.
        autoBidSettingRepository.save(setting);
    }

    private Optional<AutoBidSetting> findExistingPersistedSetting(
            final String auctionId,
            final String participantId) {

        // Tìm theo khóa nghiệp vụ auctionId + participantId thay vì id ngẫu nhiên.
        return autoBidSettingRepository.findByAuctionId(auctionId)
                .stream()
                .filter(setting -> participantId.equals(setting.getParticipant().getId()))
                .findFirst();
    }
}
