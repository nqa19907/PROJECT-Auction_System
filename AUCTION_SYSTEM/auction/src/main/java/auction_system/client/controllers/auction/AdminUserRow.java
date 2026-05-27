package auction_system.client.controllers.auction;

import javafx.beans.property.SimpleStringProperty;

/**
 * DTO hiển thị một dòng user trên bảng quản trị.
 */
public class AdminUserRow {
    private final SimpleStringProperty id;
    private final SimpleStringProperty username;
    private final SimpleStringProperty email;
    private final SimpleStringProperty status;

    /**
     * Tạo dữ liệu hiển thị cho một user.
     *
     * @param id id user
     * @param username tên đăng nhập
     * @param email email user
     * @param status trạng thái online/offline
     */
    public AdminUserRow(
            final String id,
            final String username,
            final String email,
            final String status) {

        this.id = new SimpleStringProperty(id);
        this.username = new SimpleStringProperty(username);
        this.email = new SimpleStringProperty(email);
        this.status = new SimpleStringProperty(status);
    }

    public String getId() {
        return id.get();
    }

    public String getUsername() {
        return username.get();
    }

    public String getEmail() {
        return email.get();
    }

    public String getStatus() {
        return status.get();
    }
}
