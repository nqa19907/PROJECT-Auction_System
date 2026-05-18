import auction_system.common.exceptions.InvalidItemException;
import auction_system.common.models.Item;
import auction_system.common.models.Seller;
import auction_system.common.patterns.builder.ArtBuilder;
import auction_system.common.patterns.builder.ElectronicBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SellerTest {

    private Seller seller;
    private Item item;

    @BeforeEach
    void setUp() {
        seller = new Seller(
                "TrungNguyen",
                "trung@gmail.com",
                "matkhau123",
                50000,
                4.5f
        );

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
    void testListItemForAuctionValidItemSellerIdentifierUpdated() {
        seller.listItemForAuction(item);

        assertEquals(
                seller.getId(),
                item.getSellerId(),
                "sellerId của item phải được gán bằng ID của người bán"
        );
    }

    @Test
    void testListItemForAuctionNullItemThrowsException() {
        String actualMessage = assertThrows(
                InvalidItemException.class,
                () -> seller.listItemForAuction(null)
        ).getMessage();

        String expectedMessage = "Sản phẩm không hợp lệ!";

        assertEquals(
                expectedMessage,
                actualMessage,
                "Thông báo lỗi khi truyền null phải khớp"
        );
    }

    @Test
    void testListItemForAuctionMultipleItemsAllUpdated() {
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

        seller.listItemForAuction(item);
        seller.listItemForAuction(secondItem);

        assertEquals(
                seller.getId(),
                item.getSellerId(),
                "Item đầu tiên phải mang sellerId của seller"
        );

        assertEquals(
                seller.getId(),
                secondItem.getSellerId(),
                "Item thứ hai phải mang sellerId của seller"
        );
    }

    @Test
    void testDelistItemExistingItemRemovesSuccessfully() {
        seller.listItemForAuction(item);

        assertDoesNotThrow(
                () -> seller.delistItem(item),
                "Xóa item đang tồn tại không được ném ra ngoại lệ"
        );
    }

    @Test
    void testDelistItemItemNotInListThrowsException() {
        String actualMessage = assertThrows(
                InvalidItemException.class,
                () -> seller.delistItem(item)
        ).getMessage();

        String expectedMessage =
                "Không tìm thấy sản phẩm này trong danh sách của bạn.";

        assertEquals(
                expectedMessage,
                actualMessage,
                "Thông báo lỗi khi xóa item không tồn tại phải khớp"
        );
    }

    @Test
    void testDelistItemAfterRemovalThrowsException() {
        seller.listItemForAuction(item);
        seller.delistItem(item);

        assertThrows(
                InvalidItemException.class,
                () -> seller.delistItem(item),
                "Gỡ item đã bị xóa phải ném ra InvalidItemException"
        );
    }

    @Test
    void testGetRatingMatchesConstructorValue() {
        assertEquals(
                4.5f,
                seller.getRating(),
                0.001f,
                "Rating phải bằng giá trị đã truyền vào constructor"
        );
    }
}