import auction_system.common.exceptions.InvalidItemException;
import auction_system.common.models.items.Item;
import auction_system.common.models.items.builder.ArtBuilder;
import auction_system.common.models.items.builder.ElectronicBuilder;
import auction_system.common.models.users.Seller;

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

        // Khởi tạo seller dùng chung cho các test
        seller = new Seller(
                "TrungNguyen",
                "trung@gmail.com",
                "matkhau123",
                50000,
                4.5f
        );

        // Tạo item điện tử mẫu
        item = new ElectronicBuilder()
                .itemName("Samsung Galaxy S25")
                .description("Flagship Android smartphone")
                .startPrice(3000.0)
                .sellerId("TEMP")
                .imagePath("")
                .brand("Samsung")
                .warrantyMonths(24)
                .build();
    }

    @Test
    void testListItemForAuctionValidItemSellerIdentifierUpdated() {

        // Đăng item lên hệ thống đấu giá
        seller.listItemForAuction(item);

        // Kiểm tra sellerId của item đã được cập nhật đúng
        assertEquals(
                seller.getId(),
                item.getSellerId(),
                "sellerId của item phải được gán bằng ID của người bán"
        );
    }

    @Test
    void testListItemForAuctionNullItemThrowsException() {

        // Kiểm tra truyền null có ném exception không
        String actualMessage = assertThrows(
                InvalidItemException.class,
                () -> seller.listItemForAuction(null)
        ).getMessage();

        String expectedMessage = "Sản phẩm không hợp lệ!";

        // Kiểm tra nội dung exception
        assertEquals(
                expectedMessage,
                actualMessage,
                "Thông báo lỗi khi truyền null phải khớp"
        );
    }

    @Test
    void testListItemForAuctionMultipleItemsAllUpdated() {

        // Tạo thêm item nghệ thuật
        Item secondItem = new ArtBuilder()
                .itemName("Starry Night Replica")
                .description("Bản sao Starry Night của Van Gogh")
                .startPrice(10000.0)
                .sellerId("TEMP")
                .imagePath("")
                .artistName("Van Gogh")
                .creationYear("2024")
                .hasAuthenticityCertificate(false)
                .build();

        // Đăng cả hai item
        seller.listItemForAuction(item);
        seller.listItemForAuction(secondItem);

        // Kiểm tra item đầu tiên
        assertEquals(
                seller.getId(),
                item.getSellerId(),
                "Item đầu tiên phải mang sellerId của seller"
        );

        // Kiểm tra item thứ hai
        assertEquals(
                seller.getId(),
                secondItem.getSellerId(),
                "Item thứ hai phải mang sellerId của seller"
        );
    }

    @Test
    void testDelistItemExistingItemRemovesSuccessfully() {

        // Đăng item trước khi xóa
        seller.listItemForAuction(item);

        // Kiểm tra xóa item hợp lệ không ném exception
        assertDoesNotThrow(
                () -> seller.delistItem(item),
                "Xóa item đang tồn tại không được ném ra ngoại lệ"
        );
    }

    @Test
    void testDelistItemItemNotInListThrowsException() {

        // Kiểm tra xóa item chưa tồn tại
        String actualMessage = assertThrows(
                InvalidItemException.class,
                () -> seller.delistItem(item)
        ).getMessage();

        String expectedMessage =
                "Không tìm thấy sản phẩm này trong danh sách của bạn.";

        // Kiểm tra nội dung exception
        assertEquals(
                expectedMessage,
                actualMessage,
                "Thông báo lỗi khi xóa item không tồn tại phải khớp"
        );
    }

    @Test
    void testDelistItemAfterRemovalThrowsException() {

        // Đăng item
        seller.listItemForAuction(item);

        // Xóa item
        seller.delistItem(item);

        // Kiểm tra xóa lần hai phải ném exception
        assertThrows(
                InvalidItemException.class,
                () -> seller.delistItem(item),
                "Gỡ item đã bị xóa phải ném ra InvalidItemException"
        );
    }

    @Test
    void testGetRatingMatchesConstructorValue() {

        // Kiểm tra rating được khởi tạo đúng
        assertEquals(
                4.5f,
                seller.getRating(),
                0.001f,
                "Rating phải bằng giá trị đã truyền vào constructor"
        );
    }
}