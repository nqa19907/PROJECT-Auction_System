import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.items.Item;
import auction_system.common.models.items.builder.ElectronicBuilder;
import auction_system.common.models.users.Admin;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.server.persistence.exceptions.DatabaseException;
import auction_system.server.persistence.repositories.AuctionRepository;
import auction_system.server.persistence.repositories.BidTransactionRepository;
import auction_system.server.persistence.repositories.UserRepository;
import auction_system.server.persistence.serialization.SerializedDatabase;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử tích hợp cho ba repository: {@link AuctionRepository},
 * {@link UserRepository}, và {@link BidTransactionRepository}.
 *
 * <p>Các test thực hiện thao tác trực tiếp trên repository (không qua service
 * layer) để xác minh logic xác thực, query, và persistence tại tầng dữ liệu.
 * Mỗi test dùng {@code @TempDir} riêng, đảm bảo file {@code .ser} không bị
 * dùng chung giữa các lần chạy.
 *
 * <p>Cấu trúc mỗi test tuân theo <b>Arrange – Act – Assert</b>.
 */
class RepositoryTest {

    /** Thư mục tạm do JUnit 5 tạo và dọn dẹp tự động sau mỗi test. */
    @TempDir
    Path tempDir;

    /** Cơ sở dữ liệu serialization dùng chung trong phạm vi một test. */
    private SerializedDatabase database;

    /** Repository phiên đấu giá — đối tượng kiểm thử chính. */
    private AuctionRepository auctionRepo;

    /** Repository người dùng — đối tượng kiểm thử chính. */
    private UserRepository userRepo;

    /** Repository giao dịch đặt giá — đối tượng kiểm thử chính. */
    private BidTransactionRepository bidRepo;

    /** Người bán dùng làm người tổ chức phiên đấu giá trong các fixture. */
    private Participant seller;

    /** Sản phẩm hợp lệ tái sử dụng cho các fixture auction. */
    private Item sampleItem;

    /**
     * Khởi tạo môi trường test trước mỗi phương thức kiểm thử.
     *
     * <p>Tạo {@link SerializedDatabase} trỏ vào {@code tempDir} để cô lập
     * dữ liệu giữa các test, sau đó khởi tạo seller và sampleItem dùng chung.
     */
    @BeforeEach
    void setUp() {
        database = new SerializedDatabase(tempDir);
        auctionRepo = database.auctions();
        userRepo = database.users();
        bidRepo = database.bidTransactions();

        seller = new Participant(
                "seller01", "seller@mail.com", "pass", 0.0, "SELLER");

        sampleItem = new ElectronicBuilder()
                .itemName("MacBook")
                .description("Laptop")
                .startPrice(2000.0)
                .sellerId(seller.getId())
                .build();
    }

    // =========================================================================
    // Helper
    // =========================================================================

    /**
     * Tạo và lưu một {@link Auction} với trạng thái cho trước.
     *
     * <p>{@code startTime} đặt trong quá khứ để tránh vi phạm điều kiện
     * {@code endTime.isAfter(startTime)} của {@code validateAuction()}.
     *
     * @param status trạng thái muốn gán cho phiên đấu giá
     * @return phiên đấu giá đã được lưu vào repository
     */
    private Auction saveAuction(final AuctionStatus status) {
        Auction auction = new Auction(
                sampleItem,
                seller,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(2));
        auction.setStatus(status);
        return auctionRepo.save(auction);
    }

    /**
     * Tạo và lưu một {@link BidTransaction} với số tiền cho trước.
     *
     * @param auction phiên đấu giá đích
     * @param bidder  người đặt giá
     * @param amount  số tiền đặt giá
     * @return giao dịch đã được lưu vào repository
     */
    private BidTransaction saveBid(
            final Auction auction,
            final Participant bidder,
            final double amount) {
        BidTransaction bid = new BidTransaction(bidder, amount, auction);
        return bidRepo.save(bid);
    }

    // =========================================================================
    // AuctionRepository — save() validation
    // =========================================================================

    @Test
    void auction_Save_NullAuction_ThrowsNullPointerException() {
        // null phải bị chặn ngay tại Objects.requireNonNull(auction)
        assertThrows(NullPointerException.class, () -> auctionRepo.save(null));
    }

    @Test
    void auction_Save_NullItem_ThrowsDatabaseException() {
        // item == null vi phạm validateAuction()
        Auction auction = new Auction(
                null, seller,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(2));
        assertThrows(DatabaseException.class, () -> auctionRepo.save(auction));
    }

