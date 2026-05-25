import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.common.models.items.Art;
import auction_system.common.models.items.Item;
import auction_system.common.models.users.Participant;
import auction_system.server.persistence.serialization.SerializedDatabase;
import auction_system.server.services.ParticipantItemService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử việc đăng sản phẩm và ghi sản phẩm xuống database serialization.
 */
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.common.exceptions.InvalidItemException;
import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.items.Electronic;
import auction_system.common.models.items.Item;
import auction_system.common.models.users.Admin;
import auction_system.common.models.users.Participant;
import auction_system.server.persistence.serialization.SerializedDatabase;
import auction_system.server.services.ParticipantItemService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử tích hợp cho {@link ParticipantItemService}.
 *
 * <p>Ba nhóm chính theo từng public method:
 * <ol>
 *   <li>{@code listItemForAuction} — xác thực người dùng và sản phẩm, ghi
 *       database, gán {@code sellerId}.</li>
 *   <li>{@code delistItem} — xác thực quyền sở hữu, ngăn xóa khi phiên còn
 *       hoạt động, xóa thành công.</li>
 *   <li>{@code getItemsBySeller} — lọc sản phẩm đúng theo {@code sellerId}.</li>
 * </ol>
 */
public class ParticipantItemServiceTest {

    @TempDir
    private Path tempDir;

    private SerializedDatabase database;

    private ParticipantItemService itemService;

    /** Participant có role SELLER — dùng xuyên suốt hầu hết các test. */
    private Participant seller;

    /** Participant thứ hai dùng để kiểm tra phân tách dữ liệu giữa các seller. */
    private Participant otherSeller;

    /**
     * Khởi tạo database, service và các đối tượng dùng chung trước mỗi test.
     */
    @BeforeEach
    void setUp() {
        database = new SerializedDatabase(tempDir);
        itemService = new ParticipantItemService(database);

        seller = new Participant("seller01", "seller@mail.com", "pass", 0.0, "SELLER");

        otherSeller = new Participant(
                "other01", "other@mail.com", "pass", 0.0, "SELLER");
    }

    // =========================================================================
    // Helper
    // =========================================================================

    /**
     * Tạo một {@link Item} hợp lệ dùng làm fixture trong các test.
     *
     * @param sellerId id của người bán
     * @return item hợp lệ chưa được lưu
     */
    private Item buildItem(final String sellerId) {
        return new Electronic("MacBook Pro", "Laptop cao cap", 2000.0, sellerId);
    }

    /**
     * Tạo và lưu một phiên đấu giá với trạng thái cho trước.
     *
     * @param item   item đã được lưu vào database
     * @param status trạng thái của phiên đấu giá
     * @return phiên đấu giá đã được lưu
     */
    private Auction createAuctionWithStatus(final Item item, final AuctionStatus status) {
        Auction auction = new Auction(
                item,
                seller,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(2));
        auction.setStatus(status);
        database.auctions().save(auction);
        return auction;
    }

    // =========================================================================
    // Nhóm 1: listItemForAuction()
    // =========================================================================

    /**
     * User {@code null} (chưa đăng nhập) phải bị từ chối ngay tại
     * {@code requireSellerParticipant()}.
     */
    @Test
    void listItemNullUserThrowsInvalidItemException() {
        assertThrows(InvalidItemException.class,
                () -> itemService.listItemForAuction(null, buildItem(seller.getId())));
    }

    /**
     * {@link Admin} không phải {@link Participant} nên bị từ chối bởi
     * kiểm tra {@code instanceof Participant} trong {@code requireSellerParticipant()}.
     */
    @Test
    void listItemAdminUserThrowsInvalidItemException() {
        Admin admin = new Admin("admin01", "admin@mail.com", "pass");
        assertThrows(InvalidItemException.class,
                () -> itemService.listItemForAuction(admin, buildItem("any")));
    }

    /**
     * Đăng hai sản phẩm liên tiếp phải lưu cả hai vào database.
     */
    @Test
    void listItemTwoItemsPersistedBothToDatabase() {
        itemService.listItemForAuction(seller, buildItem(seller.getId()));
        itemService.listItemForAuction(seller,
                new Electronic("iPhone 15", "Dien thoai", 1500.0, seller.getId()));

        List<Item> items = database.items().findBySellerId(seller.getId());
        assertEquals(2, items.size(), "Phai co dung 2 san pham sau khi dang 2 lan.");
    }

    /**
     * Item {@code null} phải bị từ chối bởi {@code validateItem()} sau khi
     * người dùng đã qua kiểm tra quyền.
     */
    @Test
    void listItemNullItemThrowsInvalidItemException() {
        assertThrows(InvalidItemException.class,
                () -> itemService.listItemForAuction(seller, null));
    }

