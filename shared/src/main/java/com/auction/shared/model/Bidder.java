package com.auction.shared.model;

public class Bidder extends Participant {
    public Bidder(String username, String email, String password, double balance) {
        super(username, email, password, balance);
    }


    @Override
    public void update(String message) {
        // to be coded
    }

    public void placeBid(Item item, double amount) {
        // to be coded
    }

    public void viewBidHistory() {
        // to be coded
    }

    public void setupAutoBid(Item item, double maxPrice) {
        // to be coded
    }
}