    @Test
    void auction_Save_EndTimeBeforeStartTime_ThrowsDatabaseException() {
        // endTime <= startTime vi phạm điều kiện thời gian trong validateAuction()
        Auction auction = new Auction(
                sampleItem, seller,
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().minusHours(1));
        assertThrows(DatabaseException.class, () -> auctionRepo.save(auction));
    }

    @Test
    void auction_Save_ValidAuction_CanBeFoundById() {
        // Phiên hợp lệ phải tìm lại được bằng findById()
        Auction saved = saveAuction(AuctionStatus.OPEN);

        Optional<Auction> found = auctionRepo.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    // =========================================================================
    // AuctionRepository — query methods
    // =========================================================================

    @Test
    void auction_FindByStatus_ReturnsOnlyMatchingStatus() {
        // Ba phiên khác trạng thái; chỉ OPEN phải được trả về
        saveAuction(AuctionStatus.OPEN);
        saveAuction(AuctionStatus.RUNNING);
        saveAuction(AuctionStatus.FINISHED);

        List<Auction> result = auctionRepo.findByStatus(AuctionStatus.OPEN);

        assertEquals(1, result.size());
        assertEquals(AuctionStatus.OPEN, result.get(0).getStatus());
    }

    @Test
    void auction_FindByStatus_NullStatus_ThrowsNullPointerException() {
        // null phải bị chặn bởi Objects.requireNonNull(status)
        assertThrows(
                NullPointerException.class,
                () -> auctionRepo.findByStatus(null));
    }

    @Test
    void auction_FindAvailable_ReturnsOpenAndRunningOnly() {
        // FINISHED và CANCELED không được lẫn vào kết quả
        saveAuction(AuctionStatus.OPEN);
        saveAuction(AuctionStatus.RUNNING);
        saveAuction(AuctionStatus.FINISHED);
        saveAuction(AuctionStatus.CANCELED);

        List<Auction> result = auctionRepo.findAvailableAuctions();

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(a ->
                a.getStatus() == AuctionStatus.OPEN
                        || a.getStatus() == AuctionStatus.RUNNING));
    }

    @Test
    void auction_FindBySellerId_ReturnsOnlySellerAuctions() {
        // Phiên của seller khác không được lẫn vào kết quả
        Participant otherSeller = new Participant(
                "other01", "other@mail.com", "pass", 0.0, "SELLER");
        Item otherItem = new ElectronicBuilder()
                .itemName("iPad").description("Tablet")
                .startPrice(1000.0).sellerId(otherSeller.getId()).build();

        saveAuction(AuctionStatus.OPEN);

        Auction otherAuction = new Auction(
                otherItem, otherSeller,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(2));
        otherAuction.setStatus(AuctionStatus.OPEN);
        auctionRepo.save(otherAuction);

        List<Auction> result = auctionRepo.findBySellerId(seller.getId());

        assertEquals(1, result.size());
    }

    @Test
    void auction_FindBySellerId_NullId_ThrowsDatabaseException() {
        // null bị chặn bởi validateText() trong findBySellerId()
        assertThrows(
                DatabaseException.class,
                () -> auctionRepo.findBySellerId(null));
    }

    @Test
    void auction_FindByItemId_ReturnsAuctionsForItem() {
        // Chỉ phiên gắn với sampleItem mới được trả về
        Auction saved = saveAuction(AuctionStatus.OPEN);

        List<Auction> result = auctionRepo.findByItemId(sampleItem.getId());

        assertEquals(1, result.size());
        assertEquals(saved.getId(), result.get(0).getId());
    }

    @Test
    void auction_FindReadyToStart_ReturnsOpenAuctionsPastStartTime() {
        // startTime trong quá khứ → phiên OPEN đủ điều kiện bắt đầu
        saveAuction(AuctionStatus.OPEN);
        // Phiên RUNNING không được xuất hiện trong danh sách
        saveAuction(AuctionStatus.RUNNING);

        List<Auction> result =
                auctionRepo.findAuctionsReadyToStart(LocalDateTime.now());

        assertEquals(1, result.size());
        assertEquals(AuctionStatus.OPEN, result.get(0).getStatus());
    }

    @Test
    void auction_FindReadyToEnd_ReturnsRunningAuctionsPastEndTime() {
        // Phiên RUNNING với endTime trong quá khứ phải được trả về
        Auction pastEnd = new Auction(
                sampleItem, seller,
                LocalDateTime.now().minusHours(3),
                LocalDateTime.now().minusMinutes(1));
        pastEnd.setStatus(AuctionStatus.RUNNING);
        auctionRepo.save(pastEnd);

        // Phiên RUNNING chưa tới giờ kết thúc không được trả về
        saveAuction(AuctionStatus.RUNNING);

        List<Auction> result =
                auctionRepo.findAuctionsReadyToEnd(LocalDateTime.now());

        assertEquals(1, result.size());
        assertEquals(pastEnd.getId(), result.get(0).getId());
    }

