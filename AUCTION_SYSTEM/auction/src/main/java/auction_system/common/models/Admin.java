package auction_system.common.models;

/**
 * Lớp đại diện cho quản trị viên hệ thống.
 */
public class Admin extends User {
    private int adminRoleLevel;

    /**
     * Khởi tạo một quản trị viên mới.
     *
     * @param username Tên đăng nhập.
     * @param email    Địa chỉ email.
     * @param password Mật khẩu.
     */
    public Admin(String username, String email, String password) {
        super(username, email, password);
    }

    @Override
    public void update(String message) {
        System.out.println("[NOTIFY]: " + message);
    }

    /**
     * Cấm người dùng khỏi hệ thống.
     *
     * @param user Người dùng cần cấm.
     */
    public void banUser(User user,String reason) {
        user.setBanned(true);
        System.out.println("Bạn đã bị ban với lý do: " + reason);
    }

    /**
     * Xóa sản phẩm không hợp lệ khỏi hệ thống.
     *
     * @param item Sản phẩm cần xóa.
     */
    public void removeInvalidItem(Item item) {
        // to be coded
    }

    public int getAdminRoleLevel() {
        return adminRoleLevel;
    }

    public void setAdminRoleLevel(int adminRoleLevel) {
        this.adminRoleLevel = adminRoleLevel;
    }

    @Override
    public String toString() {
        return super.toString() + " -> Admin{"
                + "adminRoleLevel=" + adminRoleLevel
                + '}';
    }
}
