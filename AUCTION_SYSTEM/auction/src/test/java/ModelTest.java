import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.auctions.Entity;
import auction_system.common.models.items.Art;
import auction_system.common.models.items.Electronic;
import auction_system.common.models.items.Item;
import auction_system.common.models.items.Vehicle;
import auction_system.common.models.items.builder.ArtBuilder;
import auction_system.common.models.items.builder.ElectronicBuilder;
import auction_system.common.models.items.builder.VehicleBuilder;
import auction_system.common.models.users.Participant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Kiem thu cac model hien tai: Entity, User, Participant, Item,
 * Auction va BidTransaction.
 */
public class ModelTest {

    @Test
    void testEntityAutoGeneratesUuidOnCreate() {
        Participant participant = new Participant("user1", "u1@mail.com", "pass", 1000);

        assertNotNull(participant.getId(), "Entity phai tu sinh UUID");
        assertFalse(participant.getId().isEmpty(), "UUID khong duoc rong");
    }

    @Test
    void testEntityTwoInstancesHaveDifferentIds() {
        Participant participant1 = new Participant("u1", "u1@mail.com", "pw", 100);
        Participant participant2 = new Participant("u2", "u2@mail.com", "pw", 100);

        assertNotEquals(participant1.getId(), participant2.getId());
    }

    @Test
    void testEntityEqualsReflexiveReturnsTrue() {
        Participant participant = new Participant("u1", "u1@mail.com", "pw", 100);

        assertEquals(participant, participant);
    }

    @Test
    void testEntityEqualsDifferentIdReturnsFalse() {
        Participant participant1 = new Participant("user1", "u1@mail.com", "pw", 100);
        Participant participant2 = new Participant("user2", "u2@mail.com", "pw", 200);

        assertNotEquals(participant1, participant2);
    }

    @Test
    void testEntityEqualsNullReturnsFalse() {
        Participant participant = new Participant("u1", "u1@mail.com", "pw", 100);

        assertNotEquals(null, participant);
    }

    @Test
    void testEntityHashCodeConsistentAcrossCalls() {
        Participant participant = new Participant("user1", "u1@mail.com", "pw", 100);

        assertEquals(participant.hashCode(), participant.hashCode());
    }

    @Test
    void testEntityHashCodeDifferentForDifferentIds() {
        Participant participant1 = new Participant("u1", "u1@mail.com", "pw", 100);
        Participant participant2 = new Participant("u2", "u2@mail.com", "pw", 100);

        assertNotEquals(participant1.hashCode(), participant2.hashCode());
    }

    @Test
    void testEntityCreatedAtIsNotNull() {
        Participant participant = new Participant("seller1", "s1@mail.com", "pw", 5000);

        assertNotNull(participant.getCreatedAt());
    }

    @Test
    void testEntityCreatedAtIsWithinExpectedRange() {
        LocalDateTime before = LocalDateTime.now();
        Participant participant = new Participant("u1", "u1@mail.com", "pw", 100);
        LocalDateTime after = LocalDateTime.now();

        assertFalse(participant.getCreatedAt().isBefore(before));
        assertFalse(participant.getCreatedAt().isAfter(after));
    }

    @Test
    void testEntityToStringContainsId() {
        Participant participant = new Participant("u1", "u1@mail.com", "pw", 100);

        assertTrue(participant.toString().contains(participant.getId()));
    }

    @Test
    void testEntityToStringContainsClassName() {
        Participant participant = new Participant("u1", "u1@mail.com", "pw", 100);

        assertTrue(participant.toString().contains("Participant"));
    }

    @Test
    void testEntityConstructorWithEmptyIdThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Entity("", LocalDateTime.now()) {
                    public void update(String msg) {}
                });
    }

    @Test
    void testEntityConstructorWithNullIdThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Entity(null, LocalDateTime.now()) {
                    public void update(String msg) {}
                });
    }

    @Test
    void testUserGettersReturnCorrectValues() {
        Participant participant = new Participant("alice", "alice@mail.com", "secret", 5000);

        assertEquals("alice", participant.getUsername());
        assertEquals("alice@mail.com", participant.getEmail());
        assertEquals("secret", participant.getPassword());
    }

    @Test
    void testUserIsOnlineDefaultIsFalse() {
        Participant participant = new Participant("user", "user@mail.com", "pw", 100);

        assertFalse(participant.isOnline());
    }

    @Test
    void testUserSetOnlineTrueUpdatesStatus() {
        Participant participant = new Participant("user", "user@mail.com", "pw", 100);
        participant.setOnline(true);

        assertTrue(participant.isOnline());
    }

    @Test
    void testUserSetOnlineFalseUpdatesStatus() {
        Participant participant = new Participant("user", "user@mail.com", "pw", 100);
        participant.setOnline(true);
        participant.setOnline(false);

        assertFalse(participant.isOnline());
    }

    @Test
    void testUserSetUsernameUpdatesValue() {
        Participant participant = new Participant("oldname", "u@mail.com", "pw", 100);
        participant.setUsername("newname");

        assertEquals("newname", participant.getUsername());
    }

    @Test
    void testUserSetEmailUpdatesValue() {
        Participant participant = new Participant("user", "old@mail.com", "pw", 100);
        participant.setEmail("new@mail.com");

        assertEquals("new@mail.com", participant.getEmail());
    }

    @Test
    void testUserSetPasswordUpdatesValue() {
        Participant participant = new Participant("user", "u@mail.com", "oldpw", 100);
        participant.setPassword("newpw");

        assertEquals("newpw", participant.getPassword());
    }

    @Test
    void testParticipantDefaultRoleNameReturnsParticipant() {
        Participant participant = new Participant("user", "u@mail.com", "pw", 100);

        assertEquals("PARTICIPANT", participant.getRoleName());
    }

    @Test
    void testParticipantRoleNameReturnsNormalizedRole() {
        Participant participant = new Participant("user", "u@mail.com", "pw", 100, "bidder");

        assertEquals("BIDDER", participant.getRoleName());
    }

    @Test
    void testParticipantBlankRoleFallsBackToParticipant() {
        Participant participant = new Participant("user", "u@mail.com", "pw", 100, " ");

        assertEquals("PARTICIPANT", participant.getRoleName());
    }

    @Test
    void testParticipantSetRoleNameUpdatesValue() {
        Participant participant = new Participant("user", "u@mail.com", "pw", 100);
        participant.setRoleName("seller");

        assertEquals("SELLER", participant.getRoleName());
    }

    @Test
    void testParticipantGetBalanceReturnsInitialValue() {
        Participant participant = new Participant("user", "u@mail.com", "pw", 3000);

        assertEquals(3000, participant.getBalance(), 0.001);
    }

    @Test
    void testParticipantSetBalanceUpdatesValue() {
        Participant participant = new Participant("user", "u@mail.com", "pw", 1000);
        participant.setBalance(5000);

        assertEquals(5000, participant.getBalance(), 0.001);
    }

    @Test
    void testArtGettersReturnCorrectValues() {
        Art art = new ArtBuilder()
                .itemName("Mona Lisa")
                .description("Famous painting")
                .startPrice(1000000.0)
                .sellerId("seller1")
                .build();

        assertEquals("Mona Lisa", art.getItemName());
        assertEquals("Famous painting", art.getDescription());
        assertEquals(1000000.0, art.getStartPrice(), 0.001);
        assertEquals("seller1", art.getSellerId());
    }

    @Test
    void testArtCurrentPriceEqualsStartPriceInitially() {
        Art art = new ArtBuilder()
                .itemName("Piece")
                .startPrice(200000.0)
                .sellerId("s1")
                .build();

        assertEquals(art.getStartPrice(), art.getCurrentPrice(), 0.001);
    }

    @Test
    void testArtSetCurrentPriceUpdatesValue() {
        Art art = new ArtBuilder()
                .itemName("Piece")
                .startPrice(1000.0)
                .sellerId("s1")
                .build();
        art.setCurrentPrice(2500.0);

        assertEquals(2500.0, art.getCurrentPrice(), 0.001);
    }

    @Test
    void testArtSetItemNameUpdatesValue() {
        Art art = new ArtBuilder()
                .itemName("Old Name")
                .startPrice(100.0)
                .sellerId("s1")
                .build();
        art.setItemName("New Name");

        assertEquals("New Name", art.getItemName());
    }

    @Test
    void testArtIsInstanceOfItem() {
        Art art = new ArtBuilder()
                .itemName("Piece")
                .startPrice(100.0)
                .sellerId("s1")
                .build();

        assertTrue(art instanceof Item);
    }

    @Test
    void testArtBuilderWithCurrentPriceOverridesStart() {
        Art art = new ArtBuilder()
                .itemName("Piece")
                .startPrice(1000.0)
                .currentPrice(1800.0)
                .sellerId("s1")
                .build();

        assertEquals(1800.0, art.getCurrentPrice(), 0.001);
    }

    @Test
    void testElectronicGettersReturnCorrectValues() {
        Electronic electronic = new ElectronicBuilder()
                .itemName("MacBook Pro")
                .description("Laptop by Apple")
                .startPrice(3000.0)
                .sellerId("seller2")
                .build();

        assertEquals("MacBook Pro", electronic.getItemName());
        assertEquals("Laptop by Apple", electronic.getDescription());
        assertEquals(3000.0, electronic.getStartPrice(), 0.001);
        assertEquals(3000.0, electronic.getCurrentPrice(), 0.001);
    }

    @Test
    void testElectronicSetCurrentPriceUpdatesValue() {
        Electronic electronic = new ElectronicBuilder()
                .itemName("Item")
                .startPrice(1000.0)
                .sellerId("s1")
                .build();
        electronic.setCurrentPrice(1500.0);

        assertEquals(1500.0, electronic.getCurrentPrice(), 0.001);
    }

    @Test
    void testElectronicIsInstanceOfItem() {
        Electronic electronic = new ElectronicBuilder()
                .itemName("Item")
                .startPrice(100.0)
                .sellerId("s1")
                .build();

        assertTrue(electronic instanceof Item);
    }

    @Test
    void testElectronicTwoInstancesHaveDifferentIds() {
        Electronic el1 = new ElectronicBuilder().startPrice(100.0).sellerId("s1").build();
        Electronic el2 = new ElectronicBuilder().startPrice(200.0).sellerId("s1").build();

        assertNotEquals(el1.getId(), el2.getId());
    }

    @Test
    void testVehicleGettersReturnCorrectValues() {
        Vehicle vehicle = new VehicleBuilder()
                .itemName("Honda Civic")
                .description("Reliable car")
                .startPrice(20000.0)
                .sellerId("seller3")
                .build();

        assertEquals("Honda Civic", vehicle.getItemName());
        assertEquals("Reliable car", vehicle.getDescription());
        assertEquals(20000.0, vehicle.getStartPrice(), 0.001);
    }

    @Test
    void testVehicleCurrentPriceEqualsStartPriceInitially() {
        Vehicle vehicle = new VehicleBuilder()
                .itemName("BMW X5")
                .startPrice(75000.0)
                .sellerId("s1")
                .build();

        assertEquals(vehicle.getStartPrice(), vehicle.getCurrentPrice(), 0.001);
    }

    @Test
    void testVehicleSetCurrentPriceUpdatesValue() {
        Vehicle vehicle = new VehicleBuilder()
                .itemName("Ford")
                .startPrice(10000.0)
                .sellerId("s1")
                .build();
        vehicle.setCurrentPrice(12000.0);

        assertEquals(12000.0, vehicle.getCurrentPrice(), 0.001);
    }

    @Test
    void testVehicleSetSellerIdUpdatesValue() {
        Vehicle vehicle = new VehicleBuilder()
                .itemName("Toyota")
                .startPrice(15000.0)
                .sellerId("old-seller")
                .build();
        vehicle.setSellerId("new-seller");

        assertEquals("new-seller", vehicle.getSellerId());
    }

    @Test
    void testVehicleIsInstanceOfItem() {
        Vehicle vehicle = new VehicleBuilder()
                .itemName("Car")
                .startPrice(10000.0)
                .sellerId("s1")
                .build();

        assertTrue(vehicle instanceof Item);
    }

    // =========================================================================
    // Builder — currentPrice override logic (if currentPrice > 0)
    // =========================================================================

    @Test
    void testElectronicBuilder_CurrentPrice_OverridesWhenPositive() {
        Electronic e = new ElectronicBuilder()
                .itemName("Item").startPrice(1000.0).currentPrice(1200.0).sellerId("s1")
                .build();

        assertEquals(1200.0, e.getCurrentPrice());
    }

    @Test
    void testElectronicBuilder_CurrentPriceZero_FallsBackToStartPrice() {
        // currentPrice = 0 không vượt điều kiện `if (currentPrice > 0)` trong build()
        // → không override → phải fallback về startPrice
        Electronic e = new ElectronicBuilder()
                .itemName("Item").startPrice(2000.0).currentPrice(0).sellerId("s1")
                .build();

        assertEquals(2000.0, e.getCurrentPrice(),
                "currentPrice = 0 không nên override startPrice");
    }

    @Test
    void testVehicleBuilder_CurrentPrice_OverridesWhenPositive() {
        Vehicle v = new VehicleBuilder()
                .itemName("Car").startPrice(20000.0).currentPrice(22000.0).sellerId("s1")
                .build();

        assertEquals(22000.0, v.getCurrentPrice());
    }

    private Auction createDummyAuction() {
        Participant seller = new Participant("tmp_seller", "seller@mail.com", "pw", 1000, "SELLER");
        Item item = new ElectronicBuilder()
                .itemName("Tmp Item")
                .startPrice(1000.0)
                .sellerId(seller.getId())
                .build();
        LocalDateTime now = LocalDateTime.now();
        return new Auction(item, seller, now, now.plusDays(1));
    }

    @Test
    void testAuctionConstructorAssignsParticipantAndItem() {
        Participant seller = new Participant("seller", "seller@mail.com", "pw", 1000, "SELLER");
        Item item = new ElectronicBuilder()
                .itemName("Phone")
                .startPrice(1000.0)
                .sellerId(seller.getId())
                .build();

        Auction auction = new Auction(item, seller, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        assertSame(seller, auction.getParticipant());
        assertSame(item, auction.getItem());
    }

    @Test
    void testBidTransactionGettersReturnCorrectValues() {
        Participant participant = new Participant("participant1", "p1@mail.com", "pw", 10000, "BIDDER");
        double amount = 3500.0;
        Auction dummyAuction = createDummyAuction();
        BidTransaction bid = new BidTransaction(participant, amount, dummyAuction);

        assertSame(participant, bid.getParticipant());
        assertEquals(amount, bid.getAmount(), 0.001);
        assertEquals(dummyAuction.getId(), bid.getAuctionId());
        assertNotNull(bid.getTimestamp());
    }

    @Test
    void testBidTransactionTimestampIsSetOnCreation() {
        LocalDateTime before = LocalDateTime.now();
        Participant participant = new Participant("tmp", "tmp@mail.com", "pw", 1000, "BIDDER");
        Auction dummyAuction = createDummyAuction();
        BidTransaction bid = new BidTransaction(participant, 5000.0, dummyAuction);
        LocalDateTime after = LocalDateTime.now();

        assertFalse(bid.getTimestamp().isBefore(before));
        assertFalse(bid.getTimestamp().isAfter(after));
    }

    @Test
    void testBidTransactionHasNonNullId() {
        Participant participant = new Participant("tmp", "tmp@mail.com", "pw", 1000, "BIDDER");
        Auction dummyAuction = createDummyAuction();
        BidTransaction bid = new BidTransaction(participant, 1000.0, dummyAuction);

        assertNotNull(bid.getId());
        assertFalse(bid.getId().isEmpty());
    }

    @Test
    void testBidTransactionTwoInstancesHaveDifferentIds() {
        Participant participant = new Participant("tmp", "tmp@mail.com", "pw", 1000, "BIDDER");
        Auction dummyAuction = createDummyAuction();
        BidTransaction bid1 = new BidTransaction(participant, 2500.0, dummyAuction);
        BidTransaction bid2 = new BidTransaction(participant, 3500.0, dummyAuction);

        assertNotEquals(bid1.getId(), bid2.getId());
    }

    @Test
    void testBidTransactionAmountIsPreservedExactly() {
        double amount = 12345.678;
        Participant participant = new Participant("tmp", "tmp@mail.com", "pw", 1000, "BIDDER");
        Auction dummyAuction = createDummyAuction();
        BidTransaction bid = new BidTransaction(participant, amount, dummyAuction);

        assertEquals(amount, bid.getAmount(), 0.00001);
    }
}