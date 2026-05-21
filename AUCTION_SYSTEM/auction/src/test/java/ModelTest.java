import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.Entity;
import auction_system.common.models.items.Art;
import auction_system.common.models.items.Electronic;
import auction_system.common.models.items.Item;
import auction_system.common.models.items.Vehicle;
import auction_system.common.models.items.builder.ArtBuilder;
import auction_system.common.models.items.builder.ElectronicBuilder;
import auction_system.common.models.items.builder.VehicleBuilder;
import auction_system.common.models.users.Bidder;
import auction_system.common.models.users.Seller;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Kiem thu toan dien cac lop model: Entity, User, Participant,
 * Bidder, Seller, Item (Art, Electronic, Vehicle), BidTransaction.
 */
public class ModelTest {

    // =========================================================================
    // Entity - UUID, equals, hashCode, toString
    // =========================================================================

    @Test
    void testEntityAutoGeneratesUuidOnCreate() {
        Bidder bidder = new Bidder("user1", "u1@mail.com", "pass", 1000);

        assertNotNull(bidder.getId(), "Entity phai tu sinh UUID");
        assertFalse(bidder.getId().isEmpty(), "UUID khong duoc rong");
    }

    @Test
    void testEntityTwoInstancesHaveDifferentIds() {
        Bidder bidder1 = new Bidder("u1", "u1@mail.com", "pw", 100);
        Bidder bidder2 = new Bidder("u2", "u2@mail.com", "pw", 100);

        assertNotEquals(bidder1.getId(), bidder2.getId());
    }

    @Test
    void testEntityEqualsReflexiveReturnsTrue() {
        Bidder bidder = new Bidder("u1", "u1@mail.com", "pw", 100);

        assertEquals(bidder, bidder);
    }

    @Test
    void testEntityEqualsDifferentIdReturnsFalse() {
        Bidder bidder1 = new Bidder("user1", "u1@mail.com", "pw", 100);
        Bidder bidder2 = new Bidder("user2", "u2@mail.com", "pw", 200);

        assertNotEquals(bidder1, bidder2);
    }

    @Test
    void testEntityEqualsNullReturnsFalse() {
        Bidder bidder = new Bidder("u1", "u1@mail.com", "pw", 100);

        assertNotEquals(null, bidder);
    }

    @Test
    void testEntityHashCodeConsistentAcrossCalls() {
        Bidder bidder = new Bidder("user1", "u1@mail.com", "pw", 100);

        assertEquals(bidder.hashCode(), bidder.hashCode());
    }

    @Test
    void testEntityHashCodeDifferentForDifferentIds() {
        Bidder bidder1 = new Bidder("u1", "u1@mail.com", "pw", 100);
        Bidder bidder2 = new Bidder("u2", "u2@mail.com", "pw", 100);

        assertNotEquals(bidder1.hashCode(), bidder2.hashCode());
    }

    @Test
    void testEntityCreatedAtIsNotNull() {
        Seller seller = new Seller("seller1", "s1@mail.com", "pw", 5000, 4.0f);

        assertNotNull(seller.getCreatedAt());
    }

    @Test
    void testEntityCreatedAtIsWithinExpectedRange() {
        LocalDateTime before = LocalDateTime.now();
        Bidder bidder = new Bidder("u1", "u1@mail.com", "pw", 100);
        LocalDateTime after = LocalDateTime.now();

        assertFalse(bidder.getCreatedAt().isBefore(before));
        assertFalse(bidder.getCreatedAt().isAfter(after));
    }

    @Test
    void testEntityToStringContainsId() {
        Bidder bidder = new Bidder("u1", "u1@mail.com", "pw", 100);

        assertTrue(bidder.toString().contains(bidder.getId()));
    }

    @Test
    void testEntityToStringContainsClassName() {
        Bidder bidder = new Bidder("u1", "u1@mail.com", "pw", 100);

        assertTrue(bidder.toString().contains("Bidder"));
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

    // =========================================================================
    // User - getters/setters
    // =========================================================================

    @Test
    void testUserGettersReturnCorrectValues() {
        Bidder bidder = new Bidder("alice", "alice@mail.com", "secret", 5000);

        assertEquals("alice", bidder.getUsername());
        assertEquals("alice@mail.com", bidder.getEmail());
        assertEquals("secret", bidder.getPassword());
    }

    @Test
    void testUserIsOnlineDefaultIsFalse() {
        Bidder bidder = new Bidder("user", "user@mail.com", "pw", 100);

        assertFalse(bidder.isOnline());
    }

    @Test
    void testUserSetOnlineTrueUpdatesStatus() {
        Bidder bidder = new Bidder("user", "user@mail.com", "pw", 100);
        bidder.setOnline(true);

        assertTrue(bidder.isOnline());
    }

    @Test
    void testUserSetOnlineFalseUpdatesStatus() {
        Bidder bidder = new Bidder("user", "user@mail.com", "pw", 100);
        bidder.setOnline(true);
        bidder.setOnline(false);

        assertFalse(bidder.isOnline());
    }

    @Test
    void testUserSetUsernameUpdatesValue() {
        Bidder bidder = new Bidder("oldname", "u@mail.com", "pw", 100);
        bidder.setUsername("newname");

        assertEquals("newname", bidder.getUsername());
    }

    @Test
    void testUserSetEmailUpdatesValue() {
        Bidder bidder = new Bidder("user", "old@mail.com", "pw", 100);
        bidder.setEmail("new@mail.com");

        assertEquals("new@mail.com", bidder.getEmail());
    }

    @Test
    void testUserSetPasswordUpdatesValue() {
        Bidder bidder = new Bidder("user", "u@mail.com", "oldpw", 100);
        bidder.setPassword("newpw");

        assertEquals("newpw", bidder.getPassword());
    }

    @Test
    void testBidderGetRoleNameReturnsBidder() {
        Bidder bidder = new Bidder("user", "u@mail.com", "pw", 100);

        assertEquals("BIDDER", bidder.getRoleName());
    }

    @Test
    void testSellerGetRoleNameReturnsSeller() {
        Seller seller = new Seller("seller", "s@mail.com", "pw", 1000, 4.0f);

        assertEquals("SELLER", seller.getRoleName());
    }

    // =========================================================================
    // Participant - balance, addFunds, withdrawFunds
    // =========================================================================

    @Test
    void testParticipantGetBalanceReturnsInitialValue() {
        Bidder bidder = new Bidder("user", "u@mail.com", "pw", 3000);

        assertEquals(3000, bidder.getBalance(), 0.001);
    }

    @Test
    void testParticipantSetBalanceUpdatesValue() {
        Bidder bidder = new Bidder("user", "u@mail.com", "pw", 1000);
        bidder.setBalance(5000);

        assertEquals(5000, bidder.getBalance(), 0.001);
    }

    @Test
    void testParticipantAddFundsIncreasesBalance() {
        Bidder bidder = new Bidder("user", "u@mail.com", "pw", 1000);
        bidder.addFunds(500);

        assertEquals(1500, bidder.getBalance(), 0.001);
    }

    @Test
    void testParticipantAddFundsZeroThrowsException() {
        Bidder bidder = new Bidder("user", "u@mail.com", "pw", 1000);

        assertThrows(IllegalArgumentException.class, () -> bidder.addFunds(0));
    }

    @Test
    void testParticipantAddFundsNegativeThrowsException() {
        Bidder bidder = new Bidder("user", "u@mail.com", "pw", 1000);

        assertThrows(IllegalArgumentException.class, () -> bidder.addFunds(-100));
    }

    @Test
    void testParticipantWithdrawFundsSuccessReturnsTrue() {
        Bidder bidder = new Bidder("user", "u@mail.com", "pw", 1000);

        assertTrue(bidder.withdrawFunds(400));
        assertEquals(600, bidder.getBalance(), 0.001);
    }

    @Test
    void testParticipantWithdrawFundsInsufficientReturnsFalse() {
        Bidder bidder = new Bidder("user", "u@mail.com", "pw", 100);

        assertFalse(bidder.withdrawFunds(999));
        assertEquals(100, bidder.getBalance(), 0.001);
    }

    @Test
    void testParticipantWithdrawFundsExactBalanceReturnsTrue() {
        Bidder bidder = new Bidder("user", "u@mail.com", "pw", 500);

        assertTrue(bidder.withdrawFunds(500));
        assertEquals(0, bidder.getBalance(), 0.001);
    }

    @Test
    void testParticipantWithdrawFundsZeroReturnsFalse() {
        Bidder bidder = new Bidder("user", "u@mail.com", "pw", 1000);

        assertFalse(bidder.withdrawFunds(0));
    }

    // =========================================================================
    // Seller
    // =========================================================================

    @Test
    void testSellerGetRatingReturnsInitialValue() {
        Seller seller = new Seller("seller1", "s@mail.com", "pw", 2000, 4.5f);

        assertEquals(4.5f, seller.getRating(), 0.001f);
    }

    @Test
    void testSellerSetRatingUpdatesValue() {
        Seller seller = new Seller("seller1", "s@mail.com", "pw", 2000, 3.0f);
        seller.setRating(4.8f);

        assertEquals(4.8f, seller.getRating(), 0.001f);
    }

    @Test
    void testSellerListItemForAuctionAssignsSellerIdToItem() {
        Seller seller = new Seller("seller1", "s@mail.com", "pw", 2000, 4.0f);
        Art art = new ArtBuilder()
                .itemName("Sunrise")
                .startPrice(50000.0)
                .sellerId("tmp")
                .build();

        seller.listItemForAuction(art);

        assertEquals(seller.getId(), art.getSellerId());
    }

    @Test
    void testSellerListItemNullThrowsException() {
        Seller seller = new Seller("seller1", "s@mail.com", "pw", 2000, 4.0f);

        assertThrows(RuntimeException.class, () -> seller.listItemForAuction(null));
    }

    @Test
    void testSellerDelistItemNotOwnedThrowsException() {
        Seller seller = new Seller("seller1", "s@mail.com", "pw", 2000, 4.0f);
        Art art = new ArtBuilder()
                .itemName("Fake")
                .startPrice(100.0)
                .sellerId("other")
                .build();

        assertThrows(RuntimeException.class, () -> seller.delistItem(art));
    }

    @Test
    void testSellerDelistAfterListSucceeds() {
        Seller seller = new Seller("seller1", "s@mail.com", "pw", 2000, 4.0f);
        Art art = new ArtBuilder()
                .itemName("Piece")
                .startPrice(100.0)
                .sellerId("tmp")
                .build();
        seller.listItemForAuction(art);

        seller.delistItem(art);

        assertThrows(RuntimeException.class, () -> seller.delistItem(art));
    }

    // =========================================================================
    // Art
    // =========================================================================

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

    // =========================================================================
    // Electronic
    // =========================================================================

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

    // =========================================================================
    // Vehicle
    // =========================================================================

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
    // BidTransaction
    // =========================================================================

    // Hàm phụ giúp tạo nhanh một đối tượng Auction hợp lệ để phục vụ việc test
    private auction_system.common.models.auctions.Auction createDummyAuction() {
        Seller dummySeller = new Seller("tmp_seller", "seller@mail.com", "pw", 1000, 4.0f);
        Item dummyItem = new auction_system.common.models.items.builder.ElectronicBuilder()
                .itemName("Tmp Item")
                .startPrice(1000.0)
                .sellerId(dummySeller.getId())
                .build();
        LocalDateTime now = LocalDateTime.now();
        return new auction_system.common.models.auctions.Auction(dummyItem, dummySeller, now, now.plusDays(1));
    }

    @Test
    void testBidTransactionGettersReturnCorrectValues() {
        Bidder bidder = new Bidder("bidder1", "b1@mail.com", "pw", 10000);
        double amount = 3500.0;
        Auction dummyAuction = createDummyAuction();
        BidTransaction bid = new BidTransaction(bidder, amount, dummyAuction);

        assertSame(bidder, bid.getBidder());
        assertEquals(amount, bid.getAmount(), 0.001);
        assertNotNull(bid.getTimestamp());
    }

    @Test
    void testBidTransactionTimestampIsSetOnCreation() {
        LocalDateTime before = LocalDateTime.now();
        Bidder dummyBidder = new Bidder("tmp", "tmp@mail.com", "pw", 1000);
        Auction dummyAuction = createDummyAuction();
        BidTransaction bid = new BidTransaction(dummyBidder, 5000.0, dummyAuction);
        LocalDateTime after = LocalDateTime.now();

        assertFalse(bid.getTimestamp().isBefore(before));
        assertFalse(bid.getTimestamp().isAfter(after));
    }

    @Test
    void testBidTransactionHasNonNullId() {
        Bidder dummyBidder = new Bidder("tmp", "tmp@mail.com", "pw", 1000);
        Auction dummyAuction = createDummyAuction();
        BidTransaction bid = new BidTransaction(dummyBidder, 1000.0, dummyAuction);

        assertNotNull(bid.getId());
        assertFalse(bid.getId().isEmpty());
    }

    @Test
    void testBidTransactionTwoInstancesHaveDifferentIds() {
        Bidder dummyBidder = new Bidder("tmp", "tmp@mail.com", "pw", 1000);
        Auction dummyAuction = createDummyAuction();
        BidTransaction bid1 = new BidTransaction(dummyBidder, 2500.0, dummyAuction);
        BidTransaction bid2 = new BidTransaction(dummyBidder, 3500.0, dummyAuction);

        assertNotEquals(bid1.getId(), bid2.getId());
    }

    @Test
    void testBidTransactionAmountIsPreservedExactly() {
        double amount = 12345.678;
        Bidder dummyBidder = new Bidder("tmp", "tmp@mail.com", "pw", 1000);
        Auction dummyAuction = createDummyAuction();
        BidTransaction bid = new BidTransaction(dummyBidder, amount, dummyAuction);

        assertEquals(amount, bid.getAmount(), 0.00001);
    }
}