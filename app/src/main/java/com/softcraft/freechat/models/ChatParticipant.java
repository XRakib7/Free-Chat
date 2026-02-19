package com.softcraft.freechat.models;

public class ChatParticipant {
    private String userId;
    private String chatId;
    private long joinedAt;
    private String role; // "admin", "member" for groups
    private boolean isMuted;
    private String lastReadMessageId; // Track last read message

    public ChatParticipant() {
        // Empty constructor for Firebase
    }

    public ChatParticipant(String userId, String chatId) {
        this.userId = userId;
        this.chatId = chatId;
        this.joinedAt = System.currentTimeMillis();
        this.role = "member";
        this.isMuted = false;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public long getJoinedAt() { return joinedAt; }
    public void setJoinedAt(long joinedAt) { this.joinedAt = joinedAt; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isMuted() { return isMuted; }
    public void setMuted(boolean muted) { isMuted = muted; }

    public String getLastReadMessageId() { return lastReadMessageId; }
    public void setLastReadMessageId(String lastReadMessageId) { this.lastReadMessageId = lastReadMessageId; }
}