package models;

public class Seller extends User {
    private double balance;

    public Seller(String username, String userId, String email, String password) {
        super(username, userId, email, password);
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString(){
        return "userId: " + getUserId() + ", username: " + getUsername() + ", password: " + getPassword() + ", email:"
                + getEmail() + ", balance:" + getBalance();
    }
}
