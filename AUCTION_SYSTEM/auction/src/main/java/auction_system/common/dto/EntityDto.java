package auction_system.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lớp Dto cơ sở đại diện cho một thực thể trong hệ thống.
 * Cung cấp các thuộc tính chung như id và thời gian tạo.
 */
public abstract class EntityDto implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String id;
    protected LocalDateTime createdAt;

    /**
     * Khởi tạo một đối tượng EntityDto trống.
     */
    public EntityDto() {
    }

    /**
     * Khởi tạo một đối tượng EntityDto với thông tin có sẵn.
     *
     * @param id        ID của thực thể.
     * @param createdAt Thời gian tạo.
     */
    public EntityDto(String id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{"
                + "id='" + id + '\''
                + ", createdAt=" + createdAt
                + '}';
    }
}