    @Test
    void auction_UpdateStatus_ChangesStatusAndPersists() {
        // Trạng thái mới phải được phản ánh cả trong bộ nhớ lẫn khi findById()
        Auction saved = saveAuction(AuctionStatus.OPEN);

        Auction updated =
                auctionRepo.updateStatus(saved.getId(), AuctionStatus.RUNNING);

        assertEquals(AuctionStatus.RUNNING, updated.getStatus());
        assertEquals(
                AuctionStatus.RUNNING,
                auctionRepo.findById(saved.getId()).get().getStatus());
    }

    @Test
    void auction_UpdateStatus_NonExistentId_ThrowsDatabaseException() {
        // Id không tồn tại phải ném DatabaseException từ orElseThrow()
        assertThrows(
                DatabaseException.class,
                () -> auctionRepo.updateStatus("bad-id", AuctionStatus.RUNNING));
    }

    // =========================================================================
    // UserRepository — save() validation
    // =========================================================================

    @Test
    void user_Save_NullUser_ThrowsNullPointerException() {
        // null phải bị chặn bởi Objects.requireNonNull(user)
        assertThrows(NullPointerException.class, () -> userRepo.save(null));
    }

    @Test
    void user_Save_DuplicateUsername_ThrowsDatabaseException() {
        // Trùng username (khác id) bị chặn bởi validateUniqueUsername()
        userRepo.save(
                new Participant("user01", "a@mail.com", "pass", 0.0, "BIDDER"));

        Participant dup =
                new Participant("user01", "b@mail.com", "pass", 0.0, "BIDDER");
        assertThrows(DatabaseException.class, () -> userRepo.save(dup));
    }

    @Test
    void user_Save_DuplicateEmail_ThrowsDatabaseException() {
        // Trùng email (khác id) bị chặn bởi validateUniqueEmail()
        userRepo.save(
                new Participant("userA", "dup@mail.com", "pass", 0.0, "BIDDER"));

        Participant dup =
                new Participant("userB", "dup@mail.com", "pass", 0.0, "BIDDER");
        assertThrows(DatabaseException.class, () -> userRepo.save(dup));
    }

    @Test
    void user_Save_UpdateExistingUser_DoesNotThrow() {
        // Cập nhật cùng id không bị chặn bởi kiểm tra trùng lặp
        Participant user =
                new Participant("user01", "u@mail.com", "pass", 0.0, "BIDDER");
        userRepo.save(user);

        user.setPassword("newpass");
        assertDoesNotThrow(() -> userRepo.save(user));
    }

    // =========================================================================
    // UserRepository — query methods
    // =========================================================================

    @Test
    void user_FindByUsername_CaseInsensitive_ReturnsUser() {
        // "USER01" phải khớp với "user01" nhờ equalsIgnoreCase()
        userRepo.save(
                new Participant("user01", "u@mail.com", "pass", 0.0, "BIDDER"));

        Optional<User> found = userRepo.findByUsername("USER01");

        assertTrue(found.isPresent());
        assertEquals("user01", found.get().getUsername());
    }

    @Test
    void user_FindByUsername_Null_ThrowsDatabaseException() {
        // null bị chặn bởi validateText() trong findByUsername()
        assertThrows(
                DatabaseException.class,
                () -> userRepo.findByUsername(null));
    }

    @Test
    void user_FindByEmail_CaseInsensitive_ReturnsUser() {
        // So sánh email không phân biệt hoa thường
        userRepo.save(
                new Participant(
                        "user02", "User@Mail.com", "pass", 0.0, "BIDDER"));

        Optional<User> found = userRepo.findByEmail("user@mail.com");

        assertTrue(found.isPresent());
    }

    @Test
    void user_FindByRoleName_ReturnsUsersWithMatchingRole() {
        // findByRoleName phải so sánh case-insensitive; "bidder" khớp "BIDDER"
        userRepo.save(
                new Participant("b1", "b1@mail.com", "pass", 0.0, "BIDDER"));
        userRepo.save(
                new Participant("b2", "b2@mail.com", "pass", 0.0, "BIDDER"));
        userRepo.save(new Admin("admin01", "admin@mail.com", "pass"));

        List<User> bidders = userRepo.findByRoleName("bidder");

        assertEquals(2, bidders.size());
    }

