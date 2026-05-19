package auction_system.server.persistence.repositories;

import auction_system.common.models.auctions.BidTransaction;
import auction_system.server.persistence.exceptions.DatabaseException;
import auction_system.server.persistence.serialization.SerializedFileStorage;
import auction_system.server.persistence.serialization.SerializedRepository;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Repository quản lý dữ liệu giao dịch đặt giá trong hệ thống.
 *
 * <p>Lớp này lưu toàn bộ lịch sử đặt giá vào file
 * {@code bid_transactions.ser} thông qua Java Serialization. Repository này
 * không tự xử lý logic đặt giá hợp lệ, vì logic đó thuộc về Auction hoặc
 * AuctionManager.
 */
public class BidTransactionRepository
    extends SerializedRepository<BidTransaction> {

    /**
     * Khởi tạo repository giao dịch đặt giá với đường dẫn file lưu trữ.
     *
     * @param storagePath đường dẫn tới file bid_transactions.ser
     */
    public BidTransactionRepository(final Path storagePath) {
        super(new SerializedFileStorage<>(storagePath), BidTransaction::getId);
    }

    /**
     * Lưu mới hoặc cập nhật giao dịch đặt giá.
     *
     * @param bidTransaction giao dịch đặt giá cần lưu
     * @return giao dịch đặt giá đã được lưu
     */
    @Override
    public synchronized BidTransaction save(
        final BidTransaction bidTransaction) {
        Objects.requireNonNull(bidTransaction, "bidTransaction");

        validateBidTransaction(bidTransaction);

        return super.save(bidTransaction);
    }

    /**
     * Tìm danh sách giao dịch đặt giá theo mã người đặt giá.
     *
     * @param bidderId mã định danh người đặt giá
     * @return danh sách giao dịch đặt giá của người dùng tương ứng
     */
    public List<BidTransaction> findByBidderId(final String bidderId) {
        validateText(bidderId, "Mã người đặt giá không được rỗng.");

        return findAll().stream()
            .filter(bid -> bid.getBidder() != null)
            .filter(bid -> bidderId.equals(bid.getBidder().getId()))
            .toList();
    }

    /**
     * Tìm danh sách giao dịch đặt giá trong khoảng thời gian.
     *
     * @param from thời điểm bắt đầu
     * @param to thời điểm kết thúc
     * @return danh sách giao dịch trong khoảng thời gian
     */
    public List<BidTransaction> findByTimeRange(
        final LocalDateTime from,
        final LocalDateTime to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");

        if (to.isBefore(from)) {
            throw new DatabaseException(
            "Thời điểm kết thúc không được trước thời điểm bắt đầu.");
        }

        return findAll().stream()
            .filter(bid -> !bid.getTimestamp().isBefore(from))
            .filter(bid -> !bid.getTimestamp().isAfter(to))
            .toList();
    }

    /**
     * Tìm danh sách giao dịch có số tiền đặt giá từ mức tối thiểu trở lên.
     *
     * @param minimumAmount số tiền tối thiểu
     * @return danh sách giao dịch thỏa điều kiện
     */
    public List<BidTransaction> findByMinimumAmount(
        final double minimumAmount) {
        validateNonNegativeAmount(minimumAmount);

        return findAll().stream()
            .filter(bid -> bid.getAmount() >= minimumAmount)
            .toList();
    }

    /**
     * Tìm giao dịch đặt giá cao nhất trong toàn bộ dữ liệu bid.
     *
     * <p>Hàm này chỉ tìm trên toàn bộ file bid transaction. Nếu muốn tìm bid cao
     * nhất của riêng một phiên đấu giá, model BidTransaction nên có thêm
     * auctionId.
     *
     * @return giao dịch đặt giá cao nhất nếu tồn tại
     */
    public Optional<BidTransaction> findHighestBid() {
        return findAll().stream()
            .max(Comparator.comparingDouble(BidTransaction::getAmount));
    }

    /**
     * Tìm giao dịch đặt giá mới nhất trong toàn bộ dữ liệu bid.
     *
     * @return giao dịch mới nhất nếu tồn tại
     */
    public Optional<BidTransaction> findLatestBid() {
        return findAll().stream()
            .max(Comparator.comparing(BidTransaction::getTimestamp));
    }

    /**
     * Kiểm tra dữ liệu giao dịch đặt giá trước khi lưu.
     *
     * @param bidTransaction giao dịch đặt giá cần kiểm tra
     */
    private void validateBidTransaction(
        final BidTransaction bidTransaction) {
        validateText(
            bidTransaction.getId(),
            "Mã giao dịch đặt giá không được rỗng.");

        validateText(
            bidTransaction.getAuctionId(),
            "Mã phiên đấu giá của giao dịch đặt giá không được rỗng.");

        if (bidTransaction.getBidder() == null) {
            throw new DatabaseException("Người đặt giá không được null.");
        }

        validateText(
            bidTransaction.getBidder().getId(),
            "Mã người đặt giá không được rỗng.");

        if (bidTransaction.getTimestamp() == null) {
            throw new DatabaseException("Thời gian đặt giá không được null.");
        }

        if (bidTransaction.getAmount() <= 0) {
            throw new DatabaseException("Số tiền đặt giá phải lớn hơn 0.");
        }
    }

    /**
     * Kiểm tra số tiền không được âm.
     *
     * @param amount số tiền cần kiểm tra
     */
    private void validateNonNegativeAmount(final double amount) {
        if (amount < 0) {
            throw new DatabaseException("Số tiền không được âm.");
        }
    }

    /**
     * Kiểm tra chuỗi không được null hoặc rỗng.
     *
     * @param value giá trị cần kiểm tra
     * @param message thông báo lỗi
     */
    private void validateText(final String value, final String message) {
        if (value == null || value.isBlank()) {
            throw new DatabaseException(message);
        }
    }
 
    /**
     * Tìm danh sách giao dịch đặt giá theo mã phiên đấu giá.
     *
     * @param auctionId mã định danh phiên đấu giá
     * @return danh sách giao dịch đặt giá thuộc phiên đấu giá đó
     */
    public List<BidTransaction> findByAuctionId(final String auctionId) {
        validateText(auctionId, "Mã phiên đấu giá không được rỗng.");

        return findAll().stream()
        .filter(bid -> auctionId.equals(bid.getAuctionId()))
        .toList();
    }

    /**
     * Tìm giao dịch đặt giá cao nhất của một phiên đấu giá.
     *
     * @param auctionId mã định danh phiên đấu giá
     * @return giao dịch đặt giá cao nhất nếu tồn tại
     */
    public Optional<BidTransaction> findHighestBidByAuctionId(
        final String auctionId) {
        validateText(auctionId, "Mã phiên đấu giá không được rỗng.");

        return findByAuctionId(auctionId).stream()
            .max(Comparator.comparingDouble(BidTransaction::getAmount));
    }

    /**
     * Tìm giao dịch đặt giá mới nhất của một phiên đấu giá.
     *
     * @param auctionId mã định danh phiên đấu giá
     * @return giao dịch đặt giá mới nhất nếu tồn tại
     */
    public Optional<BidTransaction> findLatestBidByAuctionId(
        final String auctionId) {
        validateText(auctionId, "Mã phiên đấu giá không được rỗng.");

        return findByAuctionId(auctionId).stream()
            .max(Comparator.comparing(BidTransaction::getTimestamp));
    }

    /**
     * Đếm số lượt đặt giá của một phiên đấu giá.
     *
     * @param auctionId mã định danh phiên đấu giá
     * @return số lượt đặt giá thuộc phiên đấu giá đó
     */
    public long countByAuctionId(final String auctionId) {
        validateText(auctionId, "Mã phiên đấu giá không được rỗng.");

        return findByAuctionId(auctionId).size();
    }

    /**
     * Xóa toàn bộ giao dịch đặt giá thuộc một phiên đấu giá.
     *
     * <p>Hàm này nên dùng cẩn thận, ví dụ khi xóa phiên đấu giá khỏi hệ thống và
     * muốn xóa luôn lịch sử bid liên quan.
     *
     * @param auctionId mã định danh phiên đấu giá
     * @return số giao dịch đã xóa
     */
    public int deleteByAuctionId(final String auctionId) {
        validateText(auctionId, "Mã phiên đấu giá không được rỗng.");

        List<BidTransaction> bids = findByAuctionId(auctionId);

        for (BidTransaction bid : bids) {
            deleteById(bid.getId());
        }

        return bids.size();
    }
     
}