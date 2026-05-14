import auction_system.common.exceptions.AuctionClosedException;
import auction_system.common.exceptions.InvalidBidException;
import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.items.Item;
import auction_system.common.models.users.Bidder;
import auction_system.common.models.users.Seller;
import auction_system.common.patterns.builder.ElectronicBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class CoreBiddingLogicTest {
    private static final Logger logger = LoggerFactory.getLogger(CoreBiddingLogicTest.class);

    private double startPrice;
    private Item item;
    private Seller seller;
    private Auction auction;

    @BeforeEach
    void setUp() {
        // Arrange: Chuẩn bị dữ liệu mặc định cho mỗi bài test (khởi tạo Item, Seller, Auction)
        startPrice = 2000;
        item = new ElectronicBuilder()
                .itemName("OPPO Find X9 Ultra")
                .description("The best smartphone camera")
                .startPrice(startPrice)
                .sellerId("61h23s1")
                .condition("Sealed")
                .imagePath("")
                .brand("OPPO")
                .warrantyMonths(12)
                .build();
        seller = new Seller("Nguyễn Trọng Hoàng", "lamviet7577@gmail.com",
                                    "69420", 69420, 4.69f);
        
        auction = new Auction(item, seller, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        auction.startAuction();
    }

    @Test
    void testPlaceBid_ValidAmountSlightlyAboveStartPrice_UpdatesHighestBid() {
        // Arrange: Giá đặt chỉ cao hơn giá khởi điểm một chút (kiểm tra biên)
        double validBidAmount = 2000.1;
        BidTransaction validBid = new BidTransaction(null, validBidAmount);
        
        // Act: Thực hiện đặt giá
        auction.placeBid(validBid);

        // Assert: Xác nhận giao dịch thành công và cập nhật đúng giá cao nhất
        assertEquals(validBidAmount, validBid.getAmount());
        assertEquals(validBidAmount, auction.getCurrentHighestBid().getAmount(), 
                "Giá cao nhất của phiên đấu giá phải được cập nhật");
    }
    
    @Test
    void testPlaceBid_ValidAmountSignificantlyHigher_UpdatesHighestBid() {
        // Arrange: Giá đặt cao hơn hẳn giá khởi điểm
        double validBidAmount = 10000;
        BidTransaction validBid = new BidTransaction(null, validBidAmount);
        
        // Act: Thực hiện đặt giá
        auction.placeBid(validBid);

        // Assert: Xác nhận giao dịch thành công và cập nhật đúng giá cao nhất
        assertEquals(validBidAmount, validBid.getAmount());
        assertEquals(validBidAmount, auction.getCurrentHighestBid().getAmount());
    }

    
    @Test
    void testPlaceBid_AmountEqualsStartPrice_ThrowsInvalidBidException() {
        // Arrange: Tạo giao dịch với giá tiền BẰNG mức giá khởi điểm (không hợp lệ)
        double invalidBidAmount = 2000;
        BidTransaction invalidBid = new BidTransaction(null, invalidBidAmount);

        // Act & Assert: Thực hiện đặt giá và kỳ vọng nhận về InvalidBidException
        String actualMessage = assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(invalidBid);
        }).getMessage();

        String expectedMessage = "Giá đặt phải lớn hơn giá cao nhất hiện tại (" + startPrice + ")";
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void testPlaceBid_AmountLowerThanStartPrice_ThrowsInvalidBidException() {
        // Arrange: Tạo giao dịch với giá tiền THẤP HƠN mức giá khởi điểm (không hợp lệ)
        double invalidBidAmount = 1000;
        BidTransaction invalidBid = new BidTransaction(null, invalidBidAmount);

        // Act & Assert: Thực hiện đặt giá và kỳ vọng nhận về InvalidBidException
        String actualMessage = assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(invalidBid);
        }).getMessage();

        String expectedMessage = "Giá đặt phải lớn hơn giá cao nhất hiện tại (" + startPrice + ")";
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void testPlaceBid_AuctionIsClosed_ThrowsAuctionClosedException() {
        // Arrange: Đóng phiên đấu giá bằng cách ép thời gian kết thúc về quá khứ
        auction.setEndTime(LocalDateTime.MIN);
        auction.endAuction();

        // Tạo một giao dịch ảo để tránh lỗi NullPointerException
        BidTransaction dummyBid = new BidTransaction(null, 3000);

        // Act & Assert: Kiểm tra việc đặt giá khi phiên đã đóng
        String actualMessage = assertThrows(AuctionClosedException.class, () -> {
            auction.placeBid(dummyBid);
        }).getMessage();

        String expectedMessage = "Phiên đấu giá này không ở trạng thái mở hoặc đã đóng!";
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void testCalculateWinner_AuctionEndedWithBids_ReturnsHighestBidder() {
        // Arrange: Tạo người thắng kỳ vọng và đặt giá hợp lệ
        Bidder expectedWinner = new Bidder("Phạm Việt Hoàng", "pvhgay@gmail.com",
                                    "123456789", 4000);
        BidTransaction bid = new BidTransaction(expectedWinner, 2500);
        auction.placeBid(bid);

        // Act: Kết thúc phiên đấu giá
        auction.setEndTime(LocalDateTime.MIN);
        auction.endAuction();
        
        // Assert: Xác định được người thắng phải trùng với người vừa đặt giá
        Bidder actualWinner = auction.calculateWinner();
        assertSame(expectedWinner, actualWinner);
    }

    @Test
    void testCalculateWinner_AuctionEndedWithoutBids_ReturnsNull() {
        // Arrange & Act: Kết thúc phiên đấu giá mà không có bất kì lượt đặt giá nào
        auction.setEndTime(LocalDateTime.MIN);
        auction.endAuction();
        
        // Assert: Do không có người mua nên người thắng phải là null
        Bidder actualWinner = auction.calculateWinner();
        assertNull(actualWinner, "Không có lượt đặt giá thì người chiến thắng phải là null");
    }

    @Test
    void testCalculateWinner_AuctionStillRunning_ThrowsIllegalStateException() {
        // Arrange: Đặt một giá bất kỳ trong khi phiên đấu giá ĐANG CHẠY (chưa kết thúc)
        BidTransaction bid = new BidTransaction(null, 2500);
        auction.placeBid(bid);

        // Act & Assert: Cố gắng gọi hàm tính người thắng sớm sẽ ném ra ngoại lệ
        String actualMessage = assertThrows(IllegalStateException.class, () -> {
            Bidder winner = auction.calculateWinner();
        }).getMessage();

        String expectedMessage = "Phiên đấu giá chưa kết thúc, chưa thể tìm được người thắng!";
        assertEquals(expectedMessage, actualMessage);
    }
}
