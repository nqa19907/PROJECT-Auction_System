package auction_system.common.models;

import auction_system.common.patterns.observer.AuctionObserver;

public abstract class Participant extends User implements AuctionObserver {
    protected double balance;

    public Participant(String username, String password, String email, double balance) {
        super(username, email, password);
        this.balance = balance;
    }

    public void addFunds(double amount) {
        // to be coded
    }

    public boolean withdrawFunds(double amount) {
        // to be coded
        return true;
    }

    @Override
    public void displayDashboard() {
        // to be coded
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "Participant{" +
                "balance=" + balance +
                '}';
    }
}
