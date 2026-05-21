package auction_system.server.services;

import auction_system.common.exceptions.InvalidBidException;
import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.server.persistence.exceptions.DatabaseException;
import auction_system.server.persistence.serialization.SerializedDatabase;
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

    /**
     * Khởi tạo service đặt giá.
     *
     * @param database database serialization của server
     */
    public AuctionBidService(final SerializedDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
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

        BidTransaction savedBid = database.executeInTransaction(() -> {
            Auction auction = findAuctionOrThrow(auctionId);
            Participant bidder = (Participant) currentUser;

            BidTransaction bidTransaction = new BidTransaction(bidder, amount, auction);
            auction.placeBid(bidTransaction);

            database.bidTransactions().save(bidTransaction);
            database.auctions().save(auction);
            database.flushAll();

            return bidTransaction;
        });

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
        
        Participant bidder = (Participant) currentUser;
        if (amount > bidder.getBalance()) {
            throw new InvalidBidException("Không đủ số dư để đặt giá.");
        }
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
}