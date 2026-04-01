package models;

public abstract class User extends Entity {
    private String username = "";
    private String password = "";
    private String email = "";
    private String userId = "";

    public User(String username, String password, String email, String userId) {
        super();
        this.username = username;
        this.password = password;
        this.email = email;
        this.userId = userId;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "userId: " + getUserId() + ", username: " + getUsername() + ", password: " + getPassword() + ", email:"
                + getEmail();
    }

}