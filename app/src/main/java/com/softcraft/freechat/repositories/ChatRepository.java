package com.softcraft.freechat.repositories;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.softcraft.freechat.models.Chat;
import com.softcraft.freechat.models.Message;
import com.softcraft.freechat.models.User;
import com.softcraft.freechat.utils.ImageUtils;
import com.softcraft.freechat.utils.NotificationServerHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {
    private static final String TAG = "ChatRepository";
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public ChatRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // Get all chats for current user with real-time updates
    public void getChats(final OnChatsLoadedListener listener) {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("chats")
                .whereArrayContains("participantIds", currentUserId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot value, FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.w(TAG, "Listen failed.", error);
                            listener.onError(error.getMessage());
                            return;
                        }

                        List<Chat> chats = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            Chat chat = doc.toObject(Chat.class);
                            chat.setChatId(doc.getId());
                            chats.add(chat);
                        }

                        // After getting chats, fetch user details for participants
                        fetchParticipantsDetails(chats, listener);
                    }
                });
    }

    // Fetch details of all users involved in chats
    private void fetchParticipantsDetails(List<Chat> chats, final OnChatsLoadedListener listener) {
        Map<String, User> userMap = new HashMap<>();
        List<String> userIdsToFetch = new ArrayList<>();

        // Collect all participant IDs
        for (Chat chat : chats) {
            for (String userId : chat.getParticipantIds()) {
                if (!userId.equals(auth.getCurrentUser().getUid()) && !userIdsToFetch.contains(userId)) {
                    userIdsToFetch.add(userId);
                }
            }
        }

        if (userIdsToFetch.isEmpty()) {
            listener.onChatsLoaded(chats, userMap);
            return;
        }

        // Fetch each user's details
        for (String userId : userIdsToFetch) {
            db.collection("users").document(userId)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                User user = task.getResult().toObject(User.class);
                                if (user != null) {
                                    user.setUserId(userId);
                                    userMap.put(userId, user);
                                }
                            }

                            // Check if we've fetched all users
                            if (userMap.size() == userIdsToFetch.size()) {
                                listener.onChatsLoaded(chats, userMap);
                            }
                        }
                    });
        }
    }

    // Get messages for a specific chat with real-time updates
    public void getMessages(String chatId, final OnMessagesLoadedListener listener) {
        if (chatId == null || chatId.isEmpty()) {
            Log.e(TAG, "Chat ID is null or empty");
            listener.onError("Chat ID is invalid");
            return;
        }

        db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot value, FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.w(TAG, "Listen failed.", error);
                            listener.onError(error.getMessage());
                            return;
                        }

                        List<Message> messages = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            Message message = doc.toObject(Message.class);
                            message.setMessageId(doc.getId());
                            messages.add(message);
                        }

                        listener.onMessagesLoaded(messages);
                    }
                });
    }

    // Send a new message with proper status tracking
    public void sendMessage(String chatId, String encryptedText, String senderName, OnMessageSentListener listener) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String messageId = db.collection("chats").document(chatId).collection("messages").document().getId();

        Message message = new Message(messageId, currentUserId, senderName, encryptedText);

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .set(message)
                .addOnSuccessListener(aVoid -> {
                    // Update chat last message
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("lastMessage", encryptedText.length() > 30 ? encryptedText.substring(0,30)+"..." : encryptedText);
                    updates.put("lastMessageTime", System.currentTimeMillis());
                    updates.put("lastMessageSenderId", currentUserId);

                    db.collection("chats").document(chatId).update(updates);

                    // Send push notification (use "New message" because we don't have plain text here)
                    sendPushNotification(chatId, "New message", senderName, false);

                    if (listener != null) listener.onMessageSent(true, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending message", e);
                    if (listener != null) listener.onMessageSent(false, e.getMessage());
                });
    }

    public void sendImageMessage(String chatId, String encryptedImage, String senderName, OnMessageSentListener listener) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String messageId = db.collection("chats").document(chatId).collection("messages").document().getId();

        Message message = new Message(messageId, currentUserId, senderName, encryptedImage, true);

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .set(message)
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("lastMessage", "📷 Image");
                    updates.put("lastMessageTime", System.currentTimeMillis());
                    updates.put("lastMessageSenderId", currentUserId);

                    db.collection("chats").document(chatId).update(updates);

                    sendPushNotification(chatId, "📷 Image", senderName, true);

                    if (listener != null) listener.onMessageSent(true, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending image", e);
                    if (listener != null) listener.onMessageSent(false, e.getMessage());
                });
    }
    private void sendPushNotification(String chatId, String messageContent, String senderName, boolean isImage) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getRecipientToken(chatId, currentUserId, (token, recipientId) -> {
            if (token != null && !token.isEmpty()) {
                String title = "New message from " + senderName;
                String body = isImage ? "📷 Image" : messageContent;
                if (!isImage && body.length() > 50) {
                    body = body.substring(0, 50) + "...";
                }

                JSONObject data = new JSONObject();
                try {
                    data.put("chatId", chatId);
                    data.put("senderId", currentUserId);
                    data.put("senderName", senderName);
                    data.put("type", isImage ? "image" : "text");
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating JSON data", e);
                }

                NotificationServerHelper.sendNotification(token, title, body, data, null);
            } else {
                Log.d(TAG, "No FCM token for recipient: " + recipientId);
            }
        });
    }
    // Update chat's last message
    private void updateChatLastMessage(String chatId, String messageType) {
        String lastMessagePlaceholder;
        if ("image".equals(messageType)) {
            lastMessagePlaceholder = "📷 Image";
        } else {
            lastMessagePlaceholder = "💬 New message";
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", lastMessagePlaceholder);
        updates.put("lastMessageTime", System.currentTimeMillis());
        updates.put("lastMessageSenderId", auth.getCurrentUser().getUid());

        db.collection("chats").document(chatId)
                .update(updates)
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to update last message", e));
    }

    private void getRecipientToken(String chatId, String currentUserId, RecipientTokenCallback callback) {
        db.collection("chats")
                .document(chatId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> participants = (List<String>) documentSnapshot.get("participantIds");
                        if (participants != null) {
                            for (String id : participants) {
                                if (!id.equals(currentUserId)) {
                                    // Fetch that user's token
                                    db.collection("users")
                                            .document(id)
                                            .get()
                                            .addOnSuccessListener(userDoc -> {
                                                if (userDoc.exists()) {
                                                    String token = userDoc.getString("fcmToken");
                                                    callback.onResult(token, id);
                                                } else {
                                                    callback.onResult(null, null);
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error fetching user", e);
                                                callback.onResult(null, null);
                                            });
                                    return;
                                }
                            }
                        }
                    }
                    callback.onResult(null, null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching chat", e);
                    callback.onResult(null, null);
                });
    }

    interface RecipientTokenCallback {
        void onResult(String token, String recipientId);
    }

    // Mark message as delivered
    public void markMessageAsDelivered(String chatId, String messageId, String recipientId) {
        if (chatId == null || messageId == null) return;

        DocumentReference messageRef = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId);

        messageRef.get().addOnSuccessListener(documentSnapshot -> {
            Message message = documentSnapshot.toObject(Message.class);
            if (message != null) {
                db.collection("chats").document(chatId).get()
                        .addOnSuccessListener(chatDoc -> {
                            Chat chat = chatDoc.toObject(Chat.class);
                            if (chat != null && chat.getParticipantIds() != null) {
                                List<String> participantIds = chat.getParticipantIds();

                                // Check if message is delivered to all participants
                                boolean allDelivered = true;
                                Map<String, Boolean> deliveredTo = message.getDeliveredTo();
                                if (deliveredTo == null) deliveredTo = new HashMap<>();

                                // Add current delivery
                                deliveredTo.put(recipientId, true);

                                for (String participantId : participantIds) {
                                    if (!participantId.equals(message.getSenderId())) {
                                        if (!deliveredTo.containsKey(participantId) || !deliveredTo.get(participantId)) {
                                            allDelivered = false;
                                            break;
                                        }
                                    }
                                }

                                Map<String, Object> updates = new HashMap<>();
                                updates.put("deliveredTo." + recipientId, true);

                                if (allDelivered && "sent".equals(message.getStatus())) {
                                    updates.put("status", "delivered");
                                }

                                messageRef.update(updates);
                            }
                        });
            }
        });
    }

    // Mark message as read
    public void markMessageAsRead(String chatId, String messageId, String readerId) {
        if (chatId == null || messageId == null) return;

        DocumentReference messageRef = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId);

        messageRef.get().addOnSuccessListener(documentSnapshot -> {
            Message message = documentSnapshot.toObject(Message.class);
            if (message != null && !message.getSenderId().equals(readerId)) {
                db.collection("chats").document(chatId).get()
                        .addOnSuccessListener(chatDoc -> {
                            Chat chat = chatDoc.toObject(Chat.class);
                            if (chat != null && chat.getParticipantIds() != null) {
                                List<String> participantIds = chat.getParticipantIds();

                                // Check if message is read by all participants
                                boolean allRead = true;
                                Map<String, Boolean> readBy = message.getReadBy();
                                if (readBy == null) readBy = new HashMap<>();

                                // Add current reader
                                readBy.put(readerId, true);

                                for (String participantId : participantIds) {
                                    if (!participantId.equals(message.getSenderId())) {
                                        if (!readBy.containsKey(participantId) || !readBy.get(participantId)) {
                                            allRead = false;
                                            break;
                                        }
                                    }
                                }

                                Map<String, Object> updates = new HashMap<>();
                                updates.put("readBy." + readerId, true);

                                if (allRead) {
                                    updates.put("status", "read");
                                } else if (!"read".equals(message.getStatus())) {
                                    // Check if at least delivered
                                    boolean allDelivered = true;
                                    Map<String, Boolean> deliveredTo = message.getDeliveredTo();
                                    if (deliveredTo == null) deliveredTo = new HashMap<>();

                                    for (String participantId : participantIds) {
                                        if (!participantId.equals(message.getSenderId())) {
                                            if (!deliveredTo.containsKey(participantId) || !deliveredTo.get(participantId)) {
                                                allDelivered = false;
                                                break;
                                            }
                                        }
                                    }

                                    if (allDelivered && "sent".equals(message.getStatus())) {
                                        updates.put("status", "delivered");
                                    }
                                }

                                messageRef.update(updates);
                            }
                        });
            }
        });
    }

    // Mark all messages in a chat as read
    public void markChatAsRead(String chatId) {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .whereNotEqualTo("senderId", currentUserId)
                .whereEqualTo("status", "delivered")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        markMessageAsRead(chatId, doc.getId(), currentUserId);
                    }
                });
    }

    // Create a new private chat or get existing one
    public void getOrCreateChat(String otherUserId, final OnChatCreatedListener listener) {
        String currentUserId = auth.getCurrentUser().getUid();

        // Check if chat already exists
        db.collection("chats")
                .whereArrayContains("participantIds", currentUserId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Chat chat = document.toObject(Chat.class);
                                if (chat.getParticipantIds() != null &&
                                        chat.getParticipantIds().contains(otherUserId)) {
                                    // Chat already exists
                                    listener.onChatCreated(document.getId());
                                    return;
                                }
                            }

                            // Create new chat with proper structure
                            List<String> participants = new ArrayList<>();
                            participants.add(currentUserId);
                            participants.add(otherUserId);

                            Map<String, Object> newChat = new HashMap<>();
                            newChat.put("participantIds", participants);
                            newChat.put("chatType", "private");
                            newChat.put("lastMessageTime", System.currentTimeMillis());
                            newChat.put("createdAt", System.currentTimeMillis());

                            // Initialize typing map
                            Map<String, Boolean> typingMap = new HashMap<>();
                            typingMap.put(currentUserId, false);
                            typingMap.put(otherUserId, false);
                            newChat.put("typing", typingMap);
                            newChat.put("typingTimestamp", System.currentTimeMillis());

                            db.collection("chats")
                                    .add(newChat)
                                    .addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentReference> task) {
                                            if (task.isSuccessful()) {
                                                listener.onChatCreated(task.getResult().getId());
                                            } else {
                                                listener.onError(task.getException() != null ?
                                                        task.getException().getMessage() : "Failed to create chat");
                                            }
                                        }
                                    });
                        } else {
                            listener.onError(task.getException() != null ?
                                    task.getException().getMessage() : "Failed to check existing chats");
                        }
                    }
                });
    }

    // Listen for message status changes
    public void listenForMessageStatus(String chatId, String messageId, OnMessageStatusChangedListener listener) {
        if (chatId == null || messageId == null) return;

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Message message = snapshot.toObject(Message.class);
                        if (message != null) {
                            listener.onStatusChanged(message.getStatus(), message.getReadBy());
                        }
                    }
                });
    }

    // Interfaces for callbacks
    public interface OnChatsLoadedListener {
        void onChatsLoaded(List<Chat> chats, Map<String, User> userMap);
        void onError(String error);
    }


    public interface OnMessagesLoadedListener {
        void onMessagesLoaded(List<Message> messages);
        void onError(String error);
    }

    public interface OnMessageSentListener {
        void onMessageSent(boolean success, String error);
    }

    public interface OnChatCreatedListener {
        void onChatCreated(String chatId);
        void onError(String error);
    }

    public interface OnMessageStatusChangedListener {
        void onStatusChanged(String status, Map<String, Boolean> readBy);
    }
}