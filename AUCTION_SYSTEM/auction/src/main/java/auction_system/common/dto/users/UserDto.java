package auction_system.common.dto.users;

import java.io.Serializable;

/**
 * Lớp Dto đại diện cho dữ liệu chuyển đổi của người dùng cơ sở (User).
 */
public class UserDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String email;
    private boolean isOnline;
    private boolean isBanned;
    private String roleName;

    /**
     * Khởi tạo một đối tượng UserDto trống.
     */
    public UserDto() {
    }

    /**
     * Khởi tạo một đối tượng UserDto với đầy đủ thông tin (không bao gồm password).
     *
     * @param id       ID của người dùng.
     * @param username Tên đăng nhập.
     * @param email    Địa chỉ email.
     * @param isOnline Trạng thái online.
     * @param isBanned Trạng thái cấm.
     * @param roleName Vai trò người dùng (ví dụ: ADMIN hoặc PARTICIPANT).
     */
    public UserDto(String id, String username, String email, boolean isOnline,
            boolean isBanned, String roleName) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.isOnline = isOnline;
        this.isBanned = isBanned;
        this.roleName = roleName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public boolean isBanned() {
        return isBanned;
    }

    public void setBanned(boolean banned) {
        isBanned = banned;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    @Override
    public String toString() {
        return "UserDto{"
                + "id='" + id + '\''
                + ", username='" + username + '\''
                + ", email='" + email + '\''
                + ", isOnline=" + isOnline
                + ", isBanned=" + isBanned
                + ", roleName='" + roleName + '\''
                + '}';
    }
}
