import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.items.Electronic;
import auction_system.common.models.items.Item;
import auction_system.common.models.users.Participant;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử Observer pattern trong {@link Auction}.
 *
 * <p>Các nhóm test:
 * <ol>
 *   <li>{@code attach} — không thêm null, không thêm trùng.</li>
 *   <li>{@code detach} — observer không còn nhận thông báo sau khi gỡ.</li>
 *   <li>{@code notifyObservers()} — toàn bộ observer đính kèm nhận đúng
 *       message dạng UPDATE_PRICE.</li>
 *   <li>{@code notifyObservers(String)} — custom message được truyền nguyên
 *       vẹn tới tất cả observer.</li>
 *   <li>Tích hợp — attach → bid → detach → bid lần hai chỉ notify observer
 *       còn lại.</li>
 * </ol>
 */
public class AuctionObserverTest {

    /** Phiên đấu giá dùng chung, được reset trước mỗi test. */
    private Auction auction;

    /**
     * Khởi tạo phiên đấu giá đang RUNNING trước mỗi test.
     */
    @BeforeEach
    void setUp() {
        Participant seller = new Participant(
                "seller01", "seller@mail.com", "pass", 0.0, "PARTICIPANT");
        Item item = new Electronic(
                "Test Item", "Mo ta", 1000.0, seller.getId());
        auction = new Auction(
                item,
                seller,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusHours(2));
        auction.startAuction();
    }

    // =========================================================================
    // Nhóm 1: attach
    // =========================================================================

    /**
     * attach(null) không được throw và không thêm gì vào danh sách observer.
     */
    @Test
    void attachNullObserverDoesNotThrowAndIsIgnored() {
        auction.attach(null); // không throw

        RecordingObserver observer = new RecordingObserver();
        auction.attach(observer);
        auction.notifyObservers("TEST");

        assertEquals(1, observer.getMessages().size(),
                "Attach null khong anh huong den observer hop le.");
    }

    /**
     * attach cùng một observer hai lần — observer chỉ nhận mỗi thông báo
     * một lần (không trùng lặp).
     */
    @Test
    void attachSameObserverTwiceReceivesMessageOnce() {
        RecordingObserver observer = new RecordingObserver();
        auction.attach(observer);
        auction.attach(observer);

        auction.notifyObservers("DUPLICATE_TEST");

        assertEquals(1, observer.getMessages().size(),
                "Observer chi nhan moi thong bao dung mot lan du attach 2 lan.");
    }

    /**
     * attach nhiều observer khác nhau — tất cả đều nhận thông báo.
     */
    @Test
    void attachMultipleObserversAllReceiveMessage() {
        RecordingObserver obs1 = new RecordingObserver();
        RecordingObserver obs2 = new RecordingObserver();
        RecordingObserver obs3 = new RecordingObserver();
        auction.attach(obs1);
        auction.attach(obs2);
        auction.attach(obs3);

        auction.notifyObservers("BROADCAST");

        assertEquals(1, obs1.getMessages().size());
        assertEquals(1, obs2.getMessages().size());
        assertEquals(1, obs3.getMessages().size());
    }

    // =========================================================================
    // Nhóm 2: detach
    // =========================================================================

    /**
     * Observer đã detach không nhận thêm thông báo nào nữa.
     */
    @Test
    void detachObserverStopsReceivingMessages() {
        RecordingObserver observer = new RecordingObserver();
        auction.attach(observer);
        auction.detach(observer);

        auction.notifyObservers("AFTER_DETACH");

        assertTrue(observer.getMessages().isEmpty(),
                "Observer da detach khong duoc nhan them thong bao.");
    }

    /**
     * detach observer chưa từng attach không throw và không ảnh hưởng
     * observer khác.
     */
    @Test
    void detachUnknownObserverDoesNotThrow() {
        RecordingObserver known = new RecordingObserver();
        RecordingObserver unknown = new RecordingObserver();
        auction.attach(known);

        auction.detach(unknown);
        auction.notifyObservers("SAFE_DETACH");

        assertEquals(1, known.getMessages().size(),
                "Observer con lai van nhan duoc thong bao.");
    }

    // =========================================================================
    // Nhóm 3: notifyObservers()
    // =========================================================================

