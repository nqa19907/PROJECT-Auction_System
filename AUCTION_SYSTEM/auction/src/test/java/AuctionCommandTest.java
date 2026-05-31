import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.items.Item;
import auction_system.common.models.items.builder.ElectronicBuilder;
import auction_system.common.models.users.Participant;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.auction.JoinAuctionCommand;
import auction_system.server.network.command.auction.LeaveAuctionCommand;
import auction_system.server.network.command.wallet.DepositCommand;
import auction_system.server.persistence.serialization.SerializedDatabase;
import auction_system.server.services.auth.AuthService;
import auction_system.server.services.autobid.AutoBidService;
import auction_system.server.services.bidding.AuctionBidService;
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
 * Kiểm thử các command đấu giá và ví: {@link JoinAuctionCommand},
 * {@link LeaveAuctionCommand}, {@link DepositCommand}.
 *
 * <p>Các nhóm test:
 * <ol>
 *   <li>JoinAuctionCommand — chưa đăng nhập, payload null, id sai, phiên kết thúc,
 *       thành công.</li>
 *   <li>LeaveAuctionCommand — payload null, id sai, thành công.</li>
 *   <li>DepositCommand — chưa đăng nhập, payload null, số tiền âm, số tiền hợp lệ.</li>
 * </ol>
 */
public class AuctionCommandTest {

    @TempDir
    Path tempDir;

    private AuctionManager manager;
    private SerializedDatabase database;
    private AuthService authService;
    private AuctionBidService bidService;

    /** Observer không làm gì, dùng để dựng ClientSession trong test. */
    private static final AuctionObserver NOOP_OBSERVER = msg -> { };

    @BeforeEach
    void setUp() throws Exception {
        resetSingleton();
        database = new SerializedDatabase(tempDir);
        manager = AuctionManager.getInstance(database);
        authService = new AuthService(database);
        bidService = new AuctionBidService(database, manager, new AutoBidService());
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

    /**
     * Tạo Participant mới, đăng ký vào hệ thống và trả về đối tượng.
     *
     * @return participant đã được đăng ký vào AuctionManager
     */
    private Participant makeParticipant() {
        Participant p = new Participant("part01", "part@test.com", "pass", 50_000.0, "PARTICIPANT");
        manager.registerUser(p);
        return p;
    }

    /**
     * Tạo ClientSession đã đăng nhập cho participant cho trước.
     *
     * @param user participant đã đăng nhập
     * @return session đã đặt currentUser
     */
    private ClientSession loggedInSession(final Participant user) {
        ClientSession session = new ClientSession(NOOP_OBSERVER, manager);
        session.setCurrentUser(user);
        return session;
    }

    /**
     * Tạo phiên đấu giá đang hoạt động và trả về ID.
     *
     * @param seller người bán hàng
     * @return ID phiên đấu giá đang hoạt động
     */
    private String createActiveAuction(final Participant seller) {
        Item item = new ElectronicBuilder()
                .itemName("Test Auction Item")
                .description("Mo ta test")
                .startPrice(1000.0)
                .sellerId(seller.getId())
                .build();
        Auction auction = manager.createAuction(
                item, seller,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(2));
        return auction.getId();
    }

    /**
     * Tạo phiên đấu giá đã kết thúc và trả về ID.
     *
     * @param seller người bán hàng
     * @return ID phiên đấu giá đã kết thúc
     */
    private String createFinishedAuction(final Participant seller) {
        Item item = new ElectronicBuilder()
                .itemName("Finished Item")
                .description("Mo ta ket thuc")
                .startPrice(500.0)
                .sellerId(seller.getId())
                .build();
        Auction auction = manager.createAuction(
                item, seller,
                LocalDateTime.now().minusHours(3),
                LocalDateTime.now().minusHours(1));
        auction.setStatus(AuctionStatus.FINISHED);
        return auction.getId();
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
     * Tạo payload JSON chứa số tiền nạp.
     *
     * @param amount số tiền (dạng chuỗi)
     * @return payload JSON
     */
    private JsonNode depositPayload(final String amount) {
        return JsonProtocol.payloadOf(Map.of("amount", amount));
    }

    // =========================================================================
    // JoinAuctionCommand
    // =========================================================================

    /**
     * JoinAuction khi chưa đăng nhập phải trả ERROR.
     */
    @Test
    void joinAuction_NotLoggedIn_ReturnsError() {
        JoinAuctionCommand cmd = new JoinAuctionCommand(manager);

        String response = cmd.execute(auctionIdPayload("any"), newSession());

        assertTrue(response.contains(Protocol.Response.ERROR.name()),
                "Chua dang nhap phai tra ERROR.");
    }

    /**
     * JoinAuction với payload null phải trả JOIN_FAIL.
     */
    @Test
    void joinAuction_NullPayload_ReturnsJoinFail() {
        Participant p = makeParticipant();
        JoinAuctionCommand cmd = new JoinAuctionCommand(manager);

        String response = cmd.execute(null, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.JOIN_FAIL.name()),
                "Payload null phai tra JOIN_FAIL.");
    }

    /**
     * JoinAuction với auctionId không tồn tại phải trả JOIN_FAIL.
     */
    @Test
    void joinAuction_UnknownAuctionId_ReturnsJoinFail() {
        Participant p = makeParticipant();
        JoinAuctionCommand cmd = new JoinAuctionCommand(manager);

        String response = cmd.execute(auctionIdPayload("NOTEXIST-999"), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.JOIN_FAIL.name()),
                "AuctionId khong ton tai phai tra JOIN_FAIL.");
    }

