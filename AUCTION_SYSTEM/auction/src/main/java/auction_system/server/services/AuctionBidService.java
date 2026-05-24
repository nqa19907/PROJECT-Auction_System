package auction_system.server.services;

import auction_system.common.exceptions.InvalidBidException;
import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
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
 * <p>Lớp này là nơi duy nhất nên xử lý thao tác đặt giá thật sự trên server.
 * Command chỉ đọc request và trả response, còn service chịu trách nhiệm kiểm
 * tra quyền, kiểm tra dữ liệu, cập nhật auction, lưu lịch sử bid và đảm bảo
 * thao tác ghi database được thực hiện trong transaction.
 */
public class AuctionBidService {

    private static final Logger LOGGER =
            Logger.getLogger(AuctionBidService.class.getName());

    /** Database serialization dùng chung phía server. */
    private final SerializedDatabase database;
    /** Quản lý runtime dùng để gửi realtime theo user đang online. */
    private final AuctionManager auctionManager;

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
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    /**
     * Đặt giá cho một phiên đấu giá.
     *
     * <p>Phương thức này dùng transaction ở mức database để tránh trường hợp nhiều
     * client cùng đặt giá và ghi chéo dữ liệu. Sau khi {@link Auction#placeBid}
     * xử lý hợp lệ, service lưu cả giao dịch đặt giá và phiên đấu giá đã cập nhật.
     *
     * @param auctionId   mã phiên đấu giá
     * @param currentUser người dùng hiện tại trong session
     * @param amount      số tiền đặt giá
     * @return giao dịch đặt giá đã được ghi nhận
     */
    public BidTransaction placeBid(
            final String auctionId,
            final User currentUser,
            final double amount) {
        
        validateRequest(auctionId, currentUser, amount);

        SavedBidResult savedBidResult = database.executeInTransaction(() -> {
            Auction auction = findAuctionOrThrow(auctionId);
            Participant bidder = (Participant) currentUser;
            BidTransaction previousHighestBid = auction.getCurrentHighestBid();

            validateAvailableBalance(bidder, previousHighestBid, amount);

            BidTransaction bidTransaction = new BidTransaction(bidder, amount, auction);
            auction.placeBid(bidTransaction);

            refundPreviousHighestBid(previousHighestBid);
            debitBidder(bidder, amount);

            saveAffectedUsers(bidder, previousHighestBid);
            database.bidTransactions().save(bidTransaction);
            database.auctions().save(auction);
            database.flushAll();

            // Giữ lại người dẫn đầu cũ để sau transaction có thể gửi realtime số dư mới.
            Participant previousBidder = previousHighestBid == null
                    ? null
                    : previousHighestBid.getParticipant();

            return new SavedBidResult(
                    bidTransaction,
                    auction,
                    bidder,
                    previousBidder);

        });

        // Sau khi bid transaction và auction đã được lưu, mới broadcast realtime.
        savedBidResult.auction().notifyObservers();

        // Gửi cập nhật ví cho người vừa đặt giá vì số dư của họ đã bị giữ lại.
        notifyBalanceUpdated(savedBidResult.bidder());

        // Nếu có người dẫn đầu cũ khác người đặt giá mới, gửi số dư mới sau khi hoàn tiền.
        Participant previousBidder = savedBidResult.previousBidder();
        if (previousBidder != null
                && !previousBidder.getId().equals(savedBidResult.bidder().getId())) {
            notifyBalanceUpdated(previousBidder);
        }

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
     * Kiểm tra request đặt giá trước khi thao tác database.
     *
     * @param auctionId   mã phiên đấu giá
     * @param currentUser người dùng hiện tại
     * @param amount      số tiền đặt giá
     */
    private void validateRequest(
            final String auctionId,
            final User currentUser,
            final double amount) {
        
        if (auctionId == null || auctionId.isBlank()) {
            throw new InvalidBidException("Mã phiên đấu giá không được rỗng.");
        }

        if (currentUser == null) {
            throw new InvalidBidException("Bạn cần đăng nhập trước khi đặt giá.");
        }

        if (!(currentUser instanceof Participant)) {
            throw new InvalidBidException("Chỉ người mua mới có thể đặt giá.");
        }

        if (amount <= 0) {
            throw new InvalidBidException("Số tiền đặt giá phải lớn hơn 0.");
        }
    }

    /**
     * Kiểm tra số dư khả dụng của người đặt giá.
     *
     * <p>Nếu người đặt giá hiện đang dẫn đầu phiên này, số tiền đang bị giữ ở
     * bid cũ được tính lại vào số dư khả dụng trước khi họ nâng giá.
     *
     * @param bidder người đặt giá
     * @param previousHighestBid lượt đặt giá đang dẫn đầu trước đó
     * @param amount số tiền đặt giá mới
     */
    private void validateAvailableBalance(
            final Participant bidder,
            final BidTransaction previousHighestBid,
            final double amount) {

        double availableBalance = bidder.getBalance();

        if (isSameBidder(bidder, previousHighestBid)) {
            availableBalance += previousHighestBid.getAmount();
        }

        if (amount > availableBalance) {
            throw new InvalidBidException("Không đủ số dư để đặt giá.");
        }
    }

    /**
     * Hoàn tiền cho người đang dẫn đầu cũ khi họ bị lượt đặt giá mới vượt qua.
     *
     * @param previousHighestBid lượt đặt giá đang dẫn đầu trước đó
     */
    private void refundPreviousHighestBid(final BidTransaction previousHighestBid) {
        if (previousHighestBid == null || previousHighestBid.getParticipant() == null) {
            return;
        }

        Participant previousBidder = previousHighestBid.getParticipant();
        previousBidder.setBalance(
                previousBidder.getBalance() + previousHighestBid.getAmount());
    }

    /**
     * Giữ tiền của người đặt giá mới.
     *
     * @param bidder người đặt giá mới
     * @param amount số tiền cần giữ
     */
    private void debitBidder(final Participant bidder, final double amount) {
        bidder.setBalance(bidder.getBalance() - amount);
    }

    /**
     * Lưu các user có số dư bị thay đổi sau khi đặt giá.
     *
     * @param bidder người đặt giá mới
     * @param previousHighestBid lượt đặt giá đang dẫn đầu trước đó
     */
    private void saveAffectedUsers(
            final Participant bidder,
            final BidTransaction previousHighestBid) {

        database.users().save(bidder);

        if (previousHighestBid == null || previousHighestBid.getParticipant() == null) {
            return;
        }

        Participant previousBidder = previousHighestBid.getParticipant();
        if (!previousBidder.getId().equals(bidder.getId())) {
            database.users().save(previousBidder);
        }
    }

    /**
     * Kiểm tra người đặt giá mới có phải người đang dẫn đầu cũ không.
     *
     * @param bidder người đặt giá mới
     * @param previousHighestBid lượt đặt giá đang dẫn đầu trước đó
     * @return true nếu cùng một người dùng
     */
    private boolean isSameBidder(
            final Participant bidder,
            final BidTransaction previousHighestBid) {

        return previousHighestBid != null
                && previousHighestBid.getParticipant() != null
                && previousHighestBid.getParticipant().getId().equals(bidder.getId());
    }

    /**
     * Gửi realtime số dư mới cho một user bị ảnh hưởng bởi lượt đặt giá.
     *
     * <p>Message được gửi theo user online thay vì theo auction observer, để ví
     * vẫn cập nhật realtime dù user đang ở màn hình chính hoặc xem phiên khác.
     *
     * @param participant user cần nhận số dư mới
     */
    private void notifyBalanceUpdated(final Participant participant) {
        if (participant == null) {
            return;
        }

        String message = Protocol.Response.BALANCE_UPDATED.name()
                + Protocol.SEPARATOR
                + participant.getId()
                + Protocol.SEPARATOR
                + participant.getBalance();

        auctionManager.notifyUser(participant.getId(), message);
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
        validateAuctionId(auctionId);

        return database.bidTransactions()
                .findByAuctionId(auctionId)
                .stream()
                .sorted(Comparator.comparing(BidTransaction::getTimestamp))
                .toList();
    }

    /**
     * Kiểm tra mã phiên cho các nghiệp vụ chỉ cần auctionId.
     *
     * @param auctionId mã phiên đấu giá cần kiểm tra
     */
    private void validateAuctionId(final String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            throw new InvalidBidException("Mã phiên đấu giá không được rỗng.");
        }
    }
}
