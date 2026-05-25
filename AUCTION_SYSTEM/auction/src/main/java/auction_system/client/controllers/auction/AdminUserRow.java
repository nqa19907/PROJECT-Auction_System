package auction_system.client.controllers.auction;

import javafx.beans.property.SimpleStringProperty;

/**
 * DTO hien thi mot dong user tren bang Admin Dashboard.
 */
public class AdminUserRow {

    private final SimpleStringProperty id;
    private final SimpleStringProperty username;
    private final SimpleStringProperty email;
    private final SimpleStringProperty status;

    /**
     * Tao mot dong hien thi user.
     *
     * @param id id user
     * @param username ten dang nhap
     * @param email email user
     * @param status trang thai online/offline
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

    /**
     * Lay id user.
     *
     * @return id user
     */
    public String getId() {
        return id.get();
    }

    /**
     * Lay ten dang nhap.
     *
     * @return ten dang nhap
     */
    public String getUsername() {
        return username.get();
    }

    /**
     * Lay email user.
     *
     * @return email user
     */
    public String getEmail() {
        return email.get();
    }

    /**
     * Lay trang thai user.
     *
     * @return ONLINE hoac OFFLINE
     */
    public String getStatus() {
        return status.get();
    }
}
