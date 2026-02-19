package com.softcraft.freechat.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.softcraft.freechat.R;
import com.softcraft.freechat.adapters.UsersAdapter;
import com.softcraft.freechat.models.User;
import com.softcraft.freechat.repositories.ChatRepository;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText editTextSearch;
    private RecyclerView recyclerViewUsers;
    private ProgressBar progressBar;
    private TextView textViewNoUsers;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ChatRepository chatRepository;

    private UsersAdapter usersAdapter;
    private List<User> userList = new ArrayList<>();
    private List<User> filteredList = new ArrayList<>();
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        chatRepository = new ChatRepository();

        currentUserId = auth.getCurrentUser().getUid();

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup search
        setupSearch();

        // Load users
        loadUsers();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        editTextSearch = findViewById(R.id.editTextSearch);
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        progressBar = findViewById(R.id.progressBar);
        textViewNoUsers = findViewById(R.id.textViewNoUsers);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        usersAdapter = new UsersAdapter(this, filteredList);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewUsers.setAdapter(usersAdapter);
    }

    private void setupSearch() {
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUsers() {
        showLoading(true);

        db.collection("users")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        User user = document.toObject(User.class);
                        user.setUserId(document.getId());

                        // Don't include current user
                        if (!user.getUserId().equals(currentUserId)) {
                            userList.add(user);
                        }
                    }

                    filteredList.clear();
                    filteredList.addAll(userList);
                    usersAdapter.updateList(filteredList);

                    showLoading(false);
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void filterUsers(String query) {
        filteredList.clear();

        if (TextUtils.isEmpty(query)) {
            filteredList.addAll(userList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (User user : userList) {
                if (user.getName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(user);
                }
            }
        }

        usersAdapter.updateList(filteredList);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            textViewNoUsers.setVisibility(View.VISIBLE);
            recyclerViewUsers.setVisibility(View.GONE);
        } else {
            textViewNoUsers.setVisibility(View.GONE);
            recyclerViewUsers.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            recyclerViewUsers.setVisibility(View.GONE);
            textViewNoUsers.setVisibility(View.GONE);
        }
    }
}