import auction_system.common.models.users.Bidder;
import auction_system.common.models.users.Seller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.items.Item;
import auction_system.common.models.items.builder.ElectronicBuilder;

public class AuctionLifecycleTest {

    private Auction auction;

    @BeforeEach
    void setUp() {
        Item item = new ElectronicBuilder()
                .itemName("iPhone 15")
                .description("Apple smartphone")
                .startPrice(2000.0)
                .sellerId("SN001")
                .build();
        Seller seller = new Seller("John", "john@gmail.com", "123456", 10000, 5.0f);
        auction = new Auction(item, seller,
                LocalDateTime.now(), LocalDateTime.now().plusHours(1));
    }

    @Test
    void testStartAuction_StatusChangesToRunning() {
        // Act
        auction.startAuction();

        // Assert
        assertEquals(AuctionStatus.RUNNING, auction.getStatus(),
                "Sau khi start, trạng thái phải là RUNNING");
    }

    @Test
    void testEndAuction_StatusChangesToFinished() {
        // Arrange
        auction.startAuction();
        auction.setEndTime(LocalDateTime.MIN);

        // Act
        auction.endAuction();

        // Assert
        assertEquals(AuctionStatus.FINISHED, auction.getStatus(),
                "Sau khi end, trạng thái phải là FINISHED");
    }
    @Test
    void testNewAuction_StatusIsOpen() {
        // Assert: Auction mới tạo phải có status OPEN
        assertEquals(AuctionStatus.OPEN, auction.getStatus(),
                "Auction mới tạo phải có trạng thái OPEN");
    }

    @Test
    void testMultipleBids_HighestBidWins() {
        // Arrange
        auction.startAuction();
        Bidder bidder1 = new Bidder("Alice", "alice@gmail.com", "123", 10000);
        Bidder bidder2 = new Bidder("Bob", "bob@gmail.com", "456", 20000);

        // Act
        auction.placeBid(new BidTransaction(bidder1, 3000,auction));
        auction.placeBid(new BidTransaction(bidder2, 5000,auction));

        auction.setEndTime(LocalDateTime.MIN);
        auction.endAuction();

        // Assert
        assertSame(bidder2, auction.calculateWinner(),
                "Người đặt giá cao nhất phải thắng");
    }
}