    @Test
    void user_ExistsByUsername_ExistingUser_ReturnsTrue() {
        // existsByUsername phải trả về true khi username đã có
        userRepo.save(
                new Participant(
                        "existUser", "e@mail.com", "pass", 0.0, "SELLER"));

        assertTrue(userRepo.existsByUsername("existUser"));
    }

    @Test
    void user_ExistsByEmail_NewEmail_ReturnsFalse() {
        // existsByEmail phải trả về false khi email chưa tồn tại
        assertFalse(userRepo.existsByEmail("new@mail.com"));
    }

    // =========================================================================
    // BidTransactionRepository — save() validation
    // =========================================================================

    @Test
    void bid_Save_NullBid_ThrowsNullPointerException() {
        // null phải bị chặn bởi Objects.requireNonNull(bidTransaction)
        assertThrows(NullPointerException.class, () -> bidRepo.save(null));
    }

    @Test
    void bid_Save_ZeroAmount_ThrowsDatabaseException() {
        // amount == 0 vi phạm điều kiện amount > 0 trong validateBidTransaction()
        Auction auction = saveAuction(AuctionStatus.RUNNING);
        Participant bidder = new Participant(
                "bid01", "bid01@mail.com", "pass", 10_000.0, "BIDDER");

        BidTransaction zeroBid = new BidTransaction(bidder, 0.0, auction);
        assertThrows(DatabaseException.class, () -> bidRepo.save(zeroBid));
    }

    @Test
    void bid_Save_NullParticipant_ThrowsDatabaseException() {
        // participant == null bị chặn bởi validateBidTransaction()
        Auction auction = saveAuction(AuctionStatus.RUNNING);
        BidTransaction bid = new BidTransaction(null, 500.0, auction);
        assertThrows(DatabaseException.class, () -> bidRepo.save(bid));
    }

    // =========================================================================
    // BidTransactionRepository — query methods
    // =========================================================================

