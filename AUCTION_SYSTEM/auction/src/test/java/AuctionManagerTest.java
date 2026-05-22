import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.items.Item;
import auction_system.common.models.items.builder.ElectronicBuilder;
import auction_system.common.models.users.Admin;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.server.core.AuctionManager;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử AuctionManager.
 *
 * Dùng @TempDir để database ghi vào thư mục tạm, tránh ảnh hưởng môi trường thật.
 * Reset Singleton qua reflection sau mỗi test để các test độc lập nhau.
 */
public class AuctionManagerTest {

    @TempDir
    Path tempDir;

    private AuctionManager manager;
    private SerializedDatabase database;

    @BeforeEach
    void setUp() throws Exception {
        resetSingleton();
        database = new SerializedDatabase(tempDir);
        manager = AuctionManager.getInstance(database);
    }

    @AfterEach
    void tearDown() throws Exception {
        manager.shutdown();
        resetSingleton();
    }

    /** Reset Singleton để mỗi test có instance sạch. */
    private void resetSingleton() throws Exception {
        Field field = AuctionManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }

    private Participant makeSeller() {
        return new Participant("seller01", "seller@test.com", "pass", 10_000.0, "SELLER");
    }

    private Auction makeAuction(Participant seller) {
        Item item = new ElectronicBuilder()
                .itemName("Test Item")
                .startPrice(1000.0)
                .sellerId(seller.getId())
                .build();
        return manager.createAuction(
                item, seller,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(2));
    }

    // =========================================================================
    // createAuction
    // =========================================================================

    @Test
    void testCreateAuction_ReturnsNonNull() {
        Auction auction = makeAuction(makeSeller());
        assertNotNull(auction);
    }

    @Test
    void testCreateAuction_AppearsInGetAllAuctions() {
        Participant seller = makeSeller();
        Auction auction = makeAuction(seller);

        assertTrue(manager.getAllAuctions().contains(auction));
    }

    @Test
    void testCreateAuction_InitialStatusIsOpen() {
        Auction auction = makeAuction(makeSeller());
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
    }

    @Test
    void testCreateMultipleAuctions_AllAppearInList() {
        Participant seller = makeSeller();
        makeAuction(seller);
        makeAuction(seller);

        // TestDataGenerator tạo thêm 5 auction mẫu khi init
        assertTrue(manager.getAllAuctions().size() >= 2);
    }

    // =========================================================================
    // getAuctionById
    // =========================================================================

    @Test
    void testGetAuctionById_ExistingId_ReturnsAuction() {
        Auction auction = makeAuction(makeSeller());
        assertSame(auction, manager.getAuctionById(auction.getId()));
    }

    @Test
    void testGetAuctionById_NonExistingId_ReturnsNull() {
        assertNull(manager.getAuctionById("non-existent-id"));
    }

    // =========================================================================
    // cancelAuction
    // =========================================================================

    @Test
    void testCancelAuction_ExistingId_ReturnsTrue() {
        Auction auction = makeAuction(makeSeller());
        assertTrue(manager.cancelAuction(auction.getId()));
    }

    @Test
    void testCancelAuction_ExistingId_StatusBecomesCANCELED() {
        Auction auction = makeAuction(makeSeller());
        manager.cancelAuction(auction.getId());
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
    }

    @Test
    void testCancelAuction_NonExistingId_ReturnsFalse() {
        assertFalse(manager.cancelAuction("non-existent-id"));
    }

    // =========================================================================
    // registerUser / isUsernameTaken
    // =========================================================================

    @Test
    void testRegisterUser_NewUsername_IsUsernameTakenReturnsFalse_Before() {
        assertFalse(manager.isUsernameTaken("brandnewuser"));
    }

    @Test
    void testRegisterUser_AfterRegister_IsUsernameTakenReturnsTrue() {
        Participant user = new Participant("brandnewuser", "new@test.com", "pw", 0.0);
        manager.registerUser(user);
        assertTrue(manager.isUsernameTaken("brandnewuser"));
    }

    @Test
    void testRegisterUser_AdminRole_AlsoRegistered() {
        Admin admin = new Admin("newadmin", "newadmin@test.com", "pw");
        manager.registerUser(admin);
        assertTrue(manager.isUsernameTaken("newadmin"));
    }

    // =========================================================================
    // findUserByCredentials
    // =========================================================================

    @Test
    void testFindUserByCredentials_CorrectEmailAndPassword_ReturnsUser() {
        Participant user = new Participant("alice", "alice@test.com", "pass123", 500.0);
        manager.registerUser(user);

        User found = manager.findUserByCredentials("alice@test.com", "pass123");
        assertSame(user, found);
    }

    @Test
    void testFindUserByCredentials_WrongPassword_ReturnsNull() {
        Participant user = new Participant("alice", "alice@test.com", "pass123", 500.0);
        manager.registerUser(user);

        assertNull(manager.findUserByCredentials("alice@test.com", "wrongpass"));
    }

    @Test
    void testFindUserByCredentials_WrongEmail_ReturnsNull() {
        assertNull(manager.findUserByCredentials("nobody@test.com", "pass"));
    }

    @Test
    void testFindUserByCredentials_NullEmail_ReturnsNull() {
        assertNull(manager.findUserByCredentials(null, "pass"));
    }

    @Test
    void testFindUserByCredentials_NullPassword_ReturnsNull() {
        assertNull(manager.findUserByCredentials("alice@test.com", null));
    }

    @Test
    void testFindUserByCredentials_EmailIsCaseInsensitive() {
        Participant user = new Participant("alice", "alice@test.com", "pass123", 500.0);
        manager.registerUser(user);

        assertSame(user, manager.findUserByCredentials("ALICE@TEST.COM", "pass123"));
    }

    // =========================================================================
    // userLoggedIn / userLoggedOut / isAlreadyOnline
    // =========================================================================

    @Test
    void testUserLoggedIn_UserBecomesOnline() {
        Participant user = new Participant("alice", "alice@test.com", "pw", 100.0);
        manager.userLoggedIn(user);
        assertTrue(manager.isAlreadyOnline(user.getId()));
    }

    @Test
    void testUserLoggedOut_UserBecomesOffline() {
        Participant user = new Participant("alice", "alice@test.com", "pw", 100.0);
        manager.userLoggedIn(user);
        manager.userLoggedOut(user);
        assertFalse(manager.isAlreadyOnline(user.getId()));
    }

    @Test
    void testGetOnlineCount_ReflectsLogins() {
        Participant u1 = new Participant("u1", "u1@test.com", "pw", 0.0);
        Participant u2 = new Participant("u2", "u2@test.com", "pw", 0.0);
        manager.userLoggedIn(u1);
        manager.userLoggedIn(u2);
        assertEquals(2, manager.getOnlineCount());
    }

    @Test
    void testIsAlreadyOnline_BeforeLogin_ReturnsFalse() {
        Participant user = new Participant("alice", "alice@test.com", "pw", 100.0);
        assertFalse(manager.isAlreadyOnline(user.getId()));
    }
}