import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.items.Item;
import auction_system.common.models.items.builder.ElectronicBuilder;
import auction_system.common.models.users.Admin;
import auction_system.common.models.users.Participant;
import auction_system.common.models.auctions.AuctionStatus;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.auction.GetAuctionCommand;
import auction_system.server.network.command.auction.ListAuctionsCommand;
import auction_system.server.network.command.bidding.AutoBidCommand;
import auction_system.server.network.command.bidding.PlaceBidCommand;
import auction_system.server.network.command.bidding.DeleteMyAuctionCommand;
import auction_system.server.network.command.bidding.DisableAutoBidCommand;
import auction_system.server.network.command.bidding.GetAutoBidStatusCommand;
import auction_system.server.network.command.bidding.GetBidHistoryCommand;
import auction_system.server.network.command.bidding.ListMyAuctionsCommand;
import auction_system.server.network.command.bidding.PublishItemCommand;
import auction_system.server.network.command.bidding.SetAntiSnipingCommand;
import auction_system.server.persistence.serialization.SerializedDatabase;
import auction_system.server.services.auction.ParticipantItemService;
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
 * Kiểm thử các command bidding và query auction:
 * {@link AutoBidCommand}, {@link DisableAutoBidCommand},
 * {@link GetAutoBidStatusCommand}, {@link GetBidHistoryCommand},
 * {@link ListMyAuctionsCommand}, {@link DeleteMyAuctionCommand},
 * {@link PublishItemCommand}, {@link SetAntiSnipingCommand},
 * {@link GetAuctionCommand}, {@link ListAuctionsCommand}.
 *
 * <p>Các nhóm test:
 * <ol>
 *   <li>AutoBidCommand — chưa đăng nhập, payload null/thiếu, maxAmount âm.</li>
 *   <li>DisableAutoBidCommand — chưa đăng nhập, payload null.</li>
 *   <li>GetAutoBidStatusCommand — non-participant, payload null trả disabled.</li>
 *   <li>GetBidHistoryCommand — payload null, id không tồn tại.</li>
 *   <li>ListMyAuctionsCommand — chưa đăng nhập, đã đăng nhập.</li>
 *   <li>DeleteMyAuctionCommand — chưa đăng nhập, id không phải của mình, thành công.</li>
 *   <li>PublishItemCommand — chưa đăng nhập, admin đăng bán, payload thiếu.</li>
 *   <li>SetAntiSnipingCommand — chưa đăng nhập, payload thiếu, id sai.</li>
 *   <li>GetAuctionCommand — payload null, id sai, id hợp lệ.</li>
 *   <li>ListAuctionsCommand — luôn trả AUCTION_LIST.</li>
 * </ol>
 */
public class BiddingCommandTest {

    @TempDir
    Path tempDir;

    private AuctionManager manager;
    private SerializedDatabase database;
    private AutoBidService autoBidService;
    private AuctionBidService bidService;
    private ParticipantItemService participantItemService;

    /**
     * Observer không làm gì, dùng để dựng ClientSession trong test.
     */
    private static final AuctionObserver NOOP_OBSERVER = msg -> {
    };

    @BeforeEach
    void setUp() throws Exception {
        resetSingleton();
        database = new SerializedDatabase(tempDir);
        manager = AuctionManager.getInstance(database);
        autoBidService = new AutoBidService();
        bidService = new AuctionBidService(database, manager, autoBidService);
        participantItemService = new ParticipantItemService(database);
    }

    @AfterEach
    void tearDown() throws Exception {
        manager.shutdown();
        resetSingleton();
    }

    /**
     * Reset AuctionManager singleton để các test độc lập nhau.
     */
    private void resetSingleton() throws Exception {
        Field field = AuctionManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }

    /**
     * Tạo ClientSession mới chưa đăng nhập.
     */
    private ClientSession newSession() {
        return new ClientSession(NOOP_OBSERVER, manager);
    }