    /**
     * Item có tên rỗng phải bị từ chối bởi {@code validateText(itemName, ...)}.
     */
    @Test
    void listItemBlankNameThrowsInvalidItemException() {
        Item item = new Electronic("", "Mo ta", 1000.0, seller.getId());
        assertThrows(InvalidItemException.class,
                () -> itemService.listItemForAuction(seller, item));
    }

    /**
     * Giá khởi điểm bằng 0 phải bị từ chối; {@code validateItem()} kiểm tra
     * {@code startPrice <= 0}.
     */
    @Test
    void listItemZeroStartPriceThrowsInvalidItemException() {
        Item item = new Electronic("iPhone", "Dien thoai", 0.0, seller.getId());
        assertThrows(InvalidItemException.class,
                () -> itemService.listItemForAuction(seller, item));
    }

    /**
     * Giá khởi điểm âm phải bị từ chối — kiểm tra biên còn lại của
     * {@code startPrice <= 0}.
     */
    @Test
    void listItemNegativeStartPriceThrowsInvalidItemException() {
        Item item = new Electronic("iPhone", "Dien thoai", -500.0, seller.getId());
        assertThrows(InvalidItemException.class,
                () -> itemService.listItemForAuction(seller, item));
    }

    /**
     * Đăng sản phẩm hợp lệ phải trả về {@link Item} đã lưu với đúng tên.
     */
    @Test
    void listItemValidRequestReturnsPersistedItem() {
        Item saved = itemService.listItemForAuction(seller, buildItem(seller.getId()));

        assertNotNull(saved);
        assertEquals("MacBook Pro", saved.getItemName());
    }

    /**
     * Sau khi đăng sản phẩm, {@code sellerId} của item phải được gán bằng
     * {@code seller.getId()} bởi {@code saveItemForSeller()}, bất kể giá trị
     * {@code sellerId} ban đầu trong item là gì.
     */
    @Test
    void listItemValidRequestSetsSellerIdFromCurrentUser() {
        Item saved = itemService.listItemForAuction(seller, buildItem("wrong-id"));

        assertEquals(seller.getId(), saved.getSellerId(),
                "sellerId phai duoc gan lai bang ID cua seller dang dang nhap.");
    }

    /**
     * Item phải được ghi vào
     * {@link auction_system.server.persistence.repositories.ItemRepository}
     * và tìm lại được bằng {@code sellerId} sau khi đăng.
     */
    @Test
    void listItemValidRequestItemPersistedToDatabase() {
        itemService.listItemForAuction(seller, buildItem(seller.getId()));

        List<Item> items = database.items().findBySellerId(seller.getId());
        assertEquals(1, items.size(), "Phai co dung 1 san pham duoc luu vao database.");
    }

    /**
     * File {@code items.ser} phải tồn tại trên đĩa sau khi đăng sản phẩm —
     * xác minh {@code flushAll()} được gọi trong {@code saveItemForSeller()}.
     */
    @Test
    void listItemValidRequestSerFileExistsOnDisk() {
        itemService.listItemForAuction(seller, buildItem(seller.getId()));

        assertTrue(Files.exists(tempDir.resolve("items.ser")),
                "File items.ser phai duoc tao tren dia sau khi dang san pham.");
    }

    /**
     * Item phải đọc lại được từ một {@link SerializedDatabase} mới khởi tạo
     * từ cùng thư mục — xác minh dữ liệu thực sự được ghi xuống đĩa,
     * không
     * chỉ tồn tại trong bộ nhớ của instance cũ.
     *
     * <p>Kịch bản mô phỏng server restart: nếu {@code flushAll()} bị thiếu
     * hoặc gọi sai thời điểm, item sẽ mất sau khi "restart" dù
     * {@code listItemValidRequestItemPersistedToDatabase} vẫn xanh.
     */
    @Test
    void listItemValidRequestItemSurvivesAfterDatabaseRestart() {
        Item saved = itemService.listItemForAuction(seller, buildItem(seller.getId()));

        SerializedDatabase restarted = new SerializedDatabase(tempDir);
        Optional<Item> found = restarted.items().findById(saved.getId());

        assertTrue(found.isPresent(),
                "Item phai con ton tai sau khi tao SerializedDatabase moi.");
        assertEquals(saved.getId(), found.get().getId());
    }

    // =========================================================================
    // Nhóm 2: delistItem()
    // =========================================================================

    /**
     * itemId {@code null} phải bị từ chối bởi {@code validateText()} trước
     * khi truy vấn database.
     */
    @Test
    void delistItemNullItemIdThrowsInvalidItemException() {
        assertThrows(InvalidItemException.class,
                () -> itemService.delistItem(seller, null));
    }

    /**
     * itemId chỉ có khoảng trắng phải bị từ chối.
     */
    @Test
    void delistItemBlankItemIdThrowsInvalidItemException() {
        assertThrows(InvalidItemException.class,
                () -> itemService.delistItem(seller, "   "));
    }

