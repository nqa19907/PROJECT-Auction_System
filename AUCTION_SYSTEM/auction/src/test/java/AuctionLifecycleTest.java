import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.items.Item;
import auction_system.common.models.items.builder.ElectronicBuilder;
import auction_system.common.models.users.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Kiểm thử vòng đời (lifecycle) của một phiên đấu giá:
 * khởi tạo, bắt đầu, kết thúc và xác định người thắng.
 */
public class AuctionLifecycleTest {

    private Item item;
    private Seller seller;
    private Auction auction;

    @BeforeEach
    void setUp() {
        item = new ElectronicBuilder()
                .itemName("iPhone 15")
                .description("Apple smartphone")
                .startPrice(2000.0)
                .sellerId("SN001")
                .build();

        seller = new Seller("John", "john@gmail.com", "123456", 10_000.0);

        auction = new Auction(item, seller,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1));
    }

    // =========================================================================
    // Trạng thái khởi tạo
    // =========================================================================

    @Test
    void testNewAuction_StatusIsOpen() {
        assertEquals(AuctionStatus.OPEN, auction.getStatus(),
                "Auction mới tạo phải có trạng thái OPEN");
    }

    @Test
    void testNewAuction_SellerAndItemAreSet() {
        assertSame(seller, auction.getParticipant(), "Seller phải được gán đúng");
        assertSame(item, auction.getItem(), "Item phải được gán đúng");
    }

    // =========================================================================
    // startAuction
    // =========================================================================

    @Test
    void testStartAuction_StatusChangesToRunning() {
        auction.startAuction();

        assertEquals(AuctionStatus.RUNNING, auction.getStatus(),
                "Sau khi start, trạng thái phải là RUNNING");
    }

    @Test
    void testStartAuction_WhenFutureStartTime_DoesNotStart() {
        Auction futureAuction = new Auction(item, seller,
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(4));

        futureAuction.startAuction();

        assertEquals(AuctionStatus.OPEN, futureAuction.getStatus(),
                "Auction chưa tới giờ bắt đầu thì vẫn phải là OPEN");
    }

    @Test
    void testStartAuction_WhenAlreadyRunning_DoesNotChangeStatus() {
        auction.startAuction();

        auction.startAuction();

        assertEquals(AuctionStatus.RUNNING, auction.getStatus(),
                "Gọi startAuction lần 2 khi đang RUNNING không được thay đổi trạng thái");
    }

    @Test
    void testStartAuction_WhenAlreadyFinished_DoesNotRestart() {
        auction.startAuction();
        auction.setEndTime(LocalDateTime.MIN);
        auction.endAuction();

        auction.startAuction();

        assertEquals(AuctionStatus.FINISHED, auction.getStatus(),
                "Auction đã FINISHED không thể khởi động lại");
    }

    // =========================================================================
    // endAuction
    // =========================================================================

    @Test
    void testEndAuction_StatusChangesToFinished() {
        auction.startAuction();
        auction.setEndTime(LocalDateTime.MIN);

        auction.endAuction();

        assertEquals(AuctionStatus.FINISHED, auction.getStatus(),
                "Sau khi end, trạng thái phải là FINISHED");
    }

    @Test
    void testEndAuction_WhenStatusIsOpen_DoesNotEnd() {
        auction.setEndTime(LocalDateTime.MIN);

        auction.endAuction();

        assertEquals(AuctionStatus.OPEN, auction.getStatus(),
                "Auction đang OPEN không thể kết thúc trực tiếp");
    }

    @Test
    void testEndAuction_WhenEndTimeInFuture_DoesNotEnd() {
        auction.startAuction();

        auction.endAuction();

        assertEquals(AuctionStatus.RUNNING, auction.getStatus(),
                "Auction chưa hết giờ thì không được kết thúc");
    }

}