    /**
     * notifyObservers() (không tham số) phải gửi JSON UPDATE_PRICE tới observer đang attach.
     */
    @Test
    void notifyObserversNoArgSendsUpdatePriceMessage() throws Exception {
        RecordingObserver observer = new RecordingObserver();
        auction.attach(observer);

        auction.notifyObservers();

        assertEquals(1, observer.getMessages().size(),
                "Observer phai nhan dung 1 thong bao.");
        final JsonMessage message = JsonProtocol.parse(observer.getMessages().get(0));
        assertEquals(
                Protocol.Response.UPDATE_PRICE.name(),
                message.type(),
                "Message phai co type UPDATE_PRICE.");
    }

    /**
     * notifyObservers() gửi message chứa ID của phiên đấu giá.
     */
    @Test
    void notifyObserversNoArgMessageContainsAuctionId() {
        RecordingObserver observer = new RecordingObserver();
        auction.attach(observer);

        auction.notifyObservers();

        assertTrue(
                observer.getMessages().get(0).contains(auction.getId()),
                "Message phai chua auction ID de client biet phien nao cap nhat.");
    }

    // =========================================================================
    // Nhóm 4: notifyObservers(String)
    // =========================================================================

    /**
     * notifyObservers(String) phải truyền nguyên vẹn custom message tới
     * tất cả observer.
     */
    @Test
    void notifyObserversWithCustomMessageDeliveredExactly() {
        RecordingObserver obs1 = new RecordingObserver();
        RecordingObserver obs2 = new RecordingObserver();
        auction.attach(obs1);
        auction.attach(obs2);
        String customMessage = "AUCTION_EXTENDED|" + auction.getId() + "|2099-01-01T00:00";

        auction.notifyObservers(customMessage);

        assertEquals(customMessage, obs1.getMessages().get(0),
                "obs1 phai nhan dung custom message.");
        assertEquals(customMessage, obs2.getMessages().get(0),
                "obs2 phai nhan dung custom message.");
    }

    /**
     * Phiên không có observer nào được attach — notifyObservers không throw.
     */
    @Test
    void notifyObserversNoObserversDoesNotThrow() {
        auction.notifyObservers("NO_OBSERVER_MSG");
    }

    // =========================================================================
    // Nhóm 5: Tích hợp attach + bid + detach
    // =========================================================================

    /**
     * Sau khi detach, observer không nhận thông báo khi có bid mới.
     */
    @Test
    void bidAfterDetachDoesNotNotifyDetachedObserver() {
        RecordingObserver detachedObs = new RecordingObserver();
        RecordingObserver activeObs = new RecordingObserver();
        auction.attach(detachedObs);
        auction.attach(activeObs);

        auction.detach(detachedObs);

        Participant bidder = new Participant(
                "bidder01", "bd@mail.com", "pw", 5000.0, "PARTICIPANT");
        BidTransaction bid = new BidTransaction(bidder, 1500.0, auction);
        auction.placeBid(bid);

        assertTrue(detachedObs.getMessages().isEmpty(),
                "Observer da detach khong duoc nhan thong bao khi co bid moi.");
        assertEquals(1, activeObs.getMessages().size(),
                "Observer con lai phai nhan dung 1 thong bao.");
    }

    /**
     * Observer nhận đúng số lượng thông báo tương ứng với số lần bid.
     */
    @Test
    void multipleBidsNotifyObserverEachTime() {
        RecordingObserver observer = new RecordingObserver();
        auction.attach(observer);

        Participant bidder1 = new Participant(
                "bd1", "bd1@mail.com", "pw", 10000.0, "PARTICIPANT");
        Participant bidder2 = new Participant(
                "bd2", "bd2@mail.com", "pw", 10000.0, "PARTICIPANT");

        auction.placeBid(new BidTransaction(bidder1, 1500.0, auction));
        auction.placeBid(new BidTransaction(bidder2, 2000.0, auction));

        assertEquals(2, observer.getMessages().size(),
                "Observer phai nhan thong bao moi khi co bid moi.");
    }

    // =========================================================================
    // RecordingObserver — test double
    // =========================================================================

    /**
     * Observer giả lưu lại toàn bộ message nhận được để kiểm tra trong test.
     */
    private static class RecordingObserver implements AuctionObserver {

        /** Danh sách message đã nhận. */
        private final List<String> messages = new ArrayList<>();

        @Override
        public void update(final String message) {
            messages.add(message);
        }

        /**
         * Trả về danh sách message đã nhận.
         *
         * @return danh sách message (không thể null)
         */
        public List<String> getMessages() {
            return messages;
        }
    }
}
