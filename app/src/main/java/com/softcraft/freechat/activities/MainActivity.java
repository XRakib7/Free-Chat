package com.softcraft.freechat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.softcraft.freechat.R;
import com.softcraft.freechat.adapters.ChatAdapter;
import com.softcraft.freechat.managers.AuthManager;
import com.softcraft.freechat.managers.PresenceManager;
import com.softcraft.freechat.models.Chat;
import com.softcraft.freechat.models.User;
import com.softcraft.freechat.repositories.ChatRepository;
import com.softcraft.freechat.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements
        ChatRepository.OnChatsLoadedListener,
        SwipeRefreshLayout.OnRefreshListener {

    // Managers and Repositories
    private AuthManager authManager;
    private ChatRepository chatRepository;
    private PresenceManager presenceManager;

    // UI Components
    private Toolbar toolbar;
    private RecyclerView recyclerViewChats;
    private ChatAdapter chatAdapter;
    private ProgressBar progressBar;
    private TextView textViewNoChats;
    private FloatingActionButton fabNewChat;
    private SwipeRefreshLayout swipeRefreshLayout;

    // Data
    private List<Chat> chatList = new ArrayList<>();
    private Map<String, User> userMap;
    private ListenerRegistration presenceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize managers
        initManagers();

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup swipe refresh
        setupSwipeRefresh();

        // Setup click listeners
        setupClickListeners();

        // Check authentication
        checkAuth();

        // Load chats
        loadChats();
    }

    private void initManagers() {
        authManager = new AuthManager();
        chatRepository = new ChatRepository();
        presenceManager = PresenceManager.getInstance();

        // Set user online
        presenceManager.setUserOnline();

        // Update FCM token
        NotificationHelper.updateFCMToken();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerViewChats = findViewById(R.id.recyclerViewChats);
        progressBar = findViewById(R.id.progressBar);
        textViewNoChats = findViewById(R.id.textViewNoChats);
        fabNewChat = findViewById(R.id.fabNewChat);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("FreeChat");
        }
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(this, chatList, userMap);
        recyclerViewChats.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChats.setAdapter(chatAdapter);

        // Add scroll listener to load more if needed
        recyclerViewChats.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();
                    boolean isAtTop = firstVisibleItem == 0;

                    // Enable swipe refresh only at top
                    swipeRefreshLayout.setEnabled(isAtTop);
                }
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark
        );
    }

    private void setupClickListeners() {
        fabNewChat.setOnClickListener(v -> {
            // Navigate to users list to start new chat
            startActivity(new Intent(MainActivity.this, UsersActivity.class));
        });
    }

    private void checkAuth() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            // User not logged in, go to login
            navigateToLogin();
        }
    }

    private void loadChats() {
        showLoading(true);
        chatRepository.getChats(this);
    }

    @Override
    public void onChatsLoaded(List<Chat> chats, Map<String, User> users) {
        showLoading(false);
        swipeRefreshLayout.setRefreshing(false);

        this.chatList = chats;
        this.userMap = users;

        if (chats.isEmpty()) {
            showEmptyState(true);
        } else {
            showEmptyState(false);

            // Update adapter
            chatAdapter = new ChatAdapter(this, chatList, userMap);
            recyclerViewChats.setAdapter(chatAdapter);

            // Update last message times if needed
            updateChatTimestamps();
        }
    }

    @Override
    public void onError(String error) {
        showLoading(false);
        swipeRefreshLayout.setRefreshing(false);

        Snackbar.make(
                findViewById(android.R.id.content),
                "Error loading chats: " + error,
                Snackbar.LENGTH_LONG
        ).setAction("Retry", v -> loadChats()).show();
    }

    private void updateChatTimestamps() {
        // Update relative times every minute
        final android.os.Handler handler = new android.os.Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (chatAdapter != null) {
                    chatAdapter.notifyDataSetChanged();
                }
                handler.postDelayed(this, 60000); // Update every minute
            }
        };
        handler.post(runnable);
    }

    private void showEmptyState(boolean show) {
        if (show) {
            textViewNoChats.setVisibility(View.VISIBLE);
            recyclerViewChats.setVisibility(View.GONE);

            // Set empty state message
            String message = "No conversations yet\n\n" +
                    "Tap the + button to start chatting with someone!";
            textViewNoChats.setText(message);
        } else {
            textViewNoChats.setVisibility(View.GONE);
            recyclerViewChats.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            recyclerViewChats.setVisibility(View.GONE);
            textViewNoChats.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRefresh() {
        loadChats();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_search) {
            // Open search activity
            startActivity(new Intent(this, SearchActivity.class));
            return true;
        } else if (id == R.id.action_profile) {
            // Open profile
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            // Open settings
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            // Show logout confirmation
            showLogoutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("No", null)
                .show();
    }

    private void logout() {
        // Set user offline
        presenceManager.setUserOffline();

        // Sign out
        authManager.signOut();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Set user online when app starts
        presenceManager.setUserOnline();
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Set user online when app resumes
        presenceManager.setUserOnline();
        // Refresh chats
        loadChats();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Set user offline when app goes to background
        presenceManager.setUserOffline();
    }
    @Override
    protected void onStop() {
        super.onStop();
        // Don't set offline immediately - the disconnect handler will handle it
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenceListener != null) {
            presenceListener.remove();
        }
    }
}