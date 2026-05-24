import auction_system.common.exceptions.AuctionClosedException;
import auction_system.common.exceptions.InvalidBidException;
import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.items.Item;
import auction_system.common.models.items.builder.ElectronicBuilder;
import auction_system.common.models.users.Participant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class CoreBiddingLogicTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoreBiddingLogicTest.class);

    private double startPrice;
    private Item item;
    private Participant seller;
    private Auction auction;

    @BeforeEach
    void setUp() {
        // Arrange: Chuẩn bị dữ liệu mặc định cho mỗi bài test (khởi tạo Item, Participant, Auction)
        startPrice = 2000;
        item = new ElectronicBuilder()
                .itemName("OPPO Find X9 Ultra")
                .description("The best smartphone camera")
                .startPrice(startPrice)
                .sellerId("61h23s1")
                .build();
        seller = new Participant("Nguyễn Trọng Hoàng", "lamviet7577@gmail.com",
                "69420", 69420, "PARTICIPANT");

        auction = new Auction(item, seller, LocalDateTime.now(), LocalDateTime.now().plusHours(1));
        auction.startAuction();
    }

    @Test
    void testPlaceBid_ValidAmountSlightlyAboveStartPrice_UpdatesHighestBid() {
        // Arrange: Giá đặt chỉ cao hơn giá khởi điểm một chút (kiểm tra biên)
        double validBidAmount = 2000.1;
        BidTransaction validBid = new BidTransaction(null, validBidAmount, auction);

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
        BidTransaction validBid = new BidTransaction(null, validBidAmount, auction);

        // Act: Thực hiện đặt giá
        auction.placeBid(validBid);

        // Assert: Xác nhận giao dịch thành công và cập nhật đúng giá cao nhất
        assertEquals(validBidAmount, validBid.getAmount());
        assertEquals(validBidAmount, auction.getCurrentHighestBid().getAmount());
    }

    @Test
    void testPlaceBid_SecondBidHigherThanFirst_UpdatesHighestBid() {
        // Arrange
        auction.placeBid(new BidTransaction(null, 3000, auction));

        // Act
        auction.placeBid(new BidTransaction(null, 5000, auction));

        // Assert
        assertEquals(5000, auction.getCurrentHighestBid().getAmount(),
                "Bid mới cao hơn phải trở thành currentHighestBid");
    }

    @Test
    void testPlaceBid_ItemCurrentPrice_UpdatedAfterBid() {
        // Arrange
        double bidAmount = 3500.0;

        // Act
        auction.placeBid(new BidTransaction(null, bidAmount, auction));

        // Assert
        assertEquals(bidAmount, item.getCurrentPrice(),
                "Giá hiện tại của item phải bằng giá bid mới nhất");
    }

    @Test
    void testPlaceBid_AmountEqualsStartPrice_ThrowsInvalidBidException() {
        // Arrange: Tạo giao dịch với giá tiền BẰNG mức giá khởi điểm (không hợp lệ)
        double invalidBidAmount = 2000;
        BidTransaction invalidBid = new BidTransaction(null, invalidBidAmount, auction);

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
        BidTransaction invalidBid = new BidTransaction(null, invalidBidAmount, auction);

        // Act & Assert: Thực hiện đặt giá và kỳ vọng nhận về InvalidBidException
        String actualMessage = assertThrows(InvalidBidException.class, () -> {
            auction.placeBid(invalidBid);
        }).getMessage();

        String expectedMessage = "Giá đặt phải lớn hơn giá cao nhất hiện tại (" + startPrice + ")";
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void testPlaceBid_AuctionNotStarted_ThrowsAuctionClosedException() {
        // Arrange: Auction mới tạo, chưa start (status = OPEN)
        Auction openAuction = new Auction(item, seller,
                LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        // Act & Assert
        assertThrows(AuctionClosedException.class, () ->
                        openAuction.placeBid(new BidTransaction(null, 3000, openAuction)),
                "Đặt giá khi auction chưa start phải ném AuctionClosedException");
    }

    @Test
    void testPlaceBid_AuctionIsClosed_ThrowsAuctionClosedException() {
        // Arrange: Đóng phiên đấu giá bằng cách ép thời gian kết thúc về quá khứ
        auction.setEndTime(LocalDateTime.MIN);
        auction.endAuction();

        // Tạo một giao dịch ảo để tránh lỗi NullPointerException
        BidTransaction dummyBid = new BidTransaction(null, 3000, auction);

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
        Participant expectedWinner = new Participant("Phạm Việt Hoàng", "pvhgay@gmail.com",
                "123456789", 4000, "PARTICIPANT");
        BidTransaction bid = new BidTransaction(expectedWinner, 2500, auction);
        auction.placeBid(bid);

        // Act: Kết thúc phiên đấu giá
        auction.setEndTime(LocalDateTime.MIN);
        auction.endAuction();

        // Assert: Xác định được người thắng phải trùng với người vừa đặt giá
        Participant actualWinner = auction.calculateWinner();
        assertSame(expectedWinner, actualWinner);
    }

    @Test
    void testCalculateWinner_AuctionEndedWithoutBids_ReturnsNull() {
        // Arrange & Act: Kết thúc phiên đấu giá mà không có bất kì lượt đặt giá nào
        auction.setEndTime(LocalDateTime.MIN);
        auction.endAuction();

        // Assert: Do không có người mua nên người thắng phải là null
        Participant actualWinner = auction.calculateWinner();
        assertNull(actualWinner, "Không có lượt đặt giá thì người chiến thắng phải là null");
    }

    @Test
    void testCalculateWinner_AuctionStillRunning_ThrowsIllegalStateException() {
        // Arrange: Đặt một giá bất kỳ trong khi phiên đấu giá ĐANG CHẠY (chưa kết thúc)
        BidTransaction bid = new BidTransaction(null, 2500, auction);
        auction.placeBid(bid);

        // Act & Assert: Cố gắng gọi hàm tính người thắng sớm sẽ ném ra ngoại lệ
        String actualMessage = assertThrows(IllegalStateException.class, () -> {
            auction.calculateWinner();
        }).getMessage();

        String expectedMessage = "Phiên đấu giá chưa kết thúc, chưa thể tìm được người thắng!";
        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    void testCalculateWinner_MultipleBidders_ReturnsCorrectWinner() {
        // Arrange
        Participant bidder1 = new Participant("Alice", "alice@mail.com", "pw1", 20000, "PARTICIPANT");
        Participant bidder2 = new Participant("Bob", "bob@mail.com", "pw2", 20000, "PARTICIPANT");
        Participant bidder3 = new Participant("Charlie", "charlie@mail.com", "pw3", 20000, "PARTICIPANT");
        auction.placeBid(new BidTransaction(bidder1, 2500, auction));
        auction.placeBid(new BidTransaction(bidder2, 5000, auction));
        auction.placeBid(new BidTransaction(bidder3, 8000, auction));
        auction.setEndTime(LocalDateTime.MIN);
        auction.endAuction();

        // Assert
        assertSame(bidder3, auction.calculateWinner(),
                "bidder3 đặt 8000 (cao nhất) phải là người thắng");
    }

}
