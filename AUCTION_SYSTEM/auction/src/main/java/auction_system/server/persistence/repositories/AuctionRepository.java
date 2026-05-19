package auction_system.server.persistence.repositories;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.server.persistence.exceptions.DatabaseException;
import auction_system.server.persistence.serialization.SerializedFileStorage;
import auction_system.server.persistence.serialization.SerializedRepository;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Repository quản lý dữ liệu phiên đấu giá trong hệ thống.
 *
 * <p>Lớp này lưu toàn bộ phiên đấu giá vào file {@code auctions.ser} thông qua
 * Java Serialization. Repository chỉ xử lý dữ liệu bền vững, không trực tiếp xử
 * lý Socket, observer realtime hoặc logic đặt giá.
 */
public class AuctionRepository extends SerializedRepository<Auction> {

    /**
     * Khởi tạo repository phiên đấu giá với đường dẫn file lưu trữ.
     *
     * @param storagePath đường dẫn tới file auctions.ser
     */
    public AuctionRepository(final Path storagePath) {
        super(new SerializedFileStorage<>(storagePath), Auction::getId);
    }

    /**
     * Lưu mới hoặc cập nhật phiên đấu giá.
     *
     * @param auction phiên đấu giá cần lưu
     * @return phiên đấu giá đã được lưu
     */
    @Override
    public synchronized Auction save(final Auction auction) {
        Objects.requireNonNull(auction, "auction");

        validateAuction(auction);

        return super.save(auction);
    }

    /**
     * Tìm danh sách phiên đấu giá theo trạng thái.
     *
     * @param status trạng thái phiên đấu giá cần tìm
     * @return danh sách phiên đấu giá có trạng thái tương ứng
     */
    public List<Auction> findByStatus(final AuctionStatus status) {
        Objects.requireNonNull(status, "status");

        return findAll().stream()
            .filter(auction -> status == auction.getStatus())
            .toList();
    }

    /**
     * Tìm các phiên đấu giá đang mở hoặc đang chạy.
     *
     * @return danh sách phiên đấu giá có thể hiển thị cho người dùng tham gia
     */
    public List<Auction> findAvailableAuctions() {
        return findAll().stream()
            .filter(auction -> auction.getStatus() == AuctionStatus.OPEN
                || auction.getStatus() == AuctionStatus.RUNNING)
            .toList();
    }

    /**
     * Tìm danh sách phiên đấu giá theo mã người bán.
     *
     * <p>Ưu tiên lấy sellerId trực tiếp nếu auction có lưu sellerId. Nếu sellerId
     * chưa được set, repository sẽ lấy id từ đối tượng seller.
     *
     * @param sellerId mã định danh người bán
     * @return danh sách phiên đấu giá thuộc người bán đó
     */
    public List<Auction> findBySellerId(final String sellerId) {
        validateText(sellerId, "Mã người bán không được rỗng.");

        return findAll().stream()
            .filter(auction -> sellerId.equals(resolveSellerId(auction)))
            .toList();
    }

    /**
     * Tìm danh sách phiên đấu giá theo mã sản phẩm.
     *
     * @param itemId mã định danh sản phẩm
     * @return danh sách phiên đấu giá của sản phẩm tương ứng
     */
    public List<Auction> findByItemId(final String itemId) {
        validateText(itemId, "Mã sản phẩm không được rỗng.");

        return findAll().stream()
            .filter(auction -> auction.getItem() != null)
            .filter(auction -> itemId.equals(auction.getItem().getId()))
            .toList();
    }

    /**
     * Tìm các phiên đấu giá cần bắt đầu tại thời điểm hiện tại.
     *
     * @param now thời điểm kiểm tra
     * @return danh sách phiên đấu giá đang OPEN và đã tới giờ bắt đầu
     */
    public List<Auction> findAuctionsReadyToStart(final LocalDateTime now) {
        Objects.requireNonNull(now, "now");

        return findAll().stream()
            .filter(auction -> auction.getStatus() == AuctionStatus.OPEN)
            .filter(auction -> !now.isBefore(auction.getStartTime()))
            .toList();
    }

    /**
     * Tìm các phiên đấu giá cần kết thúc tại thời điểm hiện tại.
     *
     * @param now thời điểm kiểm tra
     * @return danh sách phiên đấu giá đang RUNNING và đã tới giờ kết thúc
     */
    public List<Auction> findAuctionsReadyToEnd(final LocalDateTime now) {
        Objects.requireNonNull(now, "now");

        return findAll().stream()
            .filter(auction -> auction.getStatus() == AuctionStatus.RUNNING)
            .filter(auction -> !now.isBefore(auction.getEndTime()))
            .toList();
    }

    /**
     * Cập nhật trạng thái của một phiên đấu giá.
     *
     * @param auctionId mã định danh phiên đấu giá
     * @param status trạng thái mới
     * @return phiên đấu giá sau khi cập nhật
     */
    public Auction updateStatus(
        final String auctionId,
        final AuctionStatus status) {
        validateText(auctionId, "Mã phiên đấu giá không được rỗng.");
        Objects.requireNonNull(status, "status");

        Auction auction = findById(auctionId)
            .orElseThrow(() -> new DatabaseException(
                "Không tìm thấy phiên đấu giá: " + auctionId));

        auction.setStatus(status);
        return save(auction);
    }

    /**
     * Kiểm tra dữ liệu phiên đấu giá trước khi lưu.
     *
     * @param auction phiên đấu giá cần kiểm tra
     */
    private void validateAuction(final Auction auction) {
        validateText(auction.getId(), "Mã phiên đấu giá không được rỗng.");

        if (auction.getItem() == null) {
        throw new DatabaseException("Sản phẩm đấu giá không được null.");
        }

        if (auction.getSeller() == null && isBlank(auction.getSellerId())) {
        throw new DatabaseException("Người bán của phiên đấu giá không hợp lệ.");
        }

        if (auction.getStartTime() == null) {
        throw new DatabaseException("Thời gian bắt đầu không được null.");
        }

        if (auction.getEndTime() == null) {
        throw new DatabaseException("Thời gian kết thúc không được null.");
        }

        if (!auction.getEndTime().isAfter(auction.getStartTime())) {
        throw new DatabaseException(
            "Thời gian kết thúc phải sau thời gian bắt đầu.");
        }

        if (auction.getStatus() == null) {
        throw new DatabaseException("Trạng thái phiên đấu giá không được null.");
        }
    }

    /**
     * Lấy mã người bán từ phiên đấu giá.
     *
     * @param auction phiên đấu giá cần lấy mã người bán
     * @return mã người bán nếu có
     */
    private String resolveSellerId(final Auction auction) {
        if (!isBlank(auction.getSellerId())) {
        return auction.getSellerId();
        }

        if (auction.getSeller() == null) {
        return null;
        }

        return auction.getSeller().getId();
    }

    /**
     * Kiểm tra chuỗi không được null hoặc rỗng.
     *
     * @param value giá trị cần kiểm tra
     * @param message thông báo lỗi
     */
    private void validateText(final String value, final String message) {
        if (isBlank(value)) {
        throw new DatabaseException(message);
        }
    }

    /**
     * Kiểm tra chuỗi có rỗng hay không.
     *
     * @param value giá trị cần kiểm tra
     * @return true nếu chuỗi null hoặc rỗng
     */
    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}