    /**
     * Tạo ClientSession đã đăng nhập với participant.
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
     * Tạo Participant mới và đăng ký vào AuctionManager.
     *
     * @return participant đã được đăng ký
     */
    private Participant makeParticipant() {
        Participant p = new Participant("seller01", "seller@test.com", "pass", 5000.0, "PARTICIPANT");
        manager.registerUser(p);
        return p;
    }

    /**
     * Tạo phiên đấu giá đang chạy và trả về ID.
     *
     * @param seller người bán hàng
     * @return ID phiên đấu giá
     */
    private String createAuction(final Participant seller) {
        Item item = new ElectronicBuilder()
                .itemName("TestItem")
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
     * Tạo payload JSON chứa auctionId.
     *
     * @param auctionId mã phiên đấu giá
     * @return payload JSON
     */
    private JsonNode auctionIdPayload(final String auctionId) {
        return JsonProtocol.payloadOf(Map.of("auctionId", auctionId));
    }

    // =========================================================================
    // AutoBidCommand
    // =========================================================================

    /**
     * AutoBid khi chưa đăng nhập phải trả AUTO_BID_FAIL.
     */
    @Test
    void autoBidCommand_NotLoggedIn_ReturnsFail() {
        AutoBidCommand cmd = new AutoBidCommand(autoBidService, bidService);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.AUTO_BID_FAIL.name()),
                "Chua dang nhap phai tra AUTO_BID_FAIL.");
    }

    /**
     * AutoBid với payload null phải trả AUTO_BID_FAIL.
     */
    @Test
    void autoBidCommand_NullPayload_ReturnsFail() {
        Participant p = makeParticipant();
        AutoBidCommand cmd = new AutoBidCommand(autoBidService, bidService);

        String response = cmd.execute(null, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.AUTO_BID_FAIL.name()),
                "Payload null phai tra AUTO_BID_FAIL.");
    }

    /**
     * AutoBid thiếu auctionId phải trả AUTO_BID_FAIL.
     */
    @Test
    void autoBidCommand_BlankAuctionId_ReturnsFail() {
        Participant p = makeParticipant();
        AutoBidCommand cmd = new AutoBidCommand(autoBidService, bidService);
        JsonNode payload = JsonProtocol.payloadOf(
                Map.of("auctionId", "   ", "maxAmount", "5000", "stepAmount", "100"));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.AUTO_BID_FAIL.name()),
                "AuctionId blank phai tra AUTO_BID_FAIL.");
    }

    /**
     * AutoBid với maxAmount âm phải trả AUTO_BID_FAIL.
     */
    @Test
    void autoBidCommand_NegativeMaxAmount_ReturnsFail() {
        Participant p = makeParticipant();
        String auctionId = createAuction(p);
        AutoBidCommand cmd = new AutoBidCommand(autoBidService, bidService);
        JsonNode payload = JsonProtocol.payloadOf(
                Map.of("auctionId", auctionId, "maxAmount", "-1000", "stepAmount", "100"));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.AUTO_BID_FAIL.name()),
                "MaxAmount am phai tra AUTO_BID_FAIL.");
    }

    /**
     * AutoBid hợp lệ phải trả AUTO_BID_OK.
     */
    @Test
    void autoBidCommand_ValidPayload_ReturnsOk() {
        Participant p = makeParticipant();
        String auctionId = createAuction(p);
        AutoBidCommand cmd = new AutoBidCommand(autoBidService, bidService);
        JsonNode payload = JsonProtocol.payloadOf(
                Map.of("auctionId", auctionId, "maxAmount", "5000", "stepAmount", "100"));

        String response = cmd.execute(payload, loggedInSession(p));

        assertNotNull(response, "Response khong duoc null.");
        assertTrue(response.contains(Protocol.Response.AUTO_BID_OK.name()),
                "Payload hop le phai tra AUTO_BID_OK.");
    }

    // =========================================================================
    // DisableAutoBidCommand
    // =========================================================================

    /**
     * DisableAutoBid khi chưa đăng nhập phải trả AUTO_BID_FAIL.
     */
    @Test
    void disableAutoBidCommand_NotLoggedIn_ReturnsFail() {
        DisableAutoBidCommand cmd = new DisableAutoBidCommand(autoBidService);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.AUTO_BID_FAIL.name()),
                "Chua dang nhap phai tra AUTO_BID_FAIL.");
    }

    /**
     * DisableAutoBid với payload null phải trả AUTO_BID_FAIL.
     */
    @Test
    void disableAutoBidCommand_NullPayload_ReturnsFail() {
        Participant p = makeParticipant();
        DisableAutoBidCommand cmd = new DisableAutoBidCommand(autoBidService);

        String response = cmd.execute(null, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.AUTO_BID_FAIL.name()),
                "Payload null phai tra AUTO_BID_FAIL.");
    }

    /**
     * DisableAutoBid với auctionId hợp lệ phải trả AUTO_BID_OK.
     */
    @Test
    void disableAutoBidCommand_ValidId_ReturnsOk() {
        Participant p = makeParticipant();
        String auctionId = createAuction(p);

        // Bước 1: Bật Auto Bid lên trước
        AutoBidCommand enableCmd = new AutoBidCommand(autoBidService, bidService);
        JsonNode enablePayload = JsonProtocol.payloadOf(
                Map.of("auctionId", auctionId, "maxAmount", "5000", "stepAmount", "100"));
        enableCmd.execute(enablePayload, loggedInSession(p));

        // Bước 2: tẮT Auto Bid vừa bật
        DisableAutoBidCommand cmd = new DisableAutoBidCommand(autoBidService);
        String response = cmd.execute(auctionIdPayload(auctionId), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.AUTO_BID_OK.name()),
                "AuctionId hop le phai tra AUTO_BID_OK.");
    }

    // =========================================================================
    // GetAutoBidStatusCommand
    // =========================================================================

    /**
     * GetAutoBidStatus khi user là Admin (không phải Participant) phải trả trạng thái disabled.
     */
    @Test
    void getAutoBidStatusCommand_AdminUser_ReturnsDisabledStatus() {
        Admin admin = new Admin("adm", "adm@test.com", "pass");
        ClientSession session = new ClientSession(NOOP_OBSERVER, manager);
        session.setCurrentUser(admin);
        GetAutoBidStatusCommand cmd = new GetAutoBidStatusCommand(autoBidService);

        String response = cmd.execute(auctionIdPayload("any"), session);

        assertNotNull(response, "Response khong duoc null.");
        assertTrue(response.contains(Protocol.Response.AUTO_BID_STATUS.name()),
                "Admin phai tra AUTO_BID_STATUS (disabled).");
    }

    /**
     * GetAutoBidStatus với payload null phải trả AUTO_BID_STATUS (disabled).
     */
    @Test
    void getAutoBidStatusCommand_NullPayload_ReturnsDisabledStatus() {
        Participant p = makeParticipant();
        GetAutoBidStatusCommand cmd = new GetAutoBidStatusCommand(autoBidService);

        String response = cmd.execute(null, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.AUTO_BID_STATUS.name()),
                "Payload null phai tra AUTO_BID_STATUS disabled.");
    }

    /**
     * GetAutoBidStatus chưa enable phải trả AUTO_BID_STATUS chứa enabled:false.
     */
    @Test
    void getAutoBidStatusCommand_NotEnabled_ReturnsDisabled() {
        Participant p = makeParticipant();
        String auctionId = createAuction(p);
        GetAutoBidStatusCommand cmd = new GetAutoBidStatusCommand(autoBidService);

        String response = cmd.execute(auctionIdPayload(auctionId), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.AUTO_BID_STATUS.name()),
                "Chua enable auto-bid phai tra AUTO_BID_STATUS.");
        assertTrue(response.contains("false"),
                "Chua enable auto-bid phai co enabled:false.");
    }

    // =========================================================================
    // GetBidHistoryCommand
    // =========================================================================

    /**
     * GetBidHistory với payload null phải trả ERROR.
     */
    @Test
    void getBidHistoryCommand_NullPayload_ReturnsError() {
        GetBidHistoryCommand cmd = new GetBidHistoryCommand(bidService);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.ERROR.name()),
                "Payload null phai tra ERROR.");
    }

    /**
     * GetBidHistory với auctionId rỗng phải trả ERROR.
     */
    @Test
    void getBidHistoryCommand_BlankId_ReturnsError() {
        GetBidHistoryCommand cmd = new GetBidHistoryCommand(bidService);
        JsonNode payload = JsonProtocol.payloadOf(Map.of("auctionId", "   "));

        String response = cmd.execute(payload, newSession());

        assertTrue(response.contains(Protocol.Response.ERROR.name()),
                "AuctionId blank phai tra ERROR.");
    }

    /**
     * GetBidHistory với auctionId hợp lệ (kể cả chưa có bid) phải trả BID_HISTORY.
     */
    @Test
    void getBidHistoryCommand_ValidId_ReturnsBidHistory() {
        Participant p = makeParticipant();
        String auctionId = createAuction(p);
        GetBidHistoryCommand cmd = new GetBidHistoryCommand(bidService);

        String response = cmd.execute(auctionIdPayload(auctionId), newSession());

        assertTrue(response.contains(Protocol.Response.BID_HISTORY.name()),
                "AuctionId hop le phai tra BID_HISTORY.");
    }

    // =========================================================================
    // ListMyAuctionsCommand
    // =========================================================================

    /**
     * ListMyAuctions khi chưa đăng nhập phải trả ERROR.
     */
    @Test
    void listMyAuctionsCommand_NotLoggedIn_ReturnsError() {
        ListMyAuctionsCommand cmd = new ListMyAuctionsCommand(manager);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.ERROR.name()),
                "Chua dang nhap phai tra ERROR.");
    }

    /**
     * ListMyAuctions khi đã đăng nhập phải trả MY_AUCTION_LIST.
     */
    @Test
    void listMyAuctionsCommand_LoggedIn_ReturnsMyAuctionList() {
        Participant p = makeParticipant();
        ListMyAuctionsCommand cmd = new ListMyAuctionsCommand(manager);

        String response = cmd.execute(null, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.MY_AUCTION_LIST.name()),
                "Da dang nhap phai tra MY_AUCTION_LIST.");
    }

    // =========================================================================
    // DeleteMyAuctionCommand
    // =========================================================================

    /**
     * DeleteMyAuction khi chưa đăng nhập phải trả DELETE_MY_AUCTION_FAIL.
     */
    @Test
    void deleteMyAuctionCommand_NotLoggedIn_ReturnsFail() {
        DeleteMyAuctionCommand cmd = new DeleteMyAuctionCommand(manager);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.DELETE_MY_AUCTION_FAIL.name()),
                "Chua dang nhap phai tra DELETE_MY_AUCTION_FAIL.");
    }

    /**
     * DeleteMyAuction với payload null phải trả DELETE_MY_AUCTION_FAIL.
     */
    @Test
    void deleteMyAuctionCommand_NullPayload_ReturnsFail() {
        Participant p = makeParticipant();
        DeleteMyAuctionCommand cmd = new DeleteMyAuctionCommand(manager);

        String response = cmd.execute(null, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.DELETE_MY_AUCTION_FAIL.name()),
                "Payload null phai tra DELETE_MY_AUCTION_FAIL.");
    }

    /**
     * DeleteMyAuction với id không tồn tại phải trả DELETE_MY_AUCTION_FAIL.
     */
    @Test
    void deleteMyAuctionCommand_UnknownId_ReturnsFail() {
        Participant p = makeParticipant();
        DeleteMyAuctionCommand cmd = new DeleteMyAuctionCommand(manager);

        String response = cmd.execute(auctionIdPayload("NOTEXIST-001"), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.DELETE_MY_AUCTION_FAIL.name()),
                "Id khong ton tai phai tra DELETE_MY_AUCTION_FAIL.");
    }

    /**
     * DeleteMyAuction auction của người khác phải trả DELETE_MY_AUCTION_FAIL.
     */
    @Test
    void deleteMyAuctionCommand_NotOwner_ReturnsFail() {
        Participant owner = makeParticipant();
        Participant other = new Participant("other", "other@test.com", "pass", 0.0, "PARTICIPANT");
        manager.registerUser(other);
        String auctionId = createAuction(owner);
        DeleteMyAuctionCommand cmd = new DeleteMyAuctionCommand(manager);

        ClientSession otherSession = loggedInSession(other);
        String response = cmd.execute(auctionIdPayload(auctionId), otherSession);

        assertTrue(response.contains(Protocol.Response.DELETE_MY_AUCTION_FAIL.name()),
                "Khong phai chu so huu phai tra DELETE_MY_AUCTION_FAIL.");
    }

    /**
     * DeleteMyAuction auction của chính mình phải trả DELETE_MY_AUCTION_OK.
     */
    @Test
    void deleteMyAuctionCommand_OwnAuction_ReturnsOk() {
        Participant p = makeParticipant();
        String auctionId = createAuction(p);
        DeleteMyAuctionCommand cmd = new DeleteMyAuctionCommand(manager);

        String response = cmd.execute(auctionIdPayload(auctionId), loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.DELETE_MY_AUCTION_OK.name()),
                "Xoa phien cua chinh minh phai tra DELETE_MY_AUCTION_OK.");
    }

    // =========================================================================
    // PublishItemCommand
    // =========================================================================

    /**
     * PublishItem khi chưa đăng nhập phải trả PUBLISH_ITEM_FAIL.
     */
    @Test
    void publishItemCommand_NotLoggedIn_ReturnsFail() {
        PublishItemCommand cmd = new PublishItemCommand(participantItemService, manager);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.PUBLISH_ITEM_FAIL.name()),
                "Chua dang nhap phai tra PUBLISH_ITEM_FAIL.");
    }

    /**
     * PublishItem khi user là Admin phải trả PUBLISH_ITEM_FAIL.
     */
    @Test
    void publishItemCommand_AdminUser_ReturnsFail() {
        Admin admin = new Admin("adm", "adm@test.com", "pass");
        ClientSession session = new ClientSession(NOOP_OBSERVER, manager);
        session.setCurrentUser(admin);
        PublishItemCommand cmd = new PublishItemCommand(participantItemService, manager);
        JsonNode payload = JsonProtocol.payloadOf(Map.of(
                "category", "Electronic", "itemName", "Laptop",
                "description", "Mo ta", "condition", "Tot",
                "startPrice", "1000", "bidStep", "100",
                "startTime", LocalDateTime.now().plusMinutes(1).toString(),
                "endTime", LocalDateTime.now().plusHours(2).toString()));

        String response = cmd.execute(payload, session);

        assertTrue(response.contains(Protocol.Response.PUBLISH_ITEM_FAIL.name()),
                "Admin khong duoc dang ban phai tra PUBLISH_ITEM_FAIL.");
    }

    /**
     * PublishItem với payload thiếu field bắt buộc phải trả PUBLISH_ITEM_FAIL.
     */
    @Test
    void publishItemCommand_MissingFields_ReturnsFail() {
        Participant p = makeParticipant();
        PublishItemCommand cmd = new PublishItemCommand(participantItemService, manager);
        JsonNode payload = JsonProtocol.payloadOf(Map.of("category", "Electronic"));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.PUBLISH_ITEM_FAIL.name()),
                "Thieu field bat buoc phai tra PUBLISH_ITEM_FAIL.");
    }

    /**
     * PublishItem hợp lệ phải trả PUBLISH_ITEM_OK.
     */
    @Test
    void publishItemCommand_ValidPayload_ReturnsOk() {
        Participant p = makeParticipant();
        PublishItemCommand cmd = new PublishItemCommand(participantItemService, manager);
        JsonNode payload = JsonProtocol.payloadOf(Map.of(
                "category", "Electronic", "itemName", "Laptop Pro",
                "description", "Mo ta san pham", "condition", "Moi",
                "startPrice", "5000000", "bidStep", "100000",
                "startTime", LocalDateTime.now().plusMinutes(5).toString(),
                "endTime", LocalDateTime.now().plusHours(3).toString()));

        String response = cmd.execute(payload, loggedInSession(p));

        assertNotNull(response, "Response khong duoc null.");
        assertTrue(response.contains(Protocol.Response.PUBLISH_ITEM_OK.name()),
                "Payload hop le phai tra PUBLISH_ITEM_OK.");
    }

    // =========================================================================
    // SetAntiSnipingCommand
    // =========================================================================

    /**
     * SetAntiSniping khi chưa đăng nhập phải trả ANTI_SNIPING_UPDATE_FAIL.
     */
    @Test
    void setAntiSnipingCommand_NotLoggedIn_ReturnsFail() {
        SetAntiSnipingCommand cmd = new SetAntiSnipingCommand(manager);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.ANTI_SNIPING_UPDATE_FAIL.name()),
                "Chua dang nhap phai tra ANTI_SNIPING_UPDATE_FAIL.");
    }

    /**
     * SetAntiSniping với payload null phải trả ANTI_SNIPING_UPDATE_FAIL.
     */
    @Test
    void setAntiSnipingCommand_NullPayload_ReturnsFail() {
        Participant p = makeParticipant();
        SetAntiSnipingCommand cmd = new SetAntiSnipingCommand(manager);

        String response = cmd.execute(null, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.ANTI_SNIPING_UPDATE_FAIL.name()),
                "Payload null phai tra ANTI_SNIPING_UPDATE_FAIL.");
    }

    /**
     * SetAntiSniping với auctionId không tồn tại phải trả ANTI_SNIPING_UPDATE_FAIL.
     */
    @Test
    void setAntiSnipingCommand_UnknownAuctionId_ReturnsFail() {
        Participant p = makeParticipant();
        SetAntiSnipingCommand cmd = new SetAntiSnipingCommand(manager);
        JsonNode payload = JsonProtocol.payloadOf(
                Map.of("auctionId", "NOTEXIST-999", "enabled", true));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.ANTI_SNIPING_UPDATE_FAIL.name()),
                "AuctionId khong ton tai phai tra ANTI_SNIPING_UPDATE_FAIL.");
    }

    /**
     * SetAntiSniping hợp lệ phải trả ANTI_SNIPING_UPDATED.
     */
    @Test
    void setAntiSnipingCommand_ValidPayload_ReturnsUpdated() {
        Participant p = makeParticipant();
        String auctionId = createAuction(p);
        SetAntiSnipingCommand cmd = new SetAntiSnipingCommand(manager);
        JsonNode payload = JsonProtocol.payloadOf(
                Map.of("auctionId", auctionId, "enabled", true));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.ANTI_SNIPING_UPDATED.name()),
                "Payload hop le phai tra ANTI_SNIPING_UPDATED.");
    }

    // =========================================================================
    // GetAuctionCommand
    // =========================================================================

    /**
     * GetAuction với payload null phải trả ERROR.
     */
    @Test
    void getAuctionCommand_NullPayload_ReturnsError() {
        GetAuctionCommand cmd = new GetAuctionCommand(manager);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.ERROR.name()),
                "Payload null phai tra ERROR.");
    }

    /**
     * GetAuction với auctionId không tồn tại phải trả ERROR.
     */
    @Test
    void getAuctionCommand_UnknownId_ReturnsError() {
        GetAuctionCommand cmd = new GetAuctionCommand(manager);

        String response = cmd.execute(auctionIdPayload("NOTEXIST-001"), newSession());

        assertTrue(response.contains(Protocol.Response.ERROR.name()),
                "Id khong ton tai phai tra ERROR.");
    }

    /**
     * GetAuction với auctionId hợp lệ phải trả AUCTION_DETAIL.
     */
    @Test
    void getAuctionCommand_ValidId_ReturnsAuctionDetail() {
        Participant p = makeParticipant();
        String auctionId = createAuction(p);
        GetAuctionCommand cmd = new GetAuctionCommand(manager);

        String response = cmd.execute(auctionIdPayload(auctionId), newSession());

        assertNotNull(response, "Response khong duoc null.");
        assertTrue(response.contains(Protocol.Response.AUCTION_DETAIL.name()),
                "Id hop le phai tra AUCTION_DETAIL.");
    }

    // =========================================================================
    // ListAuctionsCommand
    // =========================================================================

    /**
     * ListAuctions không cần đăng nhập — phải trả AUCTION_LIST.
     */
    @Test
    void listAuctionsCommand_NoLogin_ReturnsAuctionList() {
        ListAuctionsCommand cmd = new ListAuctionsCommand(manager);

        String response = cmd.execute(null, newSession());

        assertNotNull(response, "Response khong duoc null.");
        assertTrue(response.contains(Protocol.Response.AUCTION_LIST.name()),
                "ListAuctions luon phai tra AUCTION_LIST.");
    }

    /**
     * ListAuctions khi có phiên đang chạy phải trả AUCTION_LIST.
     */
    @Test
    void listAuctionsCommand_WithActiveAuctions_ReturnsAuctionList() {
        Participant p = makeParticipant();
        createAuction(p);
        ListAuctionsCommand cmd = new ListAuctionsCommand(manager);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.AUCTION_LIST.name()),
                "Co phien dang chay phai tra AUCTION_LIST.");
    }
