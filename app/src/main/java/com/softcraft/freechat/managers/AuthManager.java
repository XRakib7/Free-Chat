package com.softcraft.freechat.managers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.softcraft.freechat.models.AuthState;
import com.softcraft.freechat.models.User;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private AuthListener authListener;

    public interface AuthListener {
        void onAuthSuccess(AuthState authState);
        void onAuthFailure(String error);
    }

    public AuthManager() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public void setAuthListener(AuthListener listener) {
        this.authListener = listener;
    }

    // Check if user is already logged in
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    // Sign up with email and password
    public void signUp(String email, String password, String name) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign up success
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "createUserWithEmail:success");

                            // Create user profile in Firestore
                            createUserProfile(user, name);

                        } else {
                            // Sign up fails
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            if (authListener != null) {
                                authListener.onAuthFailure(task.getException().getMessage());
                            }
                        }
                    }
                });
    }

    // Sign in with email and password
    public void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "signInWithEmail:success");

                            // Update user status to online using PresenceManager
                            PresenceManager.getInstance().setUserOnline();

                            AuthState authState = new AuthState(
                                    true,
                                    user.getUid(),
                                    user.getEmail()
                            );

                            if (authListener != null) {
                                authListener.onAuthSuccess(authState);
                            }

                        } else {
                            // Sign in fails
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            if (authListener != null) {
                                authListener.onAuthFailure(task.getException().getMessage());
                            }
                        }
                    }
                });
    }

    // Create user profile in Firestore
    private void createUserProfile(FirebaseUser firebaseUser, String name) {
        String userId = firebaseUser.getUid();
        String email = firebaseUser.getEmail();

        // Create a new user object
        User newUser = new User(
                userId,
                name,
                email,
                "https://ui-avatars.com/api/?name=" + name + "&size=512" // Default avatar
        );

        // Add user to Firestore
        db.collection("users")
                .document(userId)
                .set(newUser)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile created");

                            AuthState authState = new AuthState(
                                    true,
                                    userId,
                                    email
                            );

                            if (authListener != null) {
                                authListener.onAuthSuccess(authState);
                            }
                        } else {
                            Log.w(TAG, "Error creating user profile", task.getException());
                            if (authListener != null) {
                                authListener.onAuthFailure("Failed to create user profile");
                            }
                        }
                    }
                });
    }

    // Sign out
    public void signOut() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Set offline using PresenceManager before signing out
            PresenceManager.getInstance().setUserOffline();
        }
        mAuth.signOut();
    }

    // Update user online/offline status
    private void updateUserStatus(String userId, String status) {
        db.collection("users")
                .document(userId)
                .update(
                        "status", status,
                        "lastSeen", System.currentTimeMillis()
                )
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User status updated to: " + status);
                        } else {
                            Log.w(TAG, "Error updating status", task.getException());
                        }
                    }
                });
    }

    // Send password reset email
    public void resetPassword(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Password reset email sent");
                            if (authListener != null) {
                                authListener.onAuthSuccess(null); // You might want a different callback
                            }
                        } else {
                            Log.w(TAG, "Error sending password reset email", task.getException());
                            if (authListener != null) {
                                authListener.onAuthFailure(task.getException().getMessage());
                            }
                        }
                    }
                });
    }
}