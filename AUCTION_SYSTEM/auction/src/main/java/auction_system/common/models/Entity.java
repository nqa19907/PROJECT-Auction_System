package auction_system.common.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Lớp cơ sở đại diện cho một thực thể trong hệ thống.
 * Cung cấp các thuộc tính chung như id và thời gian tạo.
 */
public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String id;
    protected LocalDateTime createdAt;

    /**
     * Khởi tạo một thực thể mới (tự động sinh UUID).
     */
    public Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Khởi tạo một thực thể với thông tin có sẵn (dùng khi load từ database).
     *
     * @param id        ID của thực thể.
     * @param createdAt Thời gian tạo thực thể.
     */
    public Entity(String id, LocalDateTime createdAt) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID không được để trống!");
        }

        this.id = id;
        this.createdAt = createdAt; // Hoặc bạn sẽ load thời gian từ DB
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Entity)) {
            return false;
        }
        Entity entity = (Entity) o;

        // Dùng Objects.equal để tránh lỗi NullPointerException
        return Objects.equals(id, entity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


    @Override
    public String toString() {
        return String.format("%s{id='%s', createdAt='%s'}",
                this.getClass().getSimpleName(), // Tự động trả về tên lớp con
                id,
                createdAt);
    }
}