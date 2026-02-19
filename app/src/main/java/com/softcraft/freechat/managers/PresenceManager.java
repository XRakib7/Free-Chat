package com.softcraft.freechat.managers;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class PresenceManager {
    private static final String TAG = "PresenceManager";
    private static PresenceManager instance;
    private static final long PRESENCE_TIMEOUT = 60000; // 60 seconds

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private DatabaseReference databaseReference;
    private ListenerRegistration firestoreListener;
    private String currentUserId;
    private boolean isUserOnline = false;

    private PresenceManager() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }
    }

    public static synchronized PresenceManager getInstance() {
        if (instance == null) {
            instance = new PresenceManager();
        }
        return instance;
    }

    // Set user online with proper disconnect handling
    public void setUserOnline() {
        if (currentUserId == null) {
            if (auth.getCurrentUser() != null) {
                currentUserId = auth.getCurrentUser().getUid();
            } else {
                return;
            }
        }

        isUserOnline = true;

        // Update Firestore
        updateFirestoreStatus("online");

        // Set up Realtime Database presence for better disconnect handling
        DatabaseReference userPresenceRef = databaseReference
                .child("presence")
                .child(currentUserId);

        // Set up disconnect - this will trigger when the app closes or loses connection
        // Use a Map with ServerValue for proper disconnect handling
        Map<String, Object> offlineMap = new HashMap<>();
        offlineMap.put("online", false);
        offlineMap.put("lastSeen", ServerValue.TIMESTAMP);
        userPresenceRef.onDisconnect().setValue(offlineMap);

        // Set current value
        Map<String, Object> onlineMap = new HashMap<>();
        onlineMap.put("online", true);
        onlineMap.put("lastSeen", ServerValue.TIMESTAMP);
        userPresenceRef.setValue(onlineMap);

        Log.d(TAG, "User " + currentUserId + " set to online");
    }

    // Set user offline
    public void setUserOffline() {
        if (currentUserId == null) return;

        isUserOnline = false;

        // Update Firestore
        updateFirestoreStatus("offline");

        // Update Realtime Database
        Map<String, Object> offlineMap = new HashMap<>();
        offlineMap.put("online", false);
        offlineMap.put("lastSeen", ServerValue.TIMESTAMP);

        databaseReference
                .child("presence")
                .child(currentUserId)
                .setValue(offlineMap);

        Log.d(TAG, "User " + currentUserId + " set to offline");
    }

    // Update status in Firestore
    private void updateFirestoreStatus(String status) {
        if (currentUserId == null) return;

        DocumentReference userRef = firestore.collection("users").document(currentUserId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("lastSeen", System.currentTimeMillis());

        userRef.update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Firestore status updated to: " + status))
                .addOnFailureListener(e -> Log.w(TAG, "Error updating Firestore status", e));
    }

    // Listen to another user's presence (combines Firestore and Realtime DB)
    public ListenerRegistration listenToUserPresence(String userId, PresenceListener listener) {
        if (userId == null) return null;

        // First, listen to Firestore for basic status
        DocumentReference userRef = firestore.collection("users").document(userId);

        return userRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Firestore listen failed.", e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                String status = snapshot.getString("status");
                Long lastSeen = snapshot.getLong("lastSeen");

                // Also check Realtime Database for more accurate presence
                checkRealtimePresence(userId, status, lastSeen, listener);
            }
        });
    }

    // Check Realtime Database for more accurate presence
    private void checkRealtimePresence(String userId, String firestoreStatus,
                                       Long firestoreLastSeen, PresenceListener listener) {
        databaseReference.child("presence").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get the presence data as a Map
                    Object presenceValue = dataSnapshot.getValue();

                    boolean isOnline = false;
                    long lastSeen = firestoreLastSeen != null ? firestoreLastSeen : System.currentTimeMillis();

                    if (presenceValue instanceof Map) {
                        // New format with map
                        Map<String, Object> presenceMap = (Map<String, Object>) presenceValue;
                        Object onlineValue = presenceMap.get("online");
                        Object lastSeenValue = presenceMap.get("lastSeen");

                        if (onlineValue instanceof Boolean) {
                            isOnline = (Boolean) onlineValue;
                        } else if (onlineValue instanceof String) {
                            isOnline = Boolean.parseBoolean((String) onlineValue);
                        }

                        if (lastSeenValue instanceof Long) {
                            lastSeen = (Long) lastSeenValue;
                        }
                    } else if (presenceValue instanceof Boolean) {
                        // Old format with just boolean
                        isOnline = (Boolean) presenceValue;
                    } else if (presenceValue instanceof String) {
                        // Handle string values
                        isOnline = Boolean.parseBoolean((String) presenceValue);
                    }

                    String finalStatus = isOnline ? "online" : "offline";
                    listener.onPresenceChanged(finalStatus, lastSeen);
                } else {
                    // No presence data, use Firestore data
                    listener.onPresenceChanged(
                            firestoreStatus != null ? firestoreStatus : "offline",
                            firestoreLastSeen != null ? firestoreLastSeen : System.currentTimeMillis()
                    );
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "Realtime DB listen failed", databaseError.toException());
                // Fallback to Firestore data
                listener.onPresenceChanged(
                        firestoreStatus != null ? firestoreStatus : "offline",
                        firestoreLastSeen != null ? firestoreLastSeen : System.currentTimeMillis()
                );
            }
        });
    }

    // Get user's last seen text
    public String getLastSeenText(long lastSeen) {
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

    // Clean up listeners
    public void removeListeners() {
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }

    public interface PresenceListener {
        void onPresenceChanged(String status, long lastSeen);
    }

    public boolean isUserOnline() {
        return isUserOnline;
    }
}