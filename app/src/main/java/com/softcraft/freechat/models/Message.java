package com.softcraft.freechat.models;

import java.util.HashMap;
import java.util.Map;

public class Message {
    private String messageId;
    private String senderId;
    private String senderName;
    private String text;
    private long timestamp;
    private String messageType; // "text", "image"
    private String imageBase64; // Base64 encoded image (for free plan)
    private Map<String, Boolean> readBy;
    private Map<String, Boolean> deliveredTo;
    private String status; // "sending", "sent", "delivered", "read"
    private boolean encrypted = true;
    public Message() {
        // Empty constructor for Firebase
    }

    // Constructor for text message
    public Message(String messageId, String senderId, String senderName, String text) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
        this.messageType = "text";
        this.status = "sending";
        this.readBy = new HashMap<>();
        this.deliveredTo = new HashMap<>();
        this.encrypted = true;
    }

    // Constructor for image message
    public Message(String messageId, String senderId, String senderName, String imageBase64, boolean isImage) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.imageBase64 = imageBase64;
        this.timestamp = System.currentTimeMillis();
        this.messageType = "image";
        this.status = "sending";
        this.readBy = new HashMap<>();
        this.deliveredTo = new HashMap<>();
        this.encrypted = true;
    }

    // Getters and Setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public Map<String, Boolean> getReadBy() { return readBy; }
    public void setReadBy(Map<String, Boolean> readBy) { this.readBy = readBy; }

    public Map<String, Boolean> getDeliveredTo() { return deliveredTo; }
    public void setDeliveredTo(Map<String, Boolean> deliveredTo) { this.deliveredTo = deliveredTo; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Helper methods
    public boolean isReadBy(String userId) {
        return readBy != null && readBy.containsKey(userId) && readBy.get(userId);
    }

    public boolean isDeliveredTo(String userId) {
        return deliveredTo != null && deliveredTo.containsKey(userId) && deliveredTo.get(userId);
    }

    public boolean isImage() {
        return "image".equals(messageType);
    }
    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
    public void markAsRead(String userId) {
        if (readBy == null) {
            readBy = new HashMap<>();
        }
        readBy.put(userId, true);

        if (readBy.size() >= 2) {
            this.status = "read";
        }
    }

    public void markAsDelivered(String userId) {
        if (deliveredTo == null) {
            deliveredTo = new HashMap<>();
        }
        deliveredTo.put(userId, true);

        if (deliveredTo.size() >= 2 && "sent".equals(status)) {
            this.status = "delivered";
        }
    }
}