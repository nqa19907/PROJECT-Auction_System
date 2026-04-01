package models;

import java.util.UUID;
import java.time.LocalDateTime;

public abstract class Entity {
    private String createdAt;
    private String id = "";

    public Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now().toString();
    }

    public String getId() {
        return id;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public abstract void printInfo();

    @Override
    public String toString() {
        return "id = " + id + "," + "created at : " + createdAt;
    }
}