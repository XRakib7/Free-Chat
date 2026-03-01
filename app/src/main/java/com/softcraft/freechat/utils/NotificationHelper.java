package com.softcraft.freechat.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";

    public static void updateFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d(TAG, "FCM Token: " + token);
                    saveTokenToFirestore(token);
                });
    }

    private static void saveTokenToFirestore(String token) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) return;

        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("fcmToken", token);
        tokenMap.put("tokenUpdatedAt", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update(tokenMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved"))
                .addOnFailureListener(e -> Log.w(TAG, "Error saving token", e));
    }
}
