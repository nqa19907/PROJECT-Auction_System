import auction_system.common.enums.AuctionStatus;
import auction_system.common.models.Auction;
import auction_system.common.models.Electronic;
import auction_system.common.models.Item;
import auction_system.common.models.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class AuctionLifecycleTest {

    private Auction auction;

    @BeforeEach
    void setUp() {
        Item item = new Electronic("iPhone 15", "Apple smartphone",
                2000, "SN001", "New", "", "Apple", 12);
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
}