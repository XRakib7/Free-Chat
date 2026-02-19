package com.softcraft.freechat.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity implements
        ChatRepository.OnMessagesLoadedListener,
        ChatRepository.OnMessageSentListener,
        ChatRepository.OnChatCreatedListener {

    private static final int REQUEST_IMAGE_PICK = 101;

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

        // Initialize storage
        initStorage();

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
                        if (presenceListener != null) {
                            setupPresenceListener();
                        } else {
                            textViewOnlineStatus.setText("Offline");
                            textViewOnlineStatus.setTextColor(getColor(R.color.gray));
                        }
                    }
                });
            });
        }
    }

    private void initStorage() {
        // Storage already initialized
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

        // Send message - now passing otherUserName
        chatRepository.sendMessage(chatId, messageText, otherUserName, this);
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

    private void updateChatLastMessage(String lastMessage) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", lastMessage);
        updates.put("lastMessageTime", System.currentTimeMillis());
        updates.put("lastMessageSenderId", FirebaseAuth.getInstance().getCurrentUser().getUid());

        FirebaseFirestore.getInstance()
                .collection("chats")
                .document(chatId)
                .update(updates);
    }

    private String getLastSeenText(long lastSeen) {
        long now = System.currentTimeMillis();
        long diff = now - lastSeen;

        if (diff < 60000) {
            return "Last seen just now";
        } else if (diff < 3600000) {
            int minutes = (int) (diff / 60000);
            return "Last seen " + minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else if (diff < 86400000) {
            int hours = (int) (diff / 3600000);
            return "Last seen " + hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else {
            int days = (int) (diff / 86400000);
            return "Last seen " + days + " day" + (days > 1 ? "s" : "") + " ago";
        }
    }

    @Override
    public void onMessagesLoaded(List<Message> messages) {
        showLoading(false);

        this.messageList.clear();
        this.messageList.addAll(messages);

        messageAdapter.notifyDataSetChanged();

        if (!messageList.isEmpty()) {
            recyclerViewMessages.scrollToPosition(messageList.size() - 1);

            // Mark messages as read
            markMessagesAsRead(messages);
        }
    }

    @Override
    public void onError(String error) {
        showLoading(false);
        if (error != null && !error.isEmpty()) {
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMessageSent(boolean success, String error) {
        buttonSend.setEnabled(true);

        if (!success && error != null) {
            Toast.makeText(this, "Failed to send message: " + error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onChatCreated(String newChatId) {
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
        }
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