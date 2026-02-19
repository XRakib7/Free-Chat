package com.softcraft.freechat.models;

public class User {
    private String userId;
    private String name;
    private String email;
    private String photoUrl;
    private String status; // "online", "offline"
    private long lastSeen;
    private String fcmToken; // for push notifications
    private long tokenUpdatedAt;

    // Constructors
    public User() {
        // Empty constructor for Firebase
    }

    public User(String userId, String name, String email, String photoUrl) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.photoUrl = photoUrl;
        this.status = "offline";
        this.lastSeen = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public long getTokenUpdatedAt() { return tokenUpdatedAt; }
    public void setTokenUpdatedAt(long tokenUpdatedAt) { this.tokenUpdatedAt = tokenUpdatedAt; }

    public void setOnline(boolean isOnline) {
        this.status = isOnline ? "online" : "offline";
        this.lastSeen = System.currentTimeMillis();
    }

    public boolean isOnline() {
        return "online".equals(status);
    }

    public String getLastSeenText() {
        if (status != null && status.equals("online")) {
            return "Online";
        }

        long now = System.currentTimeMillis();
        long diff = now - lastSeen;

        if (diff < 60000) { // Less than 1 minute
            return "Last seen just now";
        } else if (diff < 3600000) { // Less than 1 hour
            int minutes = (int) (diff / 60000);
            return "Last seen " + minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else if (diff < 86400000) { // Less than 24 hours
            int hours = (int) (diff / 3600000);
            return "Last seen " + hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else {
            int days = (int) (diff / 86400000);
            return "Last seen " + days + " day" + (days > 1 ? "s" : "") + " ago";
        }
    }
}