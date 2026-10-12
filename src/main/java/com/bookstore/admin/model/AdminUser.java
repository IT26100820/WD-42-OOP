package com.bookstore.admin.model;

//AdminUser inherits from User
public class AdminUser extends User {
    private String adminLevel;

    //Parameterized constructor
    public AdminUser(String userId, String username, String password, String adminLevel) {
        super(userId, username, password);
        this.adminLevel = adminLevel;
    }

    //Getters
    public String getAdminLevel() {
        return adminLevel;
    }

    //Setters
    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
    }
}