    @Test
    void bid_FindByBidderId_ReturnsOnlyBidderBids() {
        // 2 bid của bidder1, 1 bid của bidder2; kết quả chỉ chứa bid của bidder1
        Auction auction = saveAuction(AuctionStatus.RUNNING);
        Participant bidder1 = new Participant(
                "bd1", "bd1@mail.com", "pass", 100_000.0, "BIDDER");
        Participant bidder2 = new Participant(
                "bd2", "bd2@mail.com", "pass", 100_000.0, "BIDDER");

        saveBid(auction, bidder1, 2500.0);
        saveBid(auction, bidder1, 3000.0);
        saveBid(auction, bidder2, 4000.0);

        List<BidTransaction> result =
                bidRepo.findByBidderId(bidder1.getId());

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(
                b -> bidder1.getId().equals(b.getParticipant().getId())));
    }

    @Test
    void bid_FindByBidderId_NullId_ThrowsDatabaseException() {
        // null bị chặn bởi validateText() trong findByBidderId()
        assertThrows(
                DatabaseException.class,
                () -> bidRepo.findByBidderId(null));
    }

    @Test
    void bid_FindByAuctionId_ReturnsOnlyAuctionBids() {
        // Bid của auction2 không được lẫn vào kết quả truy vấn auction1
        Auction auction1 = saveAuction(AuctionStatus.RUNNING);
        Item item2 = new ElectronicBuilder()
                .itemName("iPhone").description("Phone")
                .startPrice(1000.0).sellerId(seller.getId()).build();
        Auction auction2 = new Auction(
                item2, seller,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(2));
        auction2.setStatus(AuctionStatus.RUNNING);
        auctionRepo.save(auction2);

        Participant bidder = new Participant(
                "bd3", "bd3@mail.com", "pass", 100_000.0, "BIDDER");
        saveBid(auction1, bidder, 2500.0);
        saveBid(auction2, bidder, 1500.0);

        List<BidTransaction> result =
                bidRepo.findByAuctionId(auction1.getId());

        assertEquals(1, result.size());
        assertEquals(auction1.getId(), result.get(0).getAuctionId());
    }

    @Test
    void bid_FindByTimeRange_ToBeforeFrom_ThrowsDatabaseException() {
        // to < from phải ném DatabaseException
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.minusHours(1);

        assertThrows(
                DatabaseException.class,
                () -> bidRepo.findByTimeRange(from, to));
    }

    @Test
    void bid_FindByTimeRange_ReturnsOnlyBidsWithinRange() {
        // Bid vừa tạo phải nằm trong khoảng [now-5s, now+5s]
        Auction auction = saveAuction(AuctionStatus.RUNNING);
        Participant bidder = new Participant(
                "bd4", "bd4@mail.com", "pass", 100_000.0, "BIDDER");

        saveBid(auction, bidder, 2500.0);

        LocalDateTime from = LocalDateTime.now().minusSeconds(5);
        LocalDateTime to = LocalDateTime.now().plusSeconds(5);

        List<BidTransaction> result = bidRepo.findByTimeRange(from, to);

        assertEquals(1, result.size());
    }

    @Test
    void bid_FindByMinimumAmount_NegativeAmount_ThrowsDatabaseException() {
        // Số tiền âm bị chặn bởi validateNonNegativeAmount()
        assertThrows(
                DatabaseException.class,
                () -> bidRepo.findByMinimumAmount(-1.0));
    }

    @Test
    void bid_FindByMinimumAmount_Zero_ReturnsAllBids() {
        // minimumAmount == 0 hợp lệ và phải trả về toàn bộ bid
        Auction auction = saveAuction(AuctionStatus.RUNNING);
        Participant bidder = new Participant(
                "bd5", "bd5@mail.com", "pass", 100_000.0, "BIDDER");
        saveBid(auction, bidder, 2500.0);
        saveBid(auction, bidder, 5000.0);

        List<BidTransaction> result = bidRepo.findByMinimumAmount(0.0);

        assertEquals(2, result.size());
    }

    @Test
    void bid_FindHighestBid_ReturnsMaxAmountBid() {
        // Trong ba bid, bid 7000.0 phải được chọn là cao nhất
        Auction auction = saveAuction(AuctionStatus.RUNNING);
        Participant bidder = new Participant(
                "bd6", "bd6@mail.com", "pass", 100_000.0, "BIDDER");
        saveBid(auction, bidder, 2500.0);
        saveBid(auction, bidder, 7000.0);
        saveBid(auction, bidder, 4000.0);

        Optional<BidTransaction> highest = bidRepo.findHighestBid();

        assertTrue(highest.isPresent());
        assertEquals(7000.0, highest.get().getAmount(), 0.001);
    }

    @Test
    void bid_FindHighestBid_EmptyRepository_ReturnsEmpty() {
        // Repository rỗng phải trả về Optional.empty()
        assertTrue(bidRepo.findHighestBid().isEmpty());
    }

    @Test
    void bid_FindHighestBidByAuctionId_ReturnsMaxForThatAuction() {
        // Trong hai bid, bid 6000.0 phải là cao nhất của phiên đó
        Auction auction = saveAuction(AuctionStatus.RUNNING);
        Participant bidder = new Participant(
                "bd7", "bd7@mail.com", "pass", 100_000.0, "BIDDER");
        saveBid(auction, bidder, 3000.0);
        saveBid(auction, bidder, 6000.0);

        Optional<BidTransaction> highest =
                bidRepo.findHighestBidByAuctionId(auction.getId());

        assertTrue(highest.isPresent());
        assertEquals(6000.0, highest.get().getAmount(), 0.001);
    }

    @Test
    void bid_CountByAuctionId_ReturnsCorrectCount() {
        // Hai bid được lưu; countByAuctionId phải trả về đúng 2
        Auction auction = saveAuction(AuctionStatus.RUNNING);
        Participant bidder = new Participant(
                "bd8", "bd8@mail.com", "pass", 100_000.0, "BIDDER");
        saveBid(auction, bidder, 2500.0);
        saveBid(auction, bidder, 3500.0);

        assertEquals(2L, bidRepo.countByAuctionId(auction.getId()));
    }

    @Test
    void bid_DeleteByAuctionId_RemovesAllBidsForAuction() {
        // Xóa auction1 không ảnh hưởng bid của auction2
        Auction auction1 = saveAuction(AuctionStatus.RUNNING);
        Item item2 = new ElectronicBuilder()
                .itemName("Watch").description("Đồng hồ")
                .startPrice(500.0).sellerId(seller.getId()).build();
        Auction auction2 = new Auction(
                item2, seller,
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(2));
        auction2.setStatus(AuctionStatus.RUNNING);
        auctionRepo.save(auction2);

        Participant bidder = new Participant(
                "bd9", "bd9@mail.com", "pass", 100_000.0, "BIDDER");
        saveBid(auction1, bidder, 2500.0);
        saveBid(auction1, bidder, 3000.0);
        saveBid(auction2, bidder, 800.0);

        int deleted = bidRepo.deleteByAuctionId(auction1.getId());

        assertEquals(2, deleted);
        assertTrue(bidRepo.findByAuctionId(auction1.getId()).isEmpty());
        assertEquals(1, bidRepo.findByAuctionId(auction2.getId()).size());
    }
}