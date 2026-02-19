package com.softcraft.freechat.models;

public class AuthState {
    private boolean isAuthenticated;
    private String userId;
    private String userEmail;
    private String errorMessage;

    public AuthState(boolean isAuthenticated, String userId, String userEmail) {
        this.isAuthenticated = isAuthenticated;
        this.userId = userId;
        this.userEmail = userEmail;
        this.errorMessage = null;
    }

    public AuthState(String errorMessage) {
        this.isAuthenticated = false;
        this.userId = null;
        this.userEmail = null;
        this.errorMessage = errorMessage;
    }

    // Getters
    public boolean isAuthenticated() { return isAuthenticated; }
    public String getUserId() { return userId; }
    public String getUserEmail() { return userEmail; }
    public String getErrorMessage() { return errorMessage; }
}