package com.softcraft.freechat.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.softcraft.freechat.R;
import com.softcraft.freechat.adapters.MessageAdapter;
import com.softcraft.freechat.managers.PresenceManager;
import com.softcraft.freechat.managers.TypingManager;
import com.softcraft.freechat.models.Message;
import com.softcraft.freechat.repositories.ChatRepository;
import com.softcraft.freechat.utils.ImageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity implements
        ChatRepository.OnMessagesLoadedListener,
        ChatRepository.OnMessageSentListener,
        ChatRepository.OnChatCreatedListener {

    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final int REQUEST_CAMERA_PERMISSION = 103;

    private Toolbar toolbar;
    private ImageView imageViewOtherUser;
    private TextView textViewOtherUserName;
    private TextView textViewOnlineStatus;
    private RecyclerView recyclerViewMessages;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ImageButton buttonAttach;
    private ProgressBar progressBar;

    private String chatId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserPhoto;
    private boolean isNewChat = false;

    private ChatRepository chatRepository;
    private PresenceManager presenceManager;
    private TypingManager typingManager;
    private MessageAdapter messageAdapter;
    private List<Message> messageList = new ArrayList<>();
    private ListenerRegistration presenceListener;

    private boolean isUserTyping = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get intent data
        otherUserId = getIntent().getStringExtra("otherUserId");
        otherUserName = getIntent().getStringExtra("otherUserName");
        otherUserPhoto = getIntent().getStringExtra("otherUserPhoto");
        chatId = getIntent().getStringExtra("chatId");

        // Ensure otherUserName is never null
        if (otherUserName == null || otherUserName.isEmpty()) {
            otherUserName = "User";
        }

        // Initialize repositories and managers
        chatRepository = new ChatRepository();
        presenceManager = PresenceManager.getInstance();
        typingManager = new TypingManager();

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup click listeners
        setupClickListeners();

        // Setup message input
        setupMessageInput();

        // Setup presence listener
        setupPresenceListener();

        // Initialize typing manager
        initTypingManager();

        // Setup message status tracking
        setupMessageStatusUpdates();

        // If chatId is null, create or get existing chat
        if (chatId == null || chatId.isEmpty()) {
            isNewChat = true;
            createOrGetChat();
        } else {
            loadMessages();
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        imageViewOtherUser = findViewById(R.id.imageViewOtherUser);
        textViewOtherUserName = findViewById(R.id.textViewOtherUserName);
        textViewOnlineStatus = findViewById(R.id.textViewOnlineStatus);
        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.buttonSend);
        buttonAttach = findViewById(R.id.buttonAttach);
        progressBar = findViewById(R.id.progressBar);

        // Set other user info
        textViewOtherUserName.setText(otherUserName != null ? otherUserName : "User");

        // Load avatar
        if (otherUserPhoto != null && !otherUserPhoto.isEmpty()) {
            Glide.with(this)
                    .load(otherUserPhoto)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(imageViewOtherUser);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(this, messageList, otherUserPhoto);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewMessages.setLayoutManager(layoutManager);
        recyclerViewMessages.setAdapter(messageAdapter);
    }

    private void setupClickListeners() {
        buttonSend.setOnClickListener(v -> sendMessage());
        buttonAttach.setOnClickListener(v -> showImagePickerDialog());
    }

    private void setupMessageInput() {
        editTextMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (chatId != null) {
                    boolean isNowTyping = s.toString().trim().length() > 0;
                    if (isNowTyping != isUserTyping) {
                        isUserTyping = isNowTyping;
                        typingManager.setTyping(chatId, isUserTyping);
                    }
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void setupPresenceListener() {
        if (otherUserId == null) return;

        // Remove old listener if exists
        if (presenceListener != null) {
            presenceListener.remove();
        }

        presenceListener = presenceManager.listenToUserPresence(
                otherUserId,
                (status, lastSeen) -> {
                    runOnUiThread(() -> {
                        // Only update if we're not showing typing indicator
                        String currentStatus = textViewOnlineStatus.getText().toString();
                        if (!currentStatus.equals("typing...")) {
                            updateOnlineStatus(status, lastSeen);
                        }
                    });
                }
        );
    }

    private void updateOnlineStatus(String status, long lastSeen) {
        if ("online".equals(status)) {
            textViewOnlineStatus.setText("Online");
            textViewOnlineStatus.setTextColor(getColor(R.color.primary));
            textViewOnlineStatus.setVisibility(View.VISIBLE);
        } else {
            String lastSeenText = presenceManager.getLastSeenText(lastSeen);
            textViewOnlineStatus.setText(lastSeenText);
            textViewOnlineStatus.setTextColor(getColor(R.color.gray));
            textViewOnlineStatus.setVisibility(View.VISIBLE);
        }
    }

    private void initTypingManager() {
        if (chatId != null) {
            typingManager.listenToTyping(chatId, isTyping -> {
                runOnUiThread(() -> {
                    if (isTyping) {
                        textViewOnlineStatus.setText("typing...");
                        textViewOnlineStatus.setTextColor(getColor(R.color.primary));
                    } else {
                        // Reset to online status
                        setupPresenceListener();
                    }
                });
            });
        }
    }

    private void createOrGetChat() {
        showLoading(true);
        chatRepository.getOrCreateChat(otherUserId, this);
    }

    private void loadMessages() {
        if (chatId == null) return;

        showLoading(true);
        chatRepository.getMessages(chatId, this);
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();

        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        if (chatId == null) {
            Toast.makeText(this, "Chat not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Stop typing indicator
        if (isUserTyping) {
            typingManager.setTyping(chatId, false);
            isUserTyping = false;
        }

        // Disable send button temporarily
        buttonSend.setEnabled(false);

        // Clear input
        editTextMessage.setText("");

        // Send message
        chatRepository.sendMessage(chatId, messageText, otherUserName, this);
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Send Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Take photo with camera - ALWAYS check permission before opening
                        checkCameraPermissionAndOpen();
                    } else if (which == 1) {
                        // Choose from gallery (no permission needed for gallery on most devices)
                        openGallery();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndOpen() {
        // Check if we have camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted - open camera
            openCamera();
        } else {
            // Permission not granted - request it
            // Should show a rationale? (Optional but good UX)
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // Show explanation to user
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Camera Permission Needed")
                        .setMessage("This app needs camera permission to take photos and share them in chats.")
                        .setPositiveButton("OK", (dialog, which) -> {
                            // Request permission
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                // Request permission directly
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted - open camera
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
                openCamera();
            } else {
                // Permission denied
                Toast.makeText(this, "Camera permission is needed to take photos",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                // Image picked from gallery
                Uri imageUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                    sendImageMessage(bitmap);
                } catch (Exception e) {
                    Log.e("ChatActivity", "Error loading image from gallery", e);
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                // Image captured from camera
                Bundle extras = data.getExtras();
                Bitmap bitmap = (Bitmap) extras.get("data");
                if (bitmap != null) {
                    sendImageMessage(bitmap);
                }
            }
        }
    }

    private void sendImageMessage(Bitmap bitmap) {
        if (chatId == null) {
            Toast.makeText(this, "Chat not ready yet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        buttonSend.setEnabled(false);
        buttonAttach.setEnabled(false);

        // Send image message
        chatRepository.sendImageMessage(chatId, bitmap, otherUserName, new ChatRepository.OnMessageSentListener() {
            @Override
            public void onMessageSent(boolean success, String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    buttonSend.setEnabled(true);
                    buttonAttach.setEnabled(true);

                    if (!success) {
                        Toast.makeText(ChatActivity.this,
                                "Failed to send image: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void setupMessageStatusUpdates() {
        setupReadReceiptsOnScroll();
        setupMessageReadReceipts();
    }

    private void setupMessageReadReceipts() {
        if (chatId == null) return;

        // Listen for new messages to mark them as delivered/read
        chatRepository.getMessages(chatId, new ChatRepository.OnMessagesLoadedListener() {
            @Override
            public void onMessagesLoaded(List<Message> messages) {
                // Mark messages as read when they appear in the chat
                markMessagesAsRead(messages);
            }

            @Override
            public void onError(String error) {
                Log.e("ChatActivity", "Error loading messages for read receipts: " + error);
            }
        });
    }

    private void markMessagesAsRead(List<Message> messages) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        for (Message message : messages) {
            // Mark messages from other users as read
            if (!message.getSenderId().equals(currentUserId)) {
                // Check if message is not already read
                if (message.getReadBy() == null || !message.getReadBy().containsKey(currentUserId)) {
                    chatRepository.markMessageAsRead(chatId, message.getMessageId(), currentUserId);
                }
            }
        }
    }

    private void setupReadReceiptsOnScroll() {
        recyclerViewMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // Mark visible messages as read
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null) {
                        int firstVisible = layoutManager.findFirstVisibleItemPosition();
                        int lastVisible = layoutManager.findLastVisibleItemPosition();

                        for (int i = firstVisible; i <= lastVisible; i++) {
                            if (i < messageList.size()) {
                                Message message = messageList.get(i);
                                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                if (!message.getSenderId().equals(currentUserId)) {
                                    if (message.getReadBy() == null || !message.getReadBy().containsKey(currentUserId)) {
                                        chatRepository.markMessageAsRead(chatId, message.getMessageId(), currentUserId);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onMessagesLoaded(List<Message> messages) {
        runOnUiThread(() -> {
            showLoading(false);

            this.messageList.clear();
            this.messageList.addAll(messages);

            messageAdapter.notifyDataSetChanged();

            if (!messageList.isEmpty()) {
                recyclerViewMessages.scrollToPosition(messageList.size() - 1);

                // Mark messages as read
                markMessagesAsRead(messages);
            }
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            showLoading(false);
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMessageSent(boolean success, String error) {
        runOnUiThread(() -> {
            buttonSend.setEnabled(true);

            if (!success && error != null) {
                Toast.makeText(this, "Failed to send message: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onChatCreated(String newChatId) {
        runOnUiThread(() -> {
            this.chatId = newChatId;
            isNewChat = false;

            // Initialize typing manager with new chatId
            initTypingManager();

            // Load messages (will be empty for new chat)
            loadMessages();

            showLoading(false);

            // Also setup presence listener if not already done
            if (presenceListener == null) {
                setupPresenceListener();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh presence listener
        if (otherUserId != null) {
            setupPresenceListener();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenceListener != null) {
            presenceListener.remove();
        }
        if (typingManager != null) {
            typingManager.cleanup();
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            recyclerViewMessages.setVisibility(View.GONE);
        } else {
            recyclerViewMessages.setVisibility(View.VISIBLE);
        }
    }
}