package auction_system.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO chứa thông tin thực thể cơ bản để truyền qua mạng Socket.
 * Không có hàm Setter để đảm bảo tính bất biến và an toàn đa luồng.
 */
public final class EntityDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String id;
    private final LocalDateTime createdAt;

    /**
     * Khởi tạo một gói dữ liệu EntityDTO bất biến.
     *
     * @param id ID của thực thể.
     * @param createdAt Thời gian tạo thực thể.
     */
    public EntityDTO(
            final String id,
            final LocalDateTime createdAt) {

        this.id = id;
        this.createdAt = createdAt;
    }

    /**
     * Lấy ID thực thể.
     *
     * @return ID thực thể.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Lấy thời gian tạo thực thể.
     *
     * @return thời gian tạo.
     */
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public String toString() {
        return "EntityDTO{"
                + "id='" + id + '\''
                + ", createdAt=" + createdAt
                + '}';
    }
}