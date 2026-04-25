package auction_system.common.models;

import auction_system.common.patterns.observer.AuctionObserver;

public abstract class User extends Entity implements AuctionObserver {
    private String username;
    // PASSWORD SHOULD BE HASHED
    private String password;
    private String email;
    private boolean isOnline;

    public User(String username, String email, String password) {
        super();
        this.username = username;
        this.password = password;
        this.email = email;
        this.isOnline = false;
    }

    @Override
    public String toString() {
        return super.toString() + " -> User{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
}