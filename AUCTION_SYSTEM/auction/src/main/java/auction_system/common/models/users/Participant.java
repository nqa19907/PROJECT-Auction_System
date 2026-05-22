package auction_system.common.models.users;

/**
 * Lớp đại diện cho người tham gia hệ thống đấu giá.
 *
 * <p>Một participant có tài khoản, số dư và vai trò trong hệ thống. Lớp này
 * thay thế cho hai lớp Seller và Bidder để tránh tạo subclass chỉ khác nhau
 * rất ít về dữ liệu hoặc hành vi.
 */
public class Participant extends User {
    private static final String participantRoleName = "PARTICIPANT";
    private static final String sellerRoleName = "SELLER";
    private static final String bidderRoleName = "BIDDER";

    private double balance;
    private String roleName;

    /**
     * Khởi tạo participant với vai trò mặc định.
     *
     * @param username tên đăng nhập
     * @param email địa chỉ email
     * @param password mật khẩu đã xử lý
     * @param balance số dư ban đầu
     */
    public Participant(
        final String username,
        final String email,
        final String password,
        final double balance) {

        this(username, email, password, balance, participantRoleName);
    }

    /**
     * Khởi tạo participant với vai trò cụ thể.
     *
     * @param username tên đăng nhập
     * @param email địa chỉ email
     * @param password mật khẩu đã xử lý
     * @param balance số dư ban đầu
     * @param roleName vai trò của người dùng
     */
    public Participant(
        final String username,
        final String email,
        final String password,
        final double balance,
        final String roleName) {

        super(username, email, password);
        this.balance = balance;
        setRoleName(roleName);
    }

    /**
     * Nhận thông báo từ phiên đấu giá.
     *
     * <p>Trong hệ thống socket thật, việc gửi thông báo về client nên do
     * ClientHandler xử lý. Phương thức này chỉ tồn tại vì User đang đóng vai
     * trò AuctionObserver.
     *
     * @param message nội dung thông báo
     */
    @Override
    public void update(final String message) {
        System.out.println("[NOTIFY]: " + message);
    }

    /**
     * Lấy số dư hiện tại.
     *
     * @return số dư của participant
     */
    public double getBalance() {
        return balance;
    }

    /**
     * Cập nhật số dư.
     *
     * @param balance số dư mới
     */
    public void setBalance(final double balance) {
        this.balance = balance;
    }

    /**
     * Lấy tên vai trò của participant.
     *
     * @return tên vai trò
     */
    @Override
    public String getRoleName() {
        return roleName;
    }

    /**
     * Cập nhật vai trò của participant.
     *
     * @param roleName vai trò mới
     */
    public void setRoleName(final String roleName) {
        if (roleName == null || roleName.isBlank()) {
            this.roleName = participantRoleName;
            return;
        }

        this.roleName = roleName.trim().toUpperCase();
    }

    @Override
    public String getRoleDisplayName() {
        return "Người tham gia";
    }

    /**
     * Chuyển participant thành chuỗi mô tả ngắn.
     *
     * @return chuỗi mô tả participant
     */
    @Override
    public String toString() {
        return super.toString()
            + " -> Participant{"
            + "balance="
            + balance
            + ", roleName='"
            + roleName
            + '\''
            + '}';
    }
}