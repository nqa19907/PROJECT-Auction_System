package auction_system.common.dto.users;

import java.io.Serializable;

/**
 * DTO chứa thông tin người tham gia đấu giá để truyền qua mạng Socket.
 * Đã loại bỏ mật khẩu để bảo mật và không có hàm Setter để đảm bảo an toàn đa luồng.
 */
public final class ParticipantDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String email;
    private final double balance;

    /**
     * Khởi tạo một gói dữ liệu ParticipantDTO bất biến.
     */
    public ParticipantDTO(final String username, final String email, final double balance) {
        this.username = username;
        this.email = email;
        this.balance = balance;
    }

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public double getBalance() {
        return this.balance;
    }
}