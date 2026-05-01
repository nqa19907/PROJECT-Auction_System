package auction_system.common.models;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Objects;

public abstract class Entity {
    protected String id;
    protected LocalDateTime createdAt;

    // Constructor 1: Dùng khi TẠO MÓI (Tự động sinh UUID)
    public Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    // Constructor 2: Dùng khi LOAD từ DATABASE (Đã có ID sẵn)
    public Entity(String id, LocalDateTime createdAt) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID không được để trống!");
        }

        this.id = id;
        this.createdAt = createdAt; // Hoặc bạn sẽ load thời gian từ DB
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entity)) return false;
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
        return "Entity{" +
                "id='" + id + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }

    //    @Override
//    public String toString() {
//        return String.format("%s{id='%s', createdAt='%s'}",
//                this.getClass().getSimpleName(), // Tự động trả về tên lớp con
//                id,
//                createdAt);
//    }
}