package com.softcraft.freechat.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.softcraft.freechat.R;
import com.softcraft.freechat.models.User;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 101;

    private Toolbar toolbar;
    private ImageView imageViewProfile;
    private FloatingActionButton fabChangePhoto;
    private ImageView imageViewEditName;
    private ImageView imageViewEditStatus;
    private EditText editTextName;
    private TextView textViewEmail;
    private TextView textViewStatus;
    private TextView textViewChatsCount;
    private TextView textViewMessagesCount;
    private Button buttonSave;
    private ProgressBar progressBar;

    private LinearLayout layoutChangePassword;
    private LinearLayout layoutPrivacy;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private String currentUserId;
    private User currentUser;
    private Uri newImageUri;
    private boolean isEditingName = false;
    private boolean isEditingStatus = false;
    private boolean isImageChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        currentUserId = auth.getCurrentUser().getUid();

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup click listeners
        setupClickListeners();

        // Load user data
        loadUserData();

        // Load stats
        loadUserStats();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        imageViewProfile = findViewById(R.id.imageViewProfile);
        fabChangePhoto = findViewById(R.id.fabChangePhoto);
        imageViewEditName = findViewById(R.id.imageViewEditName);
        imageViewEditStatus = findViewById(R.id.imageViewEditStatus);
        editTextName = findViewById(R.id.editTextName);
        textViewEmail = findViewById(R.id.textViewEmail);
        textViewStatus = findViewById(R.id.textViewStatus);
        textViewChatsCount = findViewById(R.id.textViewChatsCount);
        textViewMessagesCount = findViewById(R.id.textViewMessagesCount);
        buttonSave = findViewById(R.id.buttonSave);
        progressBar = findViewById(R.id.progressBar);
        layoutChangePassword = findViewById(R.id.layoutChangePassword);
        layoutPrivacy = findViewById(R.id.layoutPrivacy);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profile");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupClickListeners() {

        imageViewEditName.setOnClickListener(v -> {
            isEditingName = true;
            editTextName.setEnabled(true);
            editTextName.requestFocus();
            editTextName.setSelection(editTextName.getText().length());
            buttonSave.setVisibility(View.VISIBLE);
        });

        imageViewEditStatus.setOnClickListener(v -> {
            showEditStatusDialog();
        });

        buttonSave.setOnClickListener(v -> saveChanges());

        layoutChangePassword.setOnClickListener(v -> {
            showChangePasswordDialog();
        });

        layoutPrivacy.setOnClickListener(v -> {
            // Navigate to privacy settings (can be part of SettingsActivity)
            Toast.makeText(this, "Privacy settings coming soon", Toast.LENGTH_SHORT).show();
        });
    }



    private void showEditStatusDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Update Status");

        final EditText input = new EditText(this);
        input.setText(textViewStatus.getText());
        input.setSelection(input.getText().length());
        input.setMaxLines(3);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newStatus = input.getText().toString().trim();
            if (!TextUtils.isEmpty(newStatus)) {
                textViewStatus.setText(newStatus);
                buttonSave.setVisibility(View.VISIBLE);
                isEditingStatus = true;
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showChangePasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText editTextCurrentPassword = view.findViewById(R.id.editTextCurrentPassword);
        EditText editTextNewPassword = view.findViewById(R.id.editTextNewPassword);
        EditText editTextConfirmPassword = view.findViewById(R.id.editTextConfirmPassword);

        builder.setView(view);

        builder.setPositiveButton("Change", (dialog, which) -> {
            String currentPassword = editTextCurrentPassword.getText().toString();
            String newPassword = editTextNewPassword.getText().toString();
            String confirmPassword = editTextConfirmPassword.getText().toString();

            if (TextUtils.isEmpty(currentPassword)) {
                Toast.makeText(this, "Current password is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(newPassword)) {
                Toast.makeText(this, "New password is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            changePassword(currentPassword, newPassword);
        });

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }

    private void changePassword(String currentPassword, String newPassword) {
        showLoading(true);

        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            // Re-authenticate user
            com.google.firebase.auth.AuthCredential credential =
                    com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Change password
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(passwordTask -> {
                                        showLoading(false);
                                        if (passwordTask.isSuccessful()) {
                                            Toast.makeText(ProfileActivity.this,
                                                    "Password updated successfully", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(ProfileActivity.this,
                                                    "Failed to update password", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            showLoading(false);
                            Toast.makeText(ProfileActivity.this,
                                    "Current password is incorrect", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void loadUserData() {
        showLoading(true);

        db.collection("users").document(currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful() && task.getResult() != null) {
                        currentUser = task.getResult().toObject(User.class);
                        if (currentUser != null) {
                            displayUserData();
                        }
                    } else {
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayUserData() {
        if (currentUser != null) {
            editTextName.setText(currentUser.getName());
            textViewEmail.setText(currentUser.getEmail());

            String status = currentUser.getStatus();
            if (status != null && !status.isEmpty()) {
                textViewStatus.setText(status);
            } else {
                textViewStatus.setText("Hey there! I'm using FreeChat");
            }

            if (currentUser.getPhotoUrl() != null && !currentUser.getPhotoUrl().isEmpty()) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(imageViewProfile);
            }
        }
    }

    private void loadUserStats() {
        // Load chats count
        db.collection("chats")
                .whereArrayContains("participantIds", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int chatsCount = task.getResult().size();
                        textViewChatsCount.setText(String.valueOf(chatsCount));

                        // Load messages count
                        loadMessagesCount(task.getResult());
                    }
                });
    }

    private void loadMessagesCount(QuerySnapshot chatSnapshots) {
        int[] totalMessages = {0};
        int[] processedChats = {0};
        int totalChats = chatSnapshots.size();

        if (totalChats == 0) {
            textViewMessagesCount.setText("0");
            return;
        }

        for (DocumentSnapshot chatDoc : chatSnapshots.getDocuments()) {
            String chatId = chatDoc.getId();

            db.collection("chats").document(chatId)
                    .collection("messages")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            totalMessages[0] += task.getResult().size();
                        }
                        processedChats[0]++;

                        if (processedChats[0] == totalChats) {
                            textViewMessagesCount.setText(String.valueOf(totalMessages[0]));
                        }
                    });
        }
    }

    private void saveChanges() {
        Map<String, Object> updates = new HashMap<>();
        boolean hasUpdates = false;

        // Check name update
        String newName = editTextName.getText().toString().trim();
        if (isEditingName && !TextUtils.isEmpty(newName) &&
                (currentUser == null || !newName.equals(currentUser.getName()))) {
            updates.put("name", newName);
            hasUpdates = true;
        }

        // Check status update
        String newStatus = textViewStatus.getText().toString().trim();
        if (isEditingStatus && !TextUtils.isEmpty(newStatus) &&
                (currentUser == null || !newStatus.equals(currentUser.getStatus()))) {
            updates.put("status", newStatus);
            hasUpdates = true;
        }

        // Upload image if changed
        if (newImageUri != null) {
            uploadImageAndSave(updates);
        } else if (hasUpdates) {
            updateUserData(updates);
        } else {
            buttonSave.setVisibility(View.GONE);
            editTextName.setEnabled(false);
            Toast.makeText(this, "No changes to save", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageAndSave(Map<String, Object> updates) {
        showLoading(true);

        String fileName = "profile_images/" + currentUserId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(fileName);

        imageRef.putFile(newImageUri)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    // Update progress if needed
                })
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        updates.put("photoUrl", uri.toString());
                        updateUserData(updates);
                    });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUserData(Map<String, Object> updates) {
        db.collection("users").document(currentUserId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                    // Reset editing states
                    isEditingName = false;
                    isEditingStatus = false;
                    isImageChanged = false;
                    newImageUri = null;
                    editTextName.setEnabled(false);
                    buttonSave.setVisibility(View.GONE);

                    // Reload user data
                    loadUserData();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            newImageUri = data.getData();
            imageViewProfile.setImageURI(newImageUri);
            buttonSave.setVisibility(View.VISIBLE);
            isImageChanged = true;
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}