    /**
     * itemId không tồn tại trong database phải ném {@link InvalidItemException}
     * từ {@code findItemOrThrow()}.
     */
    @Test
    void delistItemNotFoundThrowsInvalidItemException() {
        assertThrows(InvalidItemException.class,
                () -> itemService.delistItem(seller, "non-existent-id"));
    }

    /**
     * Seller khác không phải chủ sở hữu phải bị từ chối bởi
     * {@code validateOwnership()}.
     */
    @Test
    void delistItemNotOwnerThrowsInvalidItemException() {
        Item saved = itemService.listItemForAuction(seller, buildItem(seller.getId()));

        assertThrows(InvalidItemException.class,
                () -> itemService.delistItem(otherSeller, saved.getId()),
                "Seller khong phai chu so huu phai bi tu choi.");
    }

    /**
     * Sản phẩm đang nằm trong phiên đấu giá trạng thái {@code OPEN} không
     * được phép xóa; {@code validateItemCanBeDeleted()} kiểm tra
     * {@code OPEN || RUNNING}.
     */
    @Test
    void delistItemInOpenAuctionThrowsInvalidItemException() {
        Item saved = itemService.listItemForAuction(seller, buildItem(seller.getId()));
        createAuctionWithStatus(saved, AuctionStatus.OPEN);

        assertThrows(InvalidItemException.class,
                () -> itemService.delistItem(seller, saved.getId()),
                "Khong duoc go san pham khi phien dau gia dang OPEN.");
    }

    /**
     * Sản phẩm đang trong phiên {@code RUNNING} cũng không được xóa — nhánh
     * còn lại của {@code isActiveAuction()}.
     */
    @Test
    void delistItemInRunningAuctionThrowsInvalidItemException() {
        Item saved = itemService.listItemForAuction(seller, buildItem(seller.getId()));
        createAuctionWithStatus(saved, AuctionStatus.RUNNING);

        assertThrows(InvalidItemException.class,
                () -> itemService.delistItem(seller, saved.getId()),
                "Khong duoc go san pham khi phien dau gia dang RUNNING.");
    }

    /**
     * Sản phẩm trong phiên {@code FINISHED} có thể được gỡ vì phiên không
     * còn hoạt động.
     */
    @Test
    void delistItemInFinishedAuctionDeletesSuccessfully() {
        Item saved = itemService.listItemForAuction(seller, buildItem(seller.getId()));
        createAuctionWithStatus(saved, AuctionStatus.FINISHED);

        assertTrue(itemService.delistItem(seller, saved.getId()),
                "Duoc phep go san pham khi phien dau gia da FINISHED.");
    }

    /**
     * Gỡ sản phẩm hợp lệ phải trả về {@code true} và xóa khỏi database.
     */
    @Test
    void delistItemValidRequestReturnsTrueAndRemovesFromDatabase() {
        Item saved = itemService.listItemForAuction(seller, buildItem(seller.getId()));

        boolean result = itemService.delistItem(seller, saved.getId());

        assertTrue(result);
        assertTrue(database.items().findBySellerId(seller.getId()).isEmpty(),
                "San pham phai bi xoa khoi database sau khi go.");
    }

    // =========================================================================
    // Nhóm 3: getItemsBySeller()
    // =========================================================================

    /**
     * User {@code null} phải bị từ chối bởi {@code requireSellerParticipant()}.
     */
    @Test
    void getItemsBySellerNullUserThrowsInvalidItemException() {
        assertThrows(InvalidItemException.class,
                () -> itemService.getItemsBySeller(null));
    }

    /**
     * Seller có sản phẩm phải nhận về danh sách không rỗng
     * với đúng số lượng.
     */
    @Test
    void getItemsBySellerWithItemsReturnsCorrectCount() {
        itemService.listItemForAuction(seller, buildItem(seller.getId()));
        itemService.listItemForAuction(seller, buildItem(seller.getId()));

        List<Item> items = itemService.getItemsBySeller(seller);
        assertEquals(2, items.size());
    }

    /**
     * Seller chưa đăng sản phẩm nào phải nhận về danh sách rỗng.
     */
    @Test
    void getItemsBySellerNoItemsReturnsEmptyList() {
        List<Item> items = itemService.getItemsBySeller(seller);

        assertTrue(items.isEmpty());
    }

    /**
     * Chỉ trả về sản phẩm thuộc seller hiện tại — sản phẩm của seller khác
     * không được lẫn vào kết quả.
     */
    @Test
    void getItemsBySellerReturnsOnlyOwnedItems() {
        itemService.listItemForAuction(seller, buildItem(seller.getId()));
        itemService.listItemForAuction(seller, buildItem(seller.getId()));
        itemService.listItemForAuction(otherSeller, buildItem(otherSeller.getId()));

        List<Item> items = itemService.getItemsBySeller(seller);

        assertEquals(2, items.size(), "Chi tra ve san pham cua seller hien tai.");
        assertTrue(items.stream().allMatch(i -> seller.getId().equals(i.getSellerId())));
    }
}