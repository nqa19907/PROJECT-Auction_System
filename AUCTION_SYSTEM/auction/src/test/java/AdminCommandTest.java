import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.items.Item;
import auction_system.common.models.items.builder.ElectronicBuilder;
import auction_system.common.models.users.Admin;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.admin.AdminCancelAuctionCommand;
import auction_system.server.network.command.admin.AdminDeleteAuctionCommand;
import auction_system.server.network.command.admin.AdminDeleteUserCommand;
import auction_system.server.network.command.admin.AdminListAuctionsCommand;
import auction_system.server.network.command.admin.AdminListUsersCommand;
import auction_system.server.persistence.serialization.SerializedDatabase;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử các command admin: {@link AdminCancelAuctionCommand},
 * {@link AdminDeleteAuctionCommand}, {@link AdminDeleteUserCommand},
 * {@link AdminListAuctionsCommand}, {@link AdminListUsersCommand}.
 *
 * <p>Các nhóm test:
 * <ol>
 *   <li>AdminListUsersCommand — chưa đăng nhập, không phải admin, thành công.</li>
 *   <li>AdminListAuctionsCommand — chưa đăng nhập, không phải admin, thành công.</li>
 *   <li>AdminCancelAuctionCommand — chưa đăng nhập, không phải admin, thiếu id, id sai,
 *       thành công.</li>
 *   <li>AdminDeleteAuctionCommand — chưa đăng nhập, không phải admin, thiếu id, id sai,
 *       thành công.</li>
 *   <li>AdminDeleteUserCommand — chưa đăng nhập, không phải admin, thiếu id, tự xóa,
 *       xóa admin khác, xóa user thành công.</li>
 * </ol>
 */
public class AdminCommandTest {

    @TempDir
    Path tempDir;

    private AuctionManager manager;
    private SerializedDatabase database;

    /** Observer không làm gì, dùng để dựng ClientSession trong test. */
    private static final AuctionObserver NOOP_OBSERVER = msg -> { };

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

    /** Reset AuctionManager singleton để các test độc lập nhau. */
    private void resetSingleton() throws Exception {
        Field field = AuctionManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }

    /** Tạo ClientSession mới chưa đăng nhập. */
    private ClientSession newSession() {
        return new ClientSession(NOOP_OBSERVER, manager);
    }

    /** Tạo ClientSession với user Admin đã đăng nhập. */
    private ClientSession adminSession() {
        Admin admin = new Admin("admin01", "admin@test.com", "pass");
        ClientSession session = new ClientSession(NOOP_OBSERVER, manager);
        session.setCurrentUser(admin);
        return session;
    }

    /** Tạo ClientSession với user Participant đã đăng nhập. */
    private ClientSession participantSession() {
        Participant p = new Participant("user01", "user@test.com", "pass", 0.0, "PARTICIPANT");
        ClientSession session = new ClientSession(NOOP_OBSERVER, manager);
        session.setCurrentUser(p);
        return session;
    }

    /**
     * Tạo payload JSON chứa auctionId.
     *
     * @param auctionId mã phiên đấu giá
     * @return payload JSON
     */
    private JsonNode auctionIdPayload(final String auctionId) {
        return JsonProtocol.payloadOf(Map.of("auctionId", auctionId));
    }

    /**
     * Tạo payload JSON chứa userId.
     *
     * @param userId mã người dùng
     * @return payload JSON
     */
    private JsonNode userIdPayload(final String userId) {
        return JsonProtocol.payloadOf(Map.of("userId", userId));
    }

    /**
     * Tạo một phiên đấu giá test và trả về ID của phiên đó.
     *
     * @return ID phiên đấu giá vừa tạo
     */
    private String createTestAuction() {
        Participant seller = new Participant("seller", "s@test.com", "pass", 0.0, "PARTICIPANT");
        manager.registerUser(seller);
        Item item = new ElectronicBuilder()
                .itemName("Test Item")
                .description("Mo ta san pham test")
                .startPrice(1000.0)
                .sellerId(seller.getId())
                .build();
        return manager.createAuction(
                item, seller,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(2)).getId();
    }

    // =========================================================================
    // AdminListUsersCommand
    // =========================================================================

