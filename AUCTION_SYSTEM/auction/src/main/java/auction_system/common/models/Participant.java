package auction_system.common.models;

/**
 * Lớp đại diện cho người tham gia hệ thống, có tài khoản và số dư.
 */
public abstract class Participant extends User {
    protected double balance;

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

    /**
     * Nạp thêm tiền vào tài khoản.
     *
     * @param amount Số tiền cần nạp (phải lớn hơn 0).
     */
    public void addFunds(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0");
        }
        this.balance += amount;
    }

    /**
     * Rút tiền khỏi tài khoản.
     *
     * @param amount Số tiền cần rút.
     * @return true nếu rút thành công, false nếu số dư không đủ hoặc số tiền không
     *         hợp lệ.
     */
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
        return "Participant{"
                + "balance=" + balance
                + '}';
    }
}