    /**
     * JoinAuction vào phiên đã kết thúc phải trả JOIN_FAIL.
     */
    @Test
    void joinAuction_FinishedAuction_ReturnsJoinFail() {
        Participant seller = makeParticipant();
        String auctionId = createFinishedAuction(seller);
        JoinAuctionCommand cmd = new JoinAuctionCommand(manager);

        String response = cmd.execute(auctionIdPayload(auctionId), loggedInSession(seller));

        assertTrue(response.contains(Protocol.Response.JOIN_FAIL.name()),
                "Phien da ket thuc phai tra JOIN_FAIL.");
    }

    /**
     * JoinAuction vào phiên đang hoạt động phải trả JOIN_OK.
     */
    @Test
    void joinAuction_ActiveAuction_ReturnsJoinOk() {
        Participant seller = makeParticipant();
        String auctionId = createActiveAuction(seller);
        JoinAuctionCommand cmd = new JoinAuctionCommand(manager);

        String response = cmd.execute(auctionIdPayload(auctionId), loggedInSession(seller));

        assertNotNull(response, "Response khong duoc null.");
        assertTrue(response.contains(Protocol.Response.JOIN_OK.name()),
                "Phien dang hoat dong phai tra JOIN_OK.");
    }

    /**
     * JoinAuction với auctionId rỗng phải trả JOIN_FAIL.
     */
    @Test
    void joinAuction_BlankAuctionId_ReturnsJoinFail() {
        Participant p = makeParticipant();
        JoinAuctionCommand cmd = new JoinAuctionCommand(manager);
        JsonNode payload = JsonProtocol.payloadOf(Map.of("auctionId", "   "));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.JOIN_FAIL.name()),
                "AuctionId blank phai tra JOIN_FAIL.");
    }

    // =========================================================================
    // LeaveAuctionCommand
    // =========================================================================

    /**
     * LeaveAuction với payload null phải trả ERROR.
     */
    @Test
    void leaveAuction_NullPayload_ReturnsError() {
        LeaveAuctionCommand cmd = new LeaveAuctionCommand(manager);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.ERROR.name()),
                "Payload null phai tra ERROR.");
    }

    /**
     * LeaveAuction với auctionId rỗng phải trả ERROR.
     */
    @Test
    void leaveAuction_BlankAuctionId_ReturnsError() {
        LeaveAuctionCommand cmd = new LeaveAuctionCommand(manager);
        JsonNode payload = JsonProtocol.payloadOf(Map.of("auctionId", "   "));

        String response = cmd.execute(payload, newSession());

        assertTrue(response.contains(Protocol.Response.ERROR.name()),
                "AuctionId blank phai tra ERROR.");
    }

    /**
     * LeaveAuction với auctionId hợp lệ (kể cả chưa join) phải trả LEAVE_OK.
     */
    @Test
    void leaveAuction_ValidId_ReturnsLeaveOk() {
        LeaveAuctionCommand cmd = new LeaveAuctionCommand(manager);

        String response = cmd.execute(auctionIdPayload("any-id"), newSession());

        assertTrue(response.contains(Protocol.Response.LEAVE_OK.name()),
                "AuctionId hop le phai tra LEAVE_OK.");
    }

    /**
     * LeaveAuction sau khi join thành công phải trả LEAVE_OK.
     */
    @Test
    void leaveAuction_AfterJoin_ReturnsLeaveOk() {
        Participant seller = makeParticipant();
        String auctionId = createActiveAuction(seller);
        ClientSession session = loggedInSession(seller);
        // Join trước
        new JoinAuctionCommand(manager).execute(auctionIdPayload(auctionId), session);
        LeaveAuctionCommand leaveCmd = new LeaveAuctionCommand(manager);

        String response = leaveCmd.execute(auctionIdPayload(auctionId), session);

        assertTrue(response.contains(Protocol.Response.LEAVE_OK.name()),
                "Sau khi join, leave phai tra LEAVE_OK.");
    }

    // =========================================================================
    // DepositCommand
    // =========================================================================

    /**
     * Deposit khi chưa đăng nhập phải trả DEPOSIT_FAIL.
     */
    @Test
    void depositCommand_NotLoggedIn_ReturnsDepositFail() {
        DepositCommand cmd = new DepositCommand(authService, bidService);

        String response = cmd.execute(depositPayload("10000"), newSession());

        assertTrue(response.contains(Protocol.Response.DEPOSIT_FAIL.name()),
                "Chua dang nhap phai tra DEPOSIT_FAIL.");
    }

    /**
     * Deposit với payload null phải trả DEPOSIT_FAIL.
     */
    @Test
    void depositCommand_NullPayload_ReturnsDepositFail() {
        Participant p = makeParticipant();
        DepositCommand cmd = new DepositCommand(authService, bidService);

        String response = cmd.execute(null, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.DEPOSIT_FAIL.name()),
                "Payload null phai tra DEPOSIT_FAIL.");
    }

    /**
     * Deposit với số tiền rỗng phải trả DEPOSIT_FAIL.
     */
    @Test
    void depositCommand_BlankAmount_ReturnsDepositFail() {
        Participant p = makeParticipant();
        DepositCommand cmd = new DepositCommand(authService, bidService);

        String response = cmd.execute(depositPayload("   "), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.DEPOSIT_FAIL.name()),
                "Amount blank phai tra DEPOSIT_FAIL.");
    }

    /**
     * Deposit với số tiền âm phải trả DEPOSIT_FAIL.
     */
    @Test
    void depositCommand_NegativeAmount_ReturnsDepositFail() {
        Participant p = makeParticipant();
        DepositCommand cmd = new DepositCommand(authService, bidService);

        String response = cmd.execute(depositPayload("-5000"), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.DEPOSIT_FAIL.name()),
                "Amount am phai tra DEPOSIT_FAIL.");
    }

    /**
     * Deposit với số tiền bằng 0 phải trả DEPOSIT_FAIL.
     */
    @Test
    void depositCommand_ZeroAmount_ReturnsDepositFail() {
        Participant p = makeParticipant();
        DepositCommand cmd = new DepositCommand(authService, bidService);

        String response = cmd.execute(depositPayload("0"), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.DEPOSIT_FAIL.name()),
                "Amount bang 0 phai tra DEPOSIT_FAIL.");
    }

    /**
     * Deposit với số tiền hợp lệ phải trả DEPOSIT_OK.
     */
    @Test
    void depositCommand_ValidAmount_ReturnsDepositOk() {
        Participant p = makeParticipant();
        DepositCommand cmd = new DepositCommand(authService, bidService);

        String response = cmd.execute(depositPayload("50000"), loggedInSession(p));

        assertNotNull(response, "Response khong duoc null.");
        assertTrue(response.contains(Protocol.Response.DEPOSIT_OK.name()),
                "Amount hop le phai tra DEPOSIT_OK.");
    }
}