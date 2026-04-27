package auction_system.common.models;

import auction_system.common.enums.AuctionStatus;
import auction_system.common.patterns.observer.AuctionObserver;

import java.util.List;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class Auction extends Entity {
    private Item item;
    private Seller seller;
    private List<BidTransaction> bids;
    private BidTransaction currentHighestBid;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private List<AuctionObserver> observers;

    public Auction(Item item, Seller seller, LocalDateTime startTime) {
        super();
        this.item = item;
        this.seller = seller;
        this.startTime = startTime;

        this.bids = new ArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
    }

    public boolean placeBid(BidTransaction bid) {
        // to be coded
        return true;
    }

    public Bidder calculateWinner() {
        // to be coded
        return null;
    }

    public void attach(AuctionObserver observer) {
        // to be coded

    }

    public void detach(AuctionObserver observer) {
        // to be coded
    }

    public void notifyObservers() {
        // to be coded

    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public BidTransaction getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(BidTransaction currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Auction{" +
                "id='" + id + '\'' + // Sử dụng id từ lớp Entity
                ", item=" + item +
                ", seller=" + seller +
                ", bids=" + bids +
                ", currentHighestBid=" + currentHighestBid +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status=" + status +
                ", observers=" + observers +
                '}';
    }
}