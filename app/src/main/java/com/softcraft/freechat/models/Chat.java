package com.softcraft.freechat.models;

import java.util.List;
import java.util.Map;

public class Chat {
    private String chatId;
    private List<String> participantIds; // User IDs in this chat
    private Map<String, String> participantNames; // Quick access to names
    private String lastMessage;
    private long lastMessageTime;
    private String lastMessageSenderId;
    private String chatType; // "private" or "group"
    private String chatName; // For group chats

    public Chat() {
        // Empty constructor for Firebase
    }

    public Chat(String chatId, List<String> participantIds, String chatType) {
        this.chatId = chatId;
        this.participantIds = participantIds;
        this.chatType = chatType;
        this.lastMessageTime = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public List<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<String> participantIds) { this.participantIds = participantIds; }

    public Map<String, String> getParticipantNames() { return participantNames; }
    public void setParticipantNames(Map<String, String> participantNames) { this.participantNames = participantNames; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(long lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }

    public String getChatType() { return chatType; }
    public void setChatType(String chatType) { this.chatType = chatType; }

    public String getChatName() { return chatName; }
    public void setChatName(String chatName) { this.chatName = chatName; }
}