package auction_system.common.dto.users;

import java.io.Serializable;

/**
 * DTO chứa thông tin người dùng để truyền qua mạng Socket.
 * Đã loại bỏ mật khẩu để đảm bảo bảo mật.
 * Không có hàm Setter để đảm bảo tính bất biến và an toàn đa luồng.
 */
public final class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final String username;
    private final String email;
    private final boolean isOnline;
    private final boolean isBanned;
    private final String roleName;

    /**
     * Khởi tạo một gói dữ liệu UserDTO bất biến.
     *
     * @param id ID của người dùng.
     * @param username Tên đăng nhập.
     * @param email Địa chỉ email.
     * @param isOnline Trạng thái trực tuyến.
     * @param isBanned Trạng thái bị cấm.
     * @param roleName Vai trò người dùng.
     */
    public UserDTO(
            final String id,
            final String username,
            final String email,
            final boolean isOnline,
            final boolean isBanned,
            final String roleName) {

        this.id = id;
        this.username = username;
        this.email = email;
        this.isOnline = isOnline;
        this.isBanned = isBanned;
        this.roleName = roleName;
    }

    /**
     * Lấy ID người dùng.
     *
     * @return ID.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Lấy tên đăng nhập.
     *
     * @return tên đăng nhập.
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Lấy địa chỉ email.
     *
     * @return email.
     */
    public String getEmail() {
        return this.email;
    }

    /**
     * Kiểm tra trạng thái trực tuyến.
     *
     * @return true nếu online.
     */
    public boolean isOnline() {
        return this.isOnline;
    }

    /**
     * Kiểm tra trạng thái bị cấm.
     *
     * @return true nếu bị cấm.
     */
    public boolean isBanned() {
        return this.isBanned;
    }

    /**
     * Lấy vai trò người dùng.
     *
     * @return tên vai trò.
     */
    public String getRoleName() {
        return this.roleName;
    }
}