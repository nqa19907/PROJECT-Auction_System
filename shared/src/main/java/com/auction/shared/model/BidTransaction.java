package com.auction.shared.model;
import java.time.LocalDateTime;
public class BidTransaction extends Entity {
    private Bidder bidder;
    private double amount;
    private LocalDateTime timestamp;

    public BidTransaction(Bidder bidder, double amount) {
        super(); // Tự động tạo ID
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public Bidder getBidder() {
        return bidder;
    }

    public void setBidder(Bidder bidder) {
        this.bidder = bidder;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