// =========================================================================
// PlaceBidCommand
// =========================================================================

    /**
     * PlaceBid khi chưa đăng nhập phải trả ERROR.
     */
    @Test
    void placeBidCommand_NotLoggedIn_ReturnsError() {
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);
        JsonNode payload = JsonProtocol.payloadOf(Map.of("auctionId", "any-id", "amount", "1000"));

        String response = cmd.execute(payload, newSession());

        assertTrue(response.contains(Protocol.Response.ERROR.name()),
                "Chua dang nhap phai tra ERROR.");
    }

    /**
     * PlaceBid với payload null phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_NullPayload_ReturnsBidFail() {
        Participant p = makeParticipant();
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);

        String response = cmd.execute(null, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Payload null phai tra BID_FAIL.");
    }

    /**
     * PlaceBid thiếu amount trong payload phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_MissingAmount_ReturnsBidFail() {
        Participant p = makeParticipant();
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);
        JsonNode payload = JsonProtocol.payloadOf(Map.of("auctionId", "some-id"));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Thieu amount phai tra BID_FAIL.");
    }

    /**
     * PlaceBid với amount âm phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_NegativeAmount_ReturnsBidFail() {
        Participant p = makeParticipant();
        String auctionId = createAuction(p);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);
        JsonNode payload = JsonProtocol.payloadOf(Map.of("auctionId", auctionId, "amount", "-500"));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Amount am phai tra BID_FAIL.");
    }

    /**
     * PlaceBid với amount bằng 0 phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_ZeroAmount_ReturnsBidFail() {
        Participant p = makeParticipant();
        String auctionId = createAuction(p);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);
        JsonNode payload = JsonProtocol.payloadOf(Map.of("auctionId", auctionId, "amount", "0"));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Amount bang 0 phai tra BID_FAIL.");
    }

    /**
     * PlaceBid với amount không phải số phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_NonNumericAmount_ReturnsBidFail() {
        Participant p = makeParticipant();
        String auctionId = createAuction(p);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);
        JsonNode payload = JsonProtocol.payloadOf(Map.of("auctionId", auctionId, "amount", "abc"));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Amount khong phai so phai tra BID_FAIL.");
    }

    /**
     * Admin đặt giá phải trả BID_FAIL (chỉ Participant được phép đặt giá).
     */
    @Test
    void placeBidCommand_AdminUser_ReturnsBidFail() {
        Admin admin = new Admin("adm", "adm@test.com", "pass");
        ClientSession session = new ClientSession(NOOP_OBSERVER, manager);
        session.setCurrentUser(admin);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);
        JsonNode payload = JsonProtocol.payloadOf(Map.of("auctionId", "any-id", "amount", "1000"));

        String response = cmd.execute(payload, session);

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Admin dat gia phai tra BID_FAIL.");
    }

    /**
     * PlaceBid với auctionId không tồn tại phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_UnknownAuctionId_ReturnsBidFail() {
        Participant p = makeParticipant();
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);
        JsonNode payload = JsonProtocol.payloadOf(
                Map.of("auctionId", "NOTEXIST-999", "amount", "2000"));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "AuctionId khong ton tai phai tra BID_FAIL.");
    }

    /**
     * PlaceBid vào phiên đã kết thúc phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_FinishedAuction_ReturnsBidFail() {
        Participant p = makeParticipant();
        String auctionId = createAuction(p);
        manager.getAllAuctions().stream()
                .filter(a -> a.getId().equals(auctionId))
                .findFirst()
                .ifPresent(a -> a.setStatus(AuctionStatus.FINISHED));
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);
        JsonNode payload = JsonProtocol.payloadOf(Map.of("auctionId", auctionId, "amount", "2000"));

        String response = cmd.execute(payload, loggedInSession(p));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "Phien da ket thuc phai tra BID_FAIL.");
    }

    /**
     * PlaceBid khi số dư không đủ phải trả BID_FAIL.
     */
    @Test
    void placeBidCommand_InsufficientBalance_ReturnsBidFail() {
        Participant seller = makeParticipant();
        Participant poorBidder = new Participant("poor01", "poor01@test.com", "pass",
                100.0, "PARTICIPANT");
        manager.registerUser(poorBidder);
        String auctionId = createAuction(seller);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);
        JsonNode payload = JsonProtocol.payloadOf(
                Map.of("auctionId", auctionId, "amount", "10000"));

        String response = cmd.execute(payload, loggedInSession(poorBidder));

        assertTrue(response.contains(Protocol.Response.BID_FAIL.name()),
                "So du khong du phai tra BID_FAIL.");
    }

    /**
     * PlaceBid hợp lệ với đủ số dư phải trả BID_OK.
     */
    @Test
    void placeBidCommand_ValidBid_ReturnsBidOk() {
        Participant seller = makeParticipant();
        Participant richBidder = new Participant("rich01", "rich01@test.com", "pass",
                100_000.0, "PARTICIPANT");
        manager.registerUser(richBidder);
        String auctionId = createAuction(seller);
        PlaceBidCommand cmd = new PlaceBidCommand(bidService);
        JsonNode payload = JsonProtocol.payloadOf(Map.of("auctionId", auctionId, "amount", "2000"));

        String response = cmd.execute(payload, loggedInSession(richBidder));

        assertNotNull(response, "Response khong duoc null.");
        assertTrue(response.contains(Protocol.Response.BID_OK.name()),
                "Dat gia hop le phai tra BID_OK.");
    }
}