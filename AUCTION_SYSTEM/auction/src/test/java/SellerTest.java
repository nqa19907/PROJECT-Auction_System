import auction_system.common.exceptions.InvalidItemException;
import auction_system.common.models.Item;
import auction_system.common.models.Seller;
import auction_system.common.patterns.builder.ElectronicBuilder;
import auction_system.common.patterns.builder.ArtBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

public class SellerTest {
    private static final Logger logger = LoggerFactory.getLogger(SellerTest.class);

    private Seller seller;
    private Item item;

    @BeforeEach
    void setUp() {
        // Arrange: Khởi tạo dữ liệu dùng chung cho các bài test
        seller = new Seller("BibNguyen", "bib@gmail.com", "matkhau123", 50000, 4.5f);
        item = new ElectronicBuilder()
                .itemName("Samsung Galaxy S25")
                .description("Flagship Android smartphone")
                .startPrice(3000.0)
                .sellerId("TEMP")
                .condition("New")
                .imagePath("")
                .brand("Samsung")
                .warrantyMonths(24)
                .build();
    }

    @Test
    void testListItemForAuction_ValidItem_SellerIdUpdatedToSellersId() {
        // Arrange: seller chưa có item nào (mới khởi tạo, danh sách rỗng)

        // Act: Đăng ký item để đấu giá
        seller.listItemForAuction(item);

        // Assert: sellerId của item phải được cập nhật sang ID của seller
        assertEquals(seller.getId(), item.getSellerId(),
                "sellerId của item phải được gán bằng ID của người bán");
    }

    @Test
    void testListItemForAuction_NullItem_ThrowsInvalidItemException() {
        // Arrange: Chuẩn bị tình huống truyền null vào

        // Act & Assert: Truyền null phải ném ra InvalidItemException
        String actualMessage = assertThrows(InvalidItemException.class, () -> {
            seller.listItemForAuction(null);
        }).getMessage();

        String expectedMessage = "Sản phẩm không hợp lệ!";
        assertEquals(expectedMessage, actualMessage,
                "Thông báo lỗi khi truyền null phải khớp");
    }

    @Test
    void testListItemForAuction_MultipleItems_AllItemsGetSellerId() {
        // Arrange: Tạo thêm một item thứ hai
        Item secondItem = new ArtBuilder()
                .itemName("Starry Night Replica")
                .description("Bản sao Starry Night của Van Gogh")
                .startPrice(10000.0)
                .sellerId("TEMP")
                .condition("Excellent")
                .imagePath("")
                .artistName("Van Gogh")
                .creationYear("2024")
                .hasAuthenticityCertificate(false)
                .build();

        // Act: Đăng ký hai sản phẩm khác nhau
        seller.listItemForAuction(item);
        seller.listItemForAuction(secondItem);

        // Assert: Cả hai item phải mang sellerId của seller
        assertEquals(seller.getId(), item.getSellerId(),
                "Item đầu tiên phải mang sellerId của seller");
        assertEquals(seller.getId(), secondItem.getSellerId(),
                "Item thứ hai phải mang sellerId của seller");
    }

    @Test
    void testDelistItem_ExistingItem_RemovesSuccessfully() {
        // Arrange: Đăng ký item trước rồi mới xóa
        seller.listItemForAuction(item);

        // Act & Assert: Xóa item đang tồn tại không được ném ra ngoại lệ nào
        assertDoesNotThrow(() -> seller.delistItem(item),
                "Xóa item đang tồn tại không được ném ra ngoại lệ");
    }

    @Test
    void testDelistItem_ItemNotInList_ThrowsInvalidItemException() {
        // Arrange: KHÔNG đăng ký item, thử xóa item chưa được thêm vào

        // Act & Assert: Xóa item không tồn tại phải ném ra InvalidItemException
        String actualMessage = assertThrows(InvalidItemException.class, () -> {
            seller.delistItem(item);
        }).getMessage();

        String expectedMessage = "Không tìm thấy sản phẩm này trong danh sách của bạn.";
        assertEquals(expectedMessage, actualMessage,
                "Thông báo lỗi khi xóa item không tồn tại phải khớp");
    }

    @Test
    void testDelistItem_AfterRemoval_SecondRemovalThrowsException() {
        // Arrange: Đăng ký item, sau đó gỡ nó ra
        seller.listItemForAuction(item);
        seller.delistItem(item);

        // Assert: Gỡ item lần thứ hai phải ném ra ngoại lệ (item đã bị xóa rồi)
        assertThrows(InvalidItemException.class, () -> {
            seller.delistItem(item);
        }, "Gỡ item đã bị xóa phải ném ra InvalidItemException");
    }

    @Test
    void testGetRating_MatchesConstructorValue() {
        // Assert: Rating phải khớp với giá trị truyền vào lúc khởi tạo
        assertEquals(4.5f, seller.getRating(), 0.001f,
                "Rating phải bằng giá trị đã truyền vào constructor");
    }
}
