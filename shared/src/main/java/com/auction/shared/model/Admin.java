package com.auction.shared.model;

public class Admin extends User {
    private int adminRoleLevel;

    public Admin(String username, String email, String password) {
        super(username, email, password);
    }

    @Override
    public void displayDashboard() {

    }

    @Override
    public void update(String msg) {

    }

    public void banUser(User user) {
        // to be coded
    }

    public void removeInvalidItem(Item item) {
        // to be coded
    }

    public void resolveDispute() {
        // to be coded
    }

    public int getAdminRoleLevel() {
        return adminRoleLevel;
    }

    public void setAdminRoleLevel(int adminRoleLevel) {
        this.adminRoleLevel = adminRoleLevel;
    }

    @Override
    public String toString() {
        return super.toString() + " -> Admin{" +
                "adminRoleLevel=" + adminRoleLevel +
                '}';
    }
}