    /**
     * AdminListUsers khi chưa đăng nhập phải trả ADMIN_USER_LIST_FAIL.
     */
    @Test
    void adminListUsers_NotLoggedIn_ReturnsFail() {
        AdminListUsersCommand cmd = new AdminListUsersCommand(manager);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_USER_LIST_FAIL.name()),
                "Chua dang nhap phai tra ADMIN_USER_LIST_FAIL.");
    }

    /**
     * AdminListUsers khi là PARTICIPANT phải trả ADMIN_USER_LIST_FAIL.
     */
    @Test
    void adminListUsers_NonAdmin_ReturnsFail() {
        AdminListUsersCommand cmd = new AdminListUsersCommand(manager);

        String response = cmd.execute(null, participantSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_USER_LIST_FAIL.name()),
                "Non-admin phai tra ADMIN_USER_LIST_FAIL.");
    }

    /**
     * AdminListUsers khi là ADMIN phải trả ADMIN_USER_LIST.
     */
    @Test
    void adminListUsers_Admin_ReturnsUserList() {
        AdminListUsersCommand cmd = new AdminListUsersCommand(manager);

        String response = cmd.execute(null, adminSession());

        assertNotNull(response, "Response khong duoc null.");
        assertTrue(response.contains(Protocol.Response.ADMIN_USER_LIST.name()),
                "Admin phai tra ADMIN_USER_LIST.");
    }

    // =========================================================================
    // AdminListAuctionsCommand
    // =========================================================================

    /**
     * AdminListAuctions khi chưa đăng nhập phải trả ADMIN_AUCTION_LIST_FAIL.
     */
    @Test
    void adminListAuctions_NotLoggedIn_ReturnsFail() {
        AdminListAuctionsCommand cmd = new AdminListAuctionsCommand(manager);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_AUCTION_LIST_FAIL.name()),
                "Chua dang nhap phai tra ADMIN_AUCTION_LIST_FAIL.");
    }

    /**
     * AdminListAuctions khi là PARTICIPANT phải trả ADMIN_AUCTION_LIST_FAIL.
     */
    @Test
    void adminListAuctions_NonAdmin_ReturnsFail() {
        AdminListAuctionsCommand cmd = new AdminListAuctionsCommand(manager);

        String response = cmd.execute(null, participantSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_AUCTION_LIST_FAIL.name()),
                "Non-admin phai tra ADMIN_AUCTION_LIST_FAIL.");
    }

    /**
     * AdminListAuctions khi là ADMIN phải trả ADMIN_AUCTION_LIST.
     */
    @Test
    void adminListAuctions_Admin_ReturnsAuctionList() {
        AdminListAuctionsCommand cmd = new AdminListAuctionsCommand(manager);

        String response = cmd.execute(null, adminSession());

        assertNotNull(response, "Response khong duoc null.");
        assertTrue(response.contains(Protocol.Response.ADMIN_AUCTION_LIST.name()),
                "Admin phai tra ADMIN_AUCTION_LIST.");
    }

    // =========================================================================
    // AdminCancelAuctionCommand
    // =========================================================================

    /**
     * AdminCancelAuction khi chưa đăng nhập phải trả ADMIN_CANCEL_AUCTION_FAIL.
     */
    @Test
    void adminCancelAuction_NotLoggedIn_ReturnsFail() {
        AdminCancelAuctionCommand cmd = new AdminCancelAuctionCommand(manager);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_CANCEL_AUCTION_FAIL.name()),
                "Chua dang nhap phai tra ADMIN_CANCEL_AUCTION_FAIL.");
    }

    /**
     * AdminCancelAuction khi là PARTICIPANT phải trả ADMIN_CANCEL_AUCTION_FAIL.
     */
    @Test
    void adminCancelAuction_NonAdmin_ReturnsFail() {
        AdminCancelAuctionCommand cmd = new AdminCancelAuctionCommand(manager);

        String response = cmd.execute(null, participantSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_CANCEL_AUCTION_FAIL.name()),
                "Non-admin phai tra ADMIN_CANCEL_AUCTION_FAIL.");
    }

    /**
     * AdminCancelAuction với payload null phải trả ADMIN_CANCEL_AUCTION_FAIL.
     */
    @Test
    void adminCancelAuction_NullPayload_ReturnsFail() {
        AdminCancelAuctionCommand cmd = new AdminCancelAuctionCommand(manager);

        String response = cmd.execute(null, adminSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_CANCEL_AUCTION_FAIL.name()),
                "Payload null phai tra ADMIN_CANCEL_AUCTION_FAIL.");
    }

    /**
     * AdminCancelAuction với auctionId không tồn tại phải trả ADMIN_CANCEL_AUCTION_FAIL.
     */
    @Test
    void adminCancelAuction_UnknownId_ReturnsFail() {
        AdminCancelAuctionCommand cmd = new AdminCancelAuctionCommand(manager);
        JsonNode payload = auctionIdPayload("NONEXISTENT-999");

        String response = cmd.execute(payload, adminSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_CANCEL_AUCTION_FAIL.name()),
                "Id khong ton tai phai tra ADMIN_CANCEL_AUCTION_FAIL.");
    }

    /**
     * AdminCancelAuction với auctionId hợp lệ phải trả ADMIN_CANCEL_AUCTION_OK.
     */
    @Test
    void adminCancelAuction_ValidId_ReturnsOk() {
        String auctionId = createTestAuction();
        AdminCancelAuctionCommand cmd = new AdminCancelAuctionCommand(manager);
        JsonNode payload = auctionIdPayload(auctionId);

        String response = cmd.execute(payload, adminSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_CANCEL_AUCTION_OK.name()),
                "AuctionId hop le phai tra ADMIN_CANCEL_AUCTION_OK.");
    }

    // =========================================================================
    // AdminDeleteAuctionCommand
    // =========================================================================

    /**
     * AdminDeleteAuction khi chưa đăng nhập phải trả ADMIN_DELETE_AUCTION_FAIL.
     */
    @Test
    void adminDeleteAuction_NotLoggedIn_ReturnsFail() {
        AdminDeleteAuctionCommand cmd = new AdminDeleteAuctionCommand(manager);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_DELETE_AUCTION_FAIL.name()),
                "Chua dang nhap phai tra ADMIN_DELETE_AUCTION_FAIL.");
    }

    /**
     * AdminDeleteAuction khi là PARTICIPANT phải trả ADMIN_DELETE_AUCTION_FAIL.
     */
    @Test
    void adminDeleteAuction_NonAdmin_ReturnsFail() {
        AdminDeleteAuctionCommand cmd = new AdminDeleteAuctionCommand(manager);

        String response = cmd.execute(null, participantSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_DELETE_AUCTION_FAIL.name()),
                "Non-admin phai tra ADMIN_DELETE_AUCTION_FAIL.");
    }

    /**
     * AdminDeleteAuction với payload null phải trả ADMIN_DELETE_AUCTION_FAIL.
     */
    @Test
    void adminDeleteAuction_NullPayload_ReturnsFail() {
        AdminDeleteAuctionCommand cmd = new AdminDeleteAuctionCommand(manager);

        String response = cmd.execute(null, adminSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_DELETE_AUCTION_FAIL.name()),
                "Payload null phai tra ADMIN_DELETE_AUCTION_FAIL.");
    }

    /**
     * AdminDeleteAuction với auctionId không tồn tại phải trả ADMIN_DELETE_AUCTION_FAIL.
     */
    @Test
    void adminDeleteAuction_UnknownId_ReturnsFail() {
        AdminDeleteAuctionCommand cmd = new AdminDeleteAuctionCommand(manager);
        JsonNode payload = auctionIdPayload("NONEXISTENT-001");

        String response = cmd.execute(payload, adminSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_DELETE_AUCTION_FAIL.name()),
                "Id khong ton tai phai tra ADMIN_DELETE_AUCTION_FAIL.");
    }

    /**
     * AdminDeleteAuction với auctionId hợp lệ phải trả ADMIN_DELETE_AUCTION_OK.
     */
    @Test
    void adminDeleteAuction_ValidId_ReturnsOk() {
        String auctionId = createTestAuction();
        AdminDeleteAuctionCommand cmd = new AdminDeleteAuctionCommand(manager);
        JsonNode payload = auctionIdPayload(auctionId);

        String response = cmd.execute(payload, adminSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_DELETE_AUCTION_OK.name()),
                "AuctionId hop le phai tra ADMIN_DELETE_AUCTION_OK.");
    }

    // =========================================================================
    // AdminDeleteUserCommand
    // =========================================================================

    /**
     * AdminDeleteUser khi chưa đăng nhập phải trả ADMIN_DELETE_USER_FAIL.
     */
    @Test
    void adminDeleteUser_NotLoggedIn_ReturnsFail() {
        AdminDeleteUserCommand cmd = new AdminDeleteUserCommand(manager);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_DELETE_USER_FAIL.name()),
                "Chua dang nhap phai tra ADMIN_DELETE_USER_FAIL.");
    }

    /**
     * AdminDeleteUser khi là PARTICIPANT phải trả ADMIN_DELETE_USER_FAIL.
     */
    @Test
    void adminDeleteUser_NonAdmin_ReturnsFail() {
        AdminDeleteUserCommand cmd = new AdminDeleteUserCommand(manager);

        String response = cmd.execute(null, participantSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_DELETE_USER_FAIL.name()),
                "Non-admin phai tra ADMIN_DELETE_USER_FAIL.");
    }

    /**
     * AdminDeleteUser với payload null phải trả ADMIN_DELETE_USER_FAIL.
     */
    @Test
    void adminDeleteUser_NullPayload_ReturnsFail() {
        AdminDeleteUserCommand cmd = new AdminDeleteUserCommand(manager);

        String response = cmd.execute(null, adminSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_DELETE_USER_FAIL.name()),
                "Payload null phai tra ADMIN_DELETE_USER_FAIL.");
    }

    /**
     * Admin tự xóa chính mình phải trả ADMIN_DELETE_USER_FAIL.
     */
    @Test
    void adminDeleteUser_SelfDelete_ReturnsFail() {
        Admin admin = new Admin("adm", "adm@test.com", "pass");
        manager.registerUser(admin);
        ClientSession session = new ClientSession(NOOP_OBSERVER, manager);
        session.setCurrentUser(admin);
        AdminDeleteUserCommand cmd = new AdminDeleteUserCommand(manager);
        JsonNode payload = userIdPayload(admin.getId());

        String response = cmd.execute(payload, session);

        assertTrue(response.contains(Protocol.Response.ADMIN_DELETE_USER_FAIL.name()),
                "Tu xoa chinh minh phai tra ADMIN_DELETE_USER_FAIL.");
    }

    /**
     * Admin xóa admin khác phải trả ADMIN_DELETE_USER_FAIL.
     */
    @Test
    void adminDeleteUser_DeleteOtherAdmin_ReturnsFail() {
        Admin admin2 = new Admin("adm2", "adm2@test.com", "pass");
        manager.registerUser(admin2);
        AdminDeleteUserCommand cmd = new AdminDeleteUserCommand(manager);
        JsonNode payload = userIdPayload(admin2.getId());

        String response = cmd.execute(payload, adminSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_DELETE_USER_FAIL.name()),
                "Xoa admin khac phai tra ADMIN_DELETE_USER_FAIL.");
    }

    /**
     * AdminDeleteUser với userId không tồn tại phải trả ADMIN_DELETE_USER_FAIL.
     */
    @Test
    void adminDeleteUser_UnknownId_ReturnsFail() {
        AdminDeleteUserCommand cmd = new AdminDeleteUserCommand(manager);
        JsonNode payload = userIdPayload("UNKNOWN-USER-999");

        String response = cmd.execute(payload, adminSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_DELETE_USER_FAIL.name()),
                "UserId khong ton tai phai tra ADMIN_DELETE_USER_FAIL.");
    }

    /**
     * AdminDeleteUser với userId participant hợp lệ phải trả ADMIN_DELETE_USER_OK.
     */
    @Test
    void adminDeleteUser_ValidParticipantId_ReturnsOk() {
        Participant target = new Participant("target", "target@test.com", "pass", 0.0, "PARTICIPANT");
        manager.registerUser(target);
        AdminDeleteUserCommand cmd = new AdminDeleteUserCommand(manager);
        JsonNode payload = userIdPayload(target.getId());

        String response = cmd.execute(payload, adminSession());

        assertTrue(response.contains(Protocol.Response.ADMIN_DELETE_USER_OK.name()),
                "UserId participant hop le phai tra ADMIN_DELETE_USER_OK.");
    }
}