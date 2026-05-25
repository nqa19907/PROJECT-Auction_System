import static org.junit.jupiter.api.Assertions.*;

import auction_system.common.exceptions.AuctionClosedException;
import auction_system.common.exceptions.InvalidBidException;
import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.items.Item;
import auction_system.common.models.items.builder.ElectronicBuilder;
import auction_system.common.models.users.Admin;
import auction_system.common.models.users.Participant;
import auction_system.server.core.AuctionManager;
import auction_system.server.persistence.exceptions.DatabaseException;
import auction_system.server.persistence.serialization.SerializedDatabase;
import auction_system.server.services.AuctionBidService;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử tích hợp cho {@link AuctionBidService} và lớp {@link Auction}.
 *
 * <p>Lớp kiểm thử này bao gồm hai nhóm chính:
 * <ol>
 *   <li><b>Auction.placeBid</b> — trạng thái CANCELED chưa được
 *       {@code CoreBiddingLogicTest} kiểm thử tường minh.</li>
 *   <li><b>AuctionBidService.placeBid</b> — toàn bộ luồng xác thực và nghiệp
 *       vụ tại service layer: kiểm tra tham số đầu vào, trừ/hoàn số dư,
 *       ghi database, và an toàn luồng (thread-safety).</li>
 * </ol>
 *
 * <p>Mỗi test tuân theo cấu trúc <b>Arrange – Act – Assert</b>.
 * {@code @TempDir} tạo thư mục tạm riêng biệt cho mỗi test, đảm bảo các file
 * serialization không bị dùng chung giữa các lần chạy.
 */
class BidServiceTest {

    /** Thư mục tạm do JUnit 5 tạo và dọn dẹp tự động sau mỗi test. */
    @TempDir
    Path tempDir;

    /** Cơ sở dữ liệu serialization dùng chung trong phạm vi một test. */
    private SerializedDatabase database;

    /** Service đặt giá — đối tượng chính cần kiểm thử. */
    private AuctionBidService bidService;

    /** Người bán tổ chức phiên đấu giá. */
    private Participant seller;

    /** Người mua thực hiện các lượt đặt giá trong test. */
    private Participant bidder;

    /** Phiên đấu giá đang ở trạng thái {@code RUNNING}, sẵn sàng nhận bid. */
    private Auction runningAuction;

    /**
     * Giá khởi điểm dùng chung cho tất cả các test.
     *
     * <p>Khai báo là hằng số để các test dẫn xuất mức giá hợp lệ/không hợp lệ
     * từ đây thay vì dùng literal number rải rác.
     */
    private static final double START_PRICE = 2000.0;

    /**
     * Khởi tạo môi trường test trước mỗi phương thức kiểm thử.
     *
     * <p>Chuỗi khởi tạo:
     * <ol>
     *   <li>Tạo {@code SerializedDatabase} trỏ vào {@code tempDir} — cô lập
     *       dữ liệu giữa các test.</li>
     *   <li>Tạo {@code seller} và {@code bidder} với số dư đủ để đặt giá.</li>
     *   <li>Xây dựng {@code Item} qua {@code ElectronicBuilder}; {@code sellerId}
     *       lấy từ {@code seller.getId()} để khớp với đối tượng thực.</li>
     *   <li>Tạo {@code Auction} với {@code startTime} trong quá khứ 1 phút để
     *       {@code startAuction()} chuyển trạng thái sang {@code RUNNING} ngay
     *       lập tức — điều kiện bên trong là
     *       {@code !LocalDateTime.now().isBefore(startTime)}.</li>
     *   <li>Xác nhận tiền điều kiện: phiên phải ở {@code RUNNING} trước khi
     *       bất kỳ test nào chạy.</li>
     *   <li>Lưu phiên vào database để {@code findAuctionOrThrow()} tìm thấy
     *       được trong các test qua service layer.</li>
     * </ol>
     */
    @BeforeEach
    void setUp() {
        database   = new SerializedDatabase(tempDir);
        bidService = new AuctionBidService(database, AuctionManager.getInstance(database));

        seller = new Participant("seller01", "seller@mail.com", "pass", 0.0, "PARTICIPANT");
        bidder = new Participant("bidder01", "bidder@mail.com", "pass", 100_000.0, "PARTICIPANT");

        Item item = new ElectronicBuilder()
                .itemName("MacBook")
                .startPrice(START_PRICE)
                .sellerId(seller.getId())
                .build();

        runningAuction = new Auction(item, seller,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(2));
        runningAuction.startAuction();

        assertEquals(AuctionStatus.RUNNING, runningAuction.getStatus(),
                "Tiền điều kiện: runningAuction phải ở trạng thái RUNNING");

        database.auctions().save(runningAuction);
    }

    // =========================================================================
    // Nhóm 1: Auction.placeBid — trạng thái CANCELED
    // =========================================================================

    /**
     * Đặt giá vào phiên đấu giá đã bị hủy phải ném {@link AuctionClosedException}.
     *
     * <p>{@code CoreBiddingLogicTest} đã kiểm thử các trạng thái {@code OPEN}
     * và {@code FINISHED}. Test này bổ sung nhánh {@code CANCELED} — cùng điều
     * kiện {@code status != RUNNING} trong {@code Auction.placeBid()} nhưng
     * chưa được kiểm thử tường minh.
     */
    @Test
    void placeBid_AuctionCanceled_ThrowsAuctionClosedException() {
        runningAuction.setStatus(AuctionStatus.CANCELED);

        BidTransaction bid = new BidTransaction(bidder, START_PRICE + 1000.0, runningAuction);
        assertThrows(AuctionClosedException.class,
                () -> runningAuction.placeBid(bid),
                "Phiên CANCELED phải ném AuctionClosedException");
    }

    // =========================================================================
    // Nhóm 2: AuctionBidService.validateRequest — xác thực tham số đầu vào
    // =========================================================================

    /**
     * Mã phiên đấu giá {@code null} phải bị từ chối ngay tại service layer.
     *
     * <p>{@code validateRequest()} kiểm tra {@code auctionId == null ||
     * auctionId.isBlank()} trước khi truy vấn database, tránh NullPointerException
     * lan xuống các tầng phía dưới.
     */
    @Test
    void placeBid_NullAuctionId_ThrowsInvalidBidException() {
        assertThrows(InvalidBidException.class,
                () -> bidService.placeBid(null, bidder, START_PRICE + 1000.0));
    }

    /**
     * Mã phiên đấu giá chỉ chứa khoảng trắng phải bị từ chối như {@code null}.
     *
     * <p>Điều kiện {@code isBlank()} trong {@code validateRequest()} bắt cả
     * chuỗi rỗng và chuỗi chỉ có whitespace.
     */
    @Test
    void placeBid_BlankAuctionId_ThrowsInvalidBidException() {
        assertThrows(InvalidBidException.class,
                () -> bidService.placeBid("   ", bidder, START_PRICE + 1000.0));
    }

    /**
     * Người dùng {@code null} (chưa đăng nhập) phải bị từ chối đặt giá.
     *
     * <p>{@code validateRequest()} kiểm tra {@code currentUser == null} để đảm
     * bảo mọi lượt đặt giá đều có danh tính người dùng hợp lệ.
     */
    @Test
    void placeBid_NullUser_ThrowsInvalidBidException() {
        assertThrows(InvalidBidException.class,
                () -> bidService.placeBid(runningAuction.getId(), null, START_PRICE + 1000.0));
    }

    /**
     * Tài khoản {@link Admin} không được phép đặt giá.
     *
     * <p>{@code validateRequest()} kiểm tra {@code instanceof Participant}; Admin
     * kế thừa {@code User} nhưng không phải {@code Participant}, nên phải bị
     * từ chối với {@link InvalidBidException}.
     */
    @Test
    void placeBid_AdminUser_ThrowsInvalidBidException() {
        Admin admin = new Admin("admin01", "admin@mail.com", "pass");
        assertThrows(InvalidBidException.class,
                () -> bidService.placeBid(runningAuction.getId(), admin, START_PRICE + 1000.0));
    }

    /**
     * Số tiền đặt giá bằng 0 phải bị từ chối.
     *
     * <p>Điều kiện {@code amount <= 0} trong {@code validateRequest()} ngăn các
     * lượt đặt giá vô nghĩa trước khi chạm vào logic nghiệp vụ bên trong
     * {@code Auction.placeBid()}.
     */
    @Test
    void placeBid_ZeroAmount_ThrowsInvalidBidException() {
        assertThrows(InvalidBidException.class,
                () -> bidService.placeBid(runningAuction.getId(), bidder, 0.0));
    }

    /**
     * Số tiền đặt giá âm phải bị từ chối.
     *
     * <p>Cùng nhánh điều kiện với {@code amount == 0}; test tường minh giá trị
     * âm để phân biệt rõ biên kiểm tra.
     */
    @Test
    void placeBid_NegativeAmount_ThrowsInvalidBidException() {
        assertThrows(InvalidBidException.class,
                () -> bidService.placeBid(runningAuction.getId(), bidder, -500.0));
    }

    /**
     * Số tiền đặt giá vượt số dư tài khoản phải bị từ chối.
     *
     * <p>{@code validateAvailableBalance()} so sánh {@code amount} với
     * {@code bidder.getBalance()} (100 000); đặt 200 000 phải ném
     * {@link InvalidBidException}.
     */
    @Test
    void placeBid_InsufficientBalance_ThrowsInvalidBidException() {
        assertThrows(InvalidBidException.class,
                () -> bidService.placeBid(runningAuction.getId(), bidder, 200_000.0));
    }

    /**
     * Mã phiên đấu giá không tồn tại trong database phải ném {@link DatabaseException}.
     *
     * <p>{@code findAuctionOrThrow()} gọi {@code orElseThrow()} và ném
     * {@code DatabaseException} khi không tìm thấy bản ghi; lỗi này khác với
     * {@code InvalidBidException} vì nguyên nhân là thiếu dữ liệu, không phải
     * tham số sai.
     */
    @Test
    void placeBid_AuctionIdNotFound_ThrowsDatabaseException() {
        assertThrows(DatabaseException.class,
                () -> bidService.placeBid("non-existent-id", bidder, START_PRICE + 1000.0));
    }

    // =========================================================================
    // Nhóm 3: Happy-path — đặt giá hợp lệ qua service layer
    // =========================================================================

    /**
     * Đặt giá hợp lệ phải trả về {@link BidTransaction} với đúng số tiền và
     * người đặt giá.
     *
     * <p>Xác nhận rằng service tạo và trả về đối tượng {@code BidTransaction}
     * đúng — không phải một proxy hay bản sao khác.
     */
    @Test
    void placeBid_ValidRequest_ReturnsSavedBidTransaction() {
        double validAmount = START_PRICE + 1000.0;
        BidTransaction result = bidService.placeBid(runningAuction.getId(), bidder, validAmount);

        assertNotNull(result);
        assertEquals(validAmount, result.getAmount(), 0.001);
        assertSame(bidder, result.getParticipant());
    }

    /**
     * Sau khi đặt giá thành công, {@link BidTransaction} phải được ghi
     * xuống {@link SerializedDatabase}.
     *
     * <p>Kiểm tra tầng persistence: gọi {@code findAll()} để xác nhận bản ghi
     * thực sự tồn tại trong repository, không chỉ nằm trong bộ nhớ.
     */
    @Test
    void placeBid_ValidRequest_PersistsBidTransactionToDatabase() {
        double validAmount = START_PRICE + 1000.0;
        bidService.placeBid(runningAuction.getId(), bidder, validAmount);

        boolean persisted = database.bidTransactions()
                .findAll()
                .stream()
                .anyMatch(bt -> bt.getAmount() == validAmount);
        assertTrue(persisted, "BidTransaction phải được lưu vào database sau khi đặt giá");
    }

    /**
     * Sau khi đặt giá thành công, số dư của người đặt giá phải giảm đúng
     * bằng số tiền đã cam kết.
     *
     * <p>{@code debitBidder()} trong service giữ lại số tiền đặt giá cho đến
     * khi phiên kết thúc. Test xác nhận số dư trước và sau lệnh gọi chênh
     * nhau chính xác {@code validAmount}.
     */
    @Test
    void placeBid_ValidRequest_DeductsBalanceFromBidder() {
        double validAmount    = START_PRICE + 1000.0;
        double balanceBefore  = bidder.getBalance();

        bidService.placeBid(runningAuction.getId(), bidder, validAmount);

        assertEquals(balanceBefore - validAmount, bidder.getBalance(), 0.001,
                "Số dư bidder phải giảm đúng bằng số tiền đặt giá");
    }

    /**
     * Khi người đặt giá thứ hai vượt qua người dẫn đầu cũ, số dư của người
     * dẫn đầu cũ phải được hoàn trả toàn bộ.
     *
     * <p>{@code refundPreviousHighestBid()} cộng lại số tiền bid cũ vào số dư
     * của người thua. Test xác nhận số dư của {@code bidder} (người dẫn đầu đầu
     * tiên) bằng với số dư ban đầu sau khi {@code bidder2} vượt qua.
     */
    @Test
    void placeBid_SecondBidByDifferentBidder_RefundsPreviousBidder() {
        Participant bidder2 = new Participant(
                "bidder02", "bidder2@mail.com", "pass", 100_000.0, "PARTICIPANT");
        database.users().save(bidder);
        database.users().save(bidder2);

        double firstBid            = START_PRICE + 1000.0;
        double secondBid           = START_PRICE + 2000.0;
        double bidder1BalanceBefore = bidder.getBalance();

        bidService.placeBid(runningAuction.getId(), bidder, firstBid);
        bidService.placeBid(runningAuction.getId(), bidder2, secondBid);

        assertEquals(bidder1BalanceBefore, bidder.getBalance(), 0.001,
                "Số dư của bidder1 phải được hoàn đầy đủ khi bị bidder2 vượt qua");
    }

    // =========================================================================
    // Nhóm 4: Concurrent bids — an toàn luồng của synchronized placeBid
    // =========================================================================

    /**
     * Nhiều luồng đặt giá đồng thời chỉ được phép thành công một phần.
     *
     * <p>Kịch bản: 20 luồng cùng xuất phát, mỗi luồng đặt giá tăng dần từ
     * {@code START_PRICE + 1} đến {@code START_PRICE + 20}. Vì
     * {@code Auction.placeBid()} được đánh dấu {@code synchronized}, chỉ luồng
     * nào giành được khóa trước mới đặt giá thành công; các luồng còn lại sẽ
     * nhận {@link InvalidBidException} vì giá của chúng thấp hơn giá đang dẫn
     * đầu sau khi thua race.
     *
     * <p>Xác nhận:
     * <ul>
     *   <li>Ít nhất 1 lượt đặt giá thành công.</li>
     *   <li>Tổng thành công + thất bại = số luồng (không có exception bị nuốt).</li>
     *   <li>{@code currentHighestBid} sau race lớn hơn {@code START_PRICE}.</li>
     * </ul>
     */
    @Test
    void placeBid_ConcurrentBidsFromMultipleThreads_OnlyValidBidsSucceed()
            throws InterruptedException {

        int threadCount  = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready  = new CountDownLatch(threadCount);
        CountDownLatch start  = new CountDownLatch(1);
        CountDownLatch done   = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failure = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final double amount = START_PRICE + 1.0 + i;
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    Participant p = new Participant(
                            "bidder_" + amount,
                            "bidder_" + amount + "@mail.com",
                            "pw", 100_000.0, "PARTICIPANT");
                    runningAuction.placeBid(new BidTransaction(p, amount, runningAuction));
                    success.incrementAndGet();
                } catch (InvalidBidException | AuctionClosedException e) {
                    failure.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        assertTrue(done.await(10, TimeUnit.SECONDS), "Timeout — có thể bị deadlock");
        executor.shutdown();

        assertTrue(success.get() >= 1,
                "Ít nhất 1 bid phải thành công");
        assertEquals(threadCount, success.get() + failure.get(),
                "Tổng thành công + thất bại phải bằng số luồng");
        assertNotNull(runningAuction.getCurrentHighestBid());
        assertTrue(runningAuction.getCurrentHighestBid().getAmount() > START_PRICE,
                "currentHighestBid phải lớn hơn startPrice sau concurrent bids");
    }

    /**
     * Sau khi nhiều luồng đặt giá đồng thời hoàn tất, giá dẫn đầu phải lớn
     * hơn giá khởi điểm.
     *
     * <p>Kịch bản: 10 luồng, mỗi luồng đặt một mức giá cách nhau 100 đơn vị
     * (từ {@code START_PRICE + 1} đến {@code START_PRICE + 901}). Không phải
     * tất cả đều thành công vì thứ tự lên lịch của hệ điều hành quyết định ai
     * giành khóa trước. Test chỉ xác nhận bất biến cuối cùng: giá dẫn đầu
     * không bao giờ lùi về dưới giá khởi điểm dù các luồng chạy không theo
     * thứ tự.
     */
    @Test
    void placeBid_ConcurrentBids_CurrentHighestBidIsAlwaysIncreasing()
            throws InterruptedException {

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final double amount = START_PRICE + 1.0 + (i * 100.0);
            executor.submit(() -> {
                try {
                    Participant p = new Participant(
                            "cb_" + amount, "cb_" + amount + "@mail.com",
                            "pw", 100_000.0, "PARTICIPANT");
                    runningAuction.placeBid(new BidTransaction(p, amount, runningAuction));
                } catch (Exception ignored) {
                    // Các lượt thua race bị từ chối là hành vi đúng — bỏ qua
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(done.await(10, TimeUnit.SECONDS), "Timeout");
        executor.shutdown();

        assertNotNull(runningAuction.getCurrentHighestBid());
        assertTrue(runningAuction.getCurrentHighestBid().getAmount() > START_PRICE,
                "currentHighestBid phải lớn hơn startPrice sau concurrent bids");
    }
}
