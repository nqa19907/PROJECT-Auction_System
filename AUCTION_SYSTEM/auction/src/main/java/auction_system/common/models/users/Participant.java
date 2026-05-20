package auction_system.common.models.users;

/**
 * Lớp đại diện cho người tham gia hệ thống, có tài khoản và số dư.
 */
public abstract class Participant extends User {
    private double balance;

    /**
     * Khởi tạo một người tham gia mới.
     *
     * @param username Tên đăng nhập.
     * @param email    Địa chỉ email.
     * @param password Mật khẩu.
     * @param balance  Số dư ban đầu.
     */
    public Participant(String username, String email, String password, double balance) {
        super(username, email, password);
        this.balance = balance;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return super.toString() + " -> Participant{"
                + "balance=" + balance
                + '}';
    }
}
