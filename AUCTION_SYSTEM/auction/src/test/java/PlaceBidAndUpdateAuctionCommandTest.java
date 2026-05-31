import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.models.items.Item;
import auction_system.common.models.items.builder.ElectronicBuilder;
import auction_system.common.models.users.Admin;
import auction_system.common.models.users.Participant;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.bidding.PlaceBidCommand;
import auction_system.server.network.command.bidding.UpdateMyAuctionCommand;
import auction_system.server.persistence.serialization.SerializedDatabase;
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
 * Kiểm thử {@link PlaceBidCommand} và {@link UpdateMyAuctionCommand}.
 *
 * <p>Các nhóm test:
 * <ol>
 *   <li>PlaceBidCommand — chưa đăng nhập, payload null/thiếu field, số tiền không hợp lệ,
 *       admin đặt giá, phiên không tồn tại/đã kết thúc, số dư không đủ, thành công.</li>
 *   <li>UpdateMyAuctionCommand — chưa đăng nhập, payload null/thiếu field, số tiền không
 *       hợp lệ, phiên không tồn tại, không phải chủ sở hữu, thành công.</li>
 * </ol>
 */
public class PlaceBidAndUpdateAuctionCommandTest {

    @TempDir
    Path tempDir;

    private AuctionManager manager;
    private SerializedDatabase database;
    private AuctionBidService bidService;

    /** Observer không làm gì, dùng để dựng ClientSession trong test. */
    private static final AuctionObserver NOOP_OBSERVER = msg -> { };

    @BeforeEach
    void setUp() throws Exception {
        resetSingleton();
        database = new SerializedDatabase(tempDir);
        manager = AuctionManager.getInstance(database);
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
     * Tạo Participant, đăng ký vào hệ thống và trả về.
     *
     * @param username tên đăng nhập
     * @param email    địa chỉ email
     * @param balance  số dư ban đầu
     * @return participant đã đăng ký
     */
    private Participant makeParticipant(
            final String username,
            final String email,
            final double balance) {
        Participant p = new Participant(username, email, "pass", balance, "PARTICIPANT");
        manager.registerUser(p);
        return p;
    }

    /**
     * Tạo ClientSession đã đăng nhập cho participant.
     *
     * @param user participant đã đăng nhập
     * @return session có currentUser
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
     * @return ID phiên đấu giá
     */
    private String createActiveAuction(final Participant seller) {
        Item item = new ElectronicBuilder()
                .itemName("Test Item")
                .description("Mo ta san pham test")
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
                .description("Mo ta da ket thuc")
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
     * Tạo payload JSON đặt giá chứa auctionId và amount.
     *
     * @param auctionId mã phiên đấu giá
     * @param amount    số tiền đặt giá
     * @return payload JSON
     */
    private JsonNode bidPayload(final String auctionId, final String amount) {
        return JsonProtocol.payloadOf(Map.of("auctionId", auctionId, "amount", amount));
    }

    /**
     * Tạo payload cập nhật hợp lệ với auctionId cho trước.
     *
     * @param auctionId mã phiên đấu giá cần cập nhật
     * @return payload JSON hợp lệ
     */
    private JsonNode validUpdatePayload(final String auctionId) {
        return JsonProtocol.payloadOf(Map.of(
                "auctionId", auctionId,
                "category", "Electronic",
                "itemName", "Laptop Pro Updated",
                "description", "Mo ta cap nhat",
                "condition", "Moi 99%",
                "startPrice", "5500000",
                "bidStep", "100000",
                "startTime", LocalDateTime.now().plusMinutes(5).toString(),
                "endTime", LocalDateTime.now().plusHours(4).toString()));
    }

    // =========================================================================
    // PlaceBidCommand — chưa đăng nhập
    // =========================================================================

    /**
     * PlaceBid khi chưa đăng nhập phải trả ERROR.
     */
    @Test
    void placeBidCommand_NotLoggedIn_ReturnsError() {
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);

        String response = cmd.execute(bidPayload("any-id", "1000"), newSession());

        assertTrue(response.contains(Protocol.Response.ERROR.name()),
                "Chua dang nhap phai tra ERROR.");
    }

    // =========================================================================
    // PlaceBidCommand — payload lỗi
    // =========================================================================

    /**
     * PlaceBid với payload null phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_NullPayload_ReturnsBidFail() {
        Participant p = makeParticipant("bidder01", "bidder01@test.com", 50_000.0);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);

        String response = cmd.execute(null, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Payload null phai tra BID_FAIL.");
    }

    /**
     * PlaceBid thiếu auctionId trong payload phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_MissingAuctionId_ReturnsBidFail() {
        Participant p = makeParticipant("bidder02", "bidder02@test.com", 50_000.0);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);
        JsonNode payload = JsonProtocol.payloadOf(Map.of("amount", "1000"));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Thieu auctionId phai tra BID_FAIL.");
    }

    /**
     * PlaceBid thiếu amount trong payload phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_MissingAmount_ReturnsBidFail() {
        Participant p = makeParticipant("bidder03", "bidder03@test.com", 50_000.0);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);
        JsonNode payload = JsonProtocol.payloadOf(Map.of("auctionId", "some-id"));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Thieu amount phai tra BID_FAIL.");
    }

    // =========================================================================
    // PlaceBidCommand — số tiền không hợp lệ
    // =========================================================================

    /**
     * PlaceBid với amount rỗng phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_BlankAmount_ReturnsBidFail() {
        Participant p = makeParticipant("bidder04", "bidder04@test.com", 50_000.0);
        String auctionId = createActiveAuction(p);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);

        String response = cmd.execute(bidPayload(auctionId, "   "), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Amount blank phai tra BID_FAIL.");
    }

    /**
     * PlaceBid với amount âm phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_NegativeAmount_ReturnsBidFail() {
        Participant p = makeParticipant("bidder05", "bidder05@test.com", 50_000.0);
        String auctionId = createActiveAuction(p);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);

        String response = cmd.execute(bidPayload(auctionId, "-500"), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Amount am phai tra BID_FAIL.");
    }

    /**
     * PlaceBid với amount bằng 0 phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_ZeroAmount_ReturnsBidFail() {
        Participant p = makeParticipant("bidder06", "bidder06@test.com", 50_000.0);
        String auctionId = createActiveAuction(p);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);

        String response = cmd.execute(bidPayload(auctionId, "0"), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Amount bang 0 phai tra BID_FAIL.");
    }

    /**
     * PlaceBid với amount không phải số phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_NonNumericAmount_ReturnsBidFail() {
        Participant p = makeParticipant("bidder07", "bidder07@test.com", 50_000.0);
        String auctionId = createActiveAuction(p);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);

        String response = cmd.execute(bidPayload(auctionId, "abc"), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Amount khong phai so phai tra BID_FAIL.");
    }

    // =========================================================================
    // PlaceBidCommand — role và phiên không hợp lệ
    // =========================================================================

    /**
     * Admin đặt giá phải trả BID_FAIL (chỉ Participant được phép đặt giá).
     */
    @Test
    void placeBidCommand_AdminUser_ReturnsBidFail() {
        Admin admin = new Admin("admin01", "admin@test.com", "pass");
        ClientSession session = new ClientSession(NOOP_OBSERVER, manager);
        session.setCurrentUser(admin);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);

        String response = cmd.execute(bidPayload("any-id", "1000"), session);

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Admin dat gia phai tra BID_FAIL.");
    }

    /**
     * PlaceBid với auctionId không tồn tại phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_UnknownAuctionId_ReturnsBidFail() {
        Participant p = makeParticipant("bidder08", "bidder08@test.com", 50_000.0);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);

        String response = cmd.execute(bidPayload("NOTEXIST-999", "2000"), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "AuctionId khong ton tai phai tra BID_FAIL.");
    }

    /**
     * PlaceBid vào phiên đã kết thúc phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_FinishedAuction_ReturnsBidFail() {
        Participant p = makeParticipant("bidder09", "bidder09@test.com", 50_000.0);
        String auctionId = createFinishedAuction(p);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);

        String response = cmd.execute(bidPayload(auctionId, "2000"), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Phien da ket thuc phai tra BID_FAIL.");
    }

    /**
     * PlaceBid khi số dư nhỏ hơn giá đặt phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_InsufficientBalance_ReturnsBidFail() {
        Participant seller = makeParticipant("seller01", "seller01@test.com", 50_000.0);
        Participant poorBidder = new Participant("poor01", "poor01@test.com", "pass",
                100.0, "PARTICIPANT");
        manager.registerUser(poorBidder);
        String auctionId = createActiveAuction(seller);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);

        String response = cmd.execute(bidPayload(auctionId, "10000"), loggedInSession(poorBidder));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "So du khong du phai tra BID_FAIL.");
    }

    // =========================================================================
    // PlaceBidCommand — thành công
    // =========================================================================

    /**
     * PlaceBid hợp lệ với đủ số dư phải trả BID_OK.
     */
    @Test
    void placeBidCommand_ValidBid_ReturnsBidOk() {
        Participant seller = makeParticipant("seller02", "seller02@test.com", 50_000.0);
        Participant richBidder = new Participant("rich01", "rich01@test.com", "pass",
                100_000.0, "PARTICIPANT");
        manager.registerUser(richBidder);
        String auctionId = createActiveAuction(seller);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);

        String response = cmd.execute(bidPayload(auctionId, "2000"), loggedInSession(richBidder));

        assertNotNull(response, "Response khong duoc null.");
        assertTrue(response.contains(Protocol.Response.BID_OK.name()),
                "Dat gia hop le phai tra BID_OK.");
    }

    /**
     * PlaceBid hợp lệ phải có newBalance trong response.
     */
    @Test
    void placeBidCommand_ValidBid_ResponseContainsNewBalance() {
        Participant seller = makeParticipant("seller03", "seller03@test.com", 50_000.0);
        Participant richBidder = new Participant("rich02", "rich02@test.com", "pass",
                100_000.0, "PARTICIPANT");
        manager.registerUser(richBidder);
        String auctionId = createActiveAuction(seller);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);

        String response = cmd.execute(bidPayload(auctionId, "2000"), loggedInSession(richBidder));

        assertTrue(response.contains("newBalance"),
                "Response BID_OK phai chua newBalance.");
    }

    // =========================================================================
    // UpdateMyAuctionCommand — chưa đăng nhập
    // =========================================================================

    /**
     * UpdateMyAuction khi chưa đăng nhập phải trả UPDATE_MY_AUCTION_FAIL.
     */
    @Test
    void updateMyAuctionCommand_NotLoggedIn_ReturnsFail() {
        UpdateMyAuctionCommand cmd = new UpdateMyAuctionCommand(manager);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.UPDATE_MY_AUCTION_FAIL.name()),
                "Chua dang nhap phai tra UPDATE_MY_AUCTION_FAIL.");
    }

    // =========================================================================
    // UpdateMyAuctionCommand — payload lỗi
    // =========================================================================

    /**
     * UpdateMyAuction với payload null phải trả UPDATE_MY_AUCTION_FAIL.
     */
    @Test
    void updateMyAuctionCommand_NullPayload_ReturnsFail() {
        Participant p = makeParticipant("owner01", "owner01@test.com", 50_000.0);
        UpdateMyAuctionCommand cmd = new UpdateMyAuctionCommand(manager);

        String response = cmd.execute(null, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.UPDATE_MY_AUCTION_FAIL.name()),
                "Payload null phai tra UPDATE_MY_AUCTION_FAIL.");
    }

    /**
     * UpdateMyAuction thiếu itemName phải trả UPDATE_MY_AUCTION_FAIL.
     */
    @Test
    void updateMyAuctionCommand_MissingItemName_ReturnsFail() {
        Participant p = makeParticipant("owner02", "owner02@test.com", 50_000.0);
        String auctionId = createActiveAuction(p);
        UpdateMyAuctionCommand cmd = new UpdateMyAuctionCommand(manager);
        JsonNode payload = JsonProtocol.payloadOf(Map.of(
                "auctionId", auctionId,
                "category", "Electronic",
                "description", "Mo ta",
                "condition", "Moi",
                "startPrice", "1000000",
                "bidStep", "100000",
                "startTime", LocalDateTime.now().plusMinutes(5).toString(),
                "endTime", LocalDateTime.now().plusHours(3).toString()));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.UPDATE_MY_AUCTION_FAIL.name()),
                "Thieu itemName phai tra UPDATE_MY_AUCTION_FAIL.");
    }

    /**
     * UpdateMyAuction thiếu auctionId phải trả UPDATE_MY_AUCTION_FAIL.
     */
    @Test
    void updateMyAuctionCommand_MissingAuctionId_ReturnsFail() {
        Participant p = makeParticipant("owner03", "owner03@test.com", 50_000.0);
        UpdateMyAuctionCommand cmd = new UpdateMyAuctionCommand(manager);
        JsonNode payload = JsonProtocol.payloadOf(Map.of(
                "category", "Electronic",
                "itemName", "Laptop",
                "description", "Mo ta",
                "condition", "Moi",
                "startPrice", "1000000",
                "bidStep", "100000",
                "startTime", LocalDateTime.now().plusMinutes(5).toString(),
                "endTime", LocalDateTime.now().plusHours(3).toString()));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.UPDATE_MY_AUCTION_FAIL.name()),
                "Thieu auctionId phai tra UPDATE_MY_AUCTION_FAIL.");
    }

    // =========================================================================
    // UpdateMyAuctionCommand — số tiền không hợp lệ
    // =========================================================================

    /**
     * UpdateMyAuction với startPrice âm phải trả UPDATE_MY_AUCTION_FAIL.
     */
    @Test
    void updateMyAuctionCommand_NegativeStartPrice_ReturnsFail() {
        Participant p = makeParticipant("owner04", "owner04@test.com", 50_000.0);
        String auctionId = createActiveAuction(p);
        UpdateMyAuctionCommand cmd = new UpdateMyAuctionCommand(manager);
        JsonNode payload = JsonProtocol.payloadOf(Map.of(
                "auctionId", auctionId,
                "category", "Electronic",
                "itemName", "Laptop",
                "description", "Mo ta",
                "condition", "Moi",
                "startPrice", "-1000",
                "bidStep", "100000",
                "startTime", LocalDateTime.now().plusMinutes(5).toString(),
                "endTime", LocalDateTime.now().plusHours(3).toString()));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.UPDATE_MY_AUCTION_FAIL.name()),
                "StartPrice am phai tra UPDATE_MY_AUCTION_FAIL.");
    }

    /**
     * UpdateMyAuction với bidStep bằng 0 phải trả UPDATE_MY_AUCTION_FAIL.
     */
    @Test
    void updateMyAuctionCommand_ZeroBidStep_ReturnsFail() {
        Participant p = makeParticipant("owner05", "owner05@test.com", 50_000.0);
        String auctionId = createActiveAuction(p);
        UpdateMyAuctionCommand cmd = new UpdateMyAuctionCommand(manager);
        JsonNode payload = JsonProtocol.payloadOf(Map.of(
                "auctionId", auctionId,
                "category", "Electronic",
                "itemName", "Laptop",
                "description", "Mo ta",
                "condition", "Moi",
                "startPrice", "1000000",
                "bidStep", "0",
                "startTime", LocalDateTime.now().plusMinutes(5).toString(),
                "endTime", LocalDateTime.now().plusHours(3).toString()));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.UPDATE_MY_AUCTION_FAIL.name()),
                "BidStep bang 0 phai tra UPDATE_MY_AUCTION_FAIL.");
    }

    // =========================================================================
    // UpdateMyAuctionCommand — phiên không tồn tại / không phải của mình
    // =========================================================================

    /**
     * UpdateMyAuction với auctionId không tồn tại phải trả UPDATE_MY_AUCTION_FAIL.
     */
    @Test
    void updateMyAuctionCommand_UnknownAuctionId_ReturnsFail() {
        Participant p = makeParticipant("owner06", "owner06@test.com", 50_000.0);
        UpdateMyAuctionCommand cmd = new UpdateMyAuctionCommand(manager);

        String response = cmd.execute(validUpdatePayload("NOTEXIST-999"), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.UPDATE_MY_AUCTION_FAIL.name()),
                "AuctionId khong ton tai phai tra UPDATE_MY_AUCTION_FAIL.");
    }

    /**
     * UpdateMyAuction phiên không phải của mình phải trả UPDATE_MY_AUCTION_FAIL.
     */
    @Test
    void updateMyAuctionCommand_NotOwner_ReturnsFail() {
        Participant owner = makeParticipant("owner07", "owner07@test.com", 50_000.0);
        Participant other = new Participant("other01", "other01@test.com", "pass",
                10_000.0, "PARTICIPANT");
        manager.registerUser(other);
        String auctionId = createActiveAuction(owner);
        UpdateMyAuctionCommand cmd = new UpdateMyAuctionCommand(manager);

        String response = cmd.execute(validUpdatePayload(auctionId), loggedInSession(other));

        assertTrue(response.contains(Protocol.Response.UPDATE_MY_AUCTION_FAIL.name()),
                "Khong phai chu so huu phai tra UPDATE_MY_AUCTION_FAIL.");
    }

    // =========================================================================
    // UpdateMyAuctionCommand — thành công
    // =========================================================================

    /**
     * UpdateMyAuction với đầy đủ dữ liệu hợp lệ phải trả UPDATE_MY_AUCTION_OK.
     */
    @Test
    void updateMyAuctionCommand_ValidPayload_ReturnsOk() {
        Participant p = makeParticipant("owner08", "owner08@test.com", 50_000.0);
        String auctionId = createActiveAuction(p);
        UpdateMyAuctionCommand cmd = new UpdateMyAuctionCommand(manager);

        String response = cmd.execute(validUpdatePayload(auctionId), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.UPDATE_MY_AUCTION_OK.name()),
                "Payload hop le phai tra UPDATE_MY_AUCTION_OK.");
    }
}