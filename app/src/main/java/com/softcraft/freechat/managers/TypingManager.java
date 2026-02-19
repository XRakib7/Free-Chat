package com.softcraft.freechat.managers;

import android.os.Handler;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TypingManager {
    private static final String TAG = "TypingManager";
    private static final long TYPING_TIMEOUT = 3000; // 3 seconds

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Handler typingHandler;
    private Runnable stopTypingRunnable;
    private String currentChatId;
    private String currentUserId;
    private boolean isTyping = false;

    public TypingManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        typingHandler = new Handler();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }
    }

    // Set typing status for a chat - store directly in chat document
    public void setTyping(String chatId, boolean typing) {
        if (chatId == null || currentUserId == null) return;

        DocumentReference chatRef = db.collection("chats").document(chatId);

        // Store typing status in a map inside the chat document
        Map<String, Object> typingData = new HashMap<>();
        typingData.put("typing." + currentUserId, typing);
        typingData.put("typing.timestamp", System.currentTimeMillis());

        chatRef.update(typingData)
                .addOnFailureListener(e -> Log.w(TAG, "Error setting typing status", e));

        if (typing) {
            // Set timeout to auto-reset typing
            setupTypingTimeout(chatId);
        } else {
            if (typingHandler != null) {
                typingHandler.removeCallbacks(stopTypingRunnable);
            }
        }

        this.currentChatId = chatId;
        this.isTyping = typing;
    }

    private void setupTypingTimeout(String chatId) {
        if (stopTypingRunnable != null) {
            typingHandler.removeCallbacks(stopTypingRunnable);
        }

        stopTypingRunnable = () -> {
            if (isTyping && chatId.equals(currentChatId)) {
                setTyping(chatId, false);
            }
        };

        typingHandler.postDelayed(stopTypingRunnable, TYPING_TIMEOUT);
    }

    // Listen to typing status in a chat
    public void listenToTyping(String chatId, TypingListener listener) {
        if (chatId == null || currentUserId == null) return;

        db.collection("chats").document(chatId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        boolean someoneTyping = false;

                        // Check all fields that start with "typing."
                        Map<String, Object> data = snapshot.getData();
                        if (data != null) {
                            for (Map.Entry<String, Object> entry : data.entrySet()) {
                                String key = entry.getKey();
                                if (key.startsWith("typing.") && !key.equals("typing.timestamp")) {
                                    String userId = key.substring(7); // Remove "typing." prefix
                                    Boolean isTyping = (Boolean) entry.getValue();

                                    // Check if it's not the current user and they are typing
                                    if (!userId.equals(currentUserId) && isTyping != null && isTyping) {
                                        someoneTyping = true;
                                        break;
                                    }
                                }
                            }
                        }

                        listener.onTypingStatusChanged(someoneTyping);
                    }
                });
    }

    public void cleanup() {
        if (typingHandler != null) {
            typingHandler.removeCallbacksAndMessages(null);
        }
        if (currentChatId != null && isTyping && currentUserId != null) {
            setTyping(currentChatId, false);
        }
    }

    public interface TypingListener {
        void onTypingStatusChanged(boolean isTyping);
    }
}