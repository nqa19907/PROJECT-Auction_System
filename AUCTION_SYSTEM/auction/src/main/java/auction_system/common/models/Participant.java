package auction_system.common.models;

import auction_system.common.patterns.observer.AuctionObserver;

public abstract class Participant extends User implements AuctionObserver {
    protected double balance;

    public Participant(String username, String password, String email, double balance) {
        super(username, email, password);
        this.balance = balance;
    }

    public void addFunds(double amount) {
        if  (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }
        this.balance += amount;
    }

    public boolean withdrawFunds(double amount) {
        if (amount <= 0 || this.balance < amount) {
            return false;
        }

        this.balance -= amount;
        return true;
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
