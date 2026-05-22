package auction_system.common.models.users;

import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.auctions.Entity;

/**
 * Lớp trừu tượng đại diện cho người dùng trong hệ thống.
 */
public abstract class User extends Entity implements AuctionObserver {
    private String username;
    // PASSWORD SHOULD BE HASHED
    private String password;
    private String email;
    private boolean isOnline;
    private boolean isBanned = false;

    /**
     * Khởi tạo một người dùng mới.
     *
     * @param username Tên đăng nhập.
     * @param email    Địa chỉ email.
     * @param password Mật khẩu.
     */
    public User(String username, String email, String password) {
        super();
        this.username = username;
        this.password = password;
        this.email = email;
        this.isOnline = false;
    }

    @Override
    public String toString() {
        return super.toString() + " -> User{"
                + "username='" + username + '\''
                + ", email='" + email + '\''
                + '}';
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
    
    public void setBanned(boolean isBanned) {
        this.isBanned = isBanned;
    }

    /**
     * Lấy tên vai trò của người dùng (VD: "ADMIN", "SELLER", "BIDDER").
     *
     * @return Chuỗi đại diện cho vai trò.
     */
    public abstract String getRoleName();

    public abstract String getRoleDisplayName();
}