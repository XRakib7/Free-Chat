package com.softcraft.freechat.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.softcraft.freechat.R;
import com.softcraft.freechat.adapters.SearchAdapter;
import com.softcraft.freechat.models.Message;
import com.softcraft.freechat.models.User;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private EditText editTextSearch;
    private RecyclerView recyclerViewResults;
    private ProgressBar progressBar;
    private TextView textViewNoResults;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SearchAdapter searchAdapter;
    private List<Object> searchResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupToolbar();
        setupSearch();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        editTextSearch = findViewById(R.id.editTextSearch);
        recyclerViewResults = findViewById(R.id.recyclerViewResults);
        progressBar = findViewById(R.id.progressBar);
        textViewNoResults = findViewById(R.id.textViewNoResults);

        recyclerViewResults.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new SearchAdapter(this, searchResults);
        recyclerViewResults.setAdapter(searchAdapter);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Search");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupSearch() {
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.length() >= 2) {
                    performSearch(query);
                } else {
                    clearResults();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String query) {
        showLoading(true);
        searchResults.clear();

        // Search in messages
        searchMessages(query);

        // Search in users
        searchUsers(query);
    }

    private void searchMessages(String query) {
        String currentUserId = auth.getCurrentUser().getUid();

        // Get all chats of current user
        db.collection("chats")
                .whereArrayContains("participantIds", currentUserId)
                .get()
                .addOnSuccessListener(chatSnapshots -> {
                    for (var chatDoc : chatSnapshots.getDocuments()) {
                        String chatId = chatDoc.getId();

                        // Search messages in this chat
                        db.collection("chats").document(chatId)
                                .collection("messages")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(50)
                                .get()
                                .addOnSuccessListener(messageSnapshots -> {
                                    for (var messageDoc : messageSnapshots.getDocuments()) {
                                        Message message = messageDoc.toObject(Message.class);
                                        String text = message.getText();

                                        if (text != null && text.toLowerCase().contains(query.toLowerCase())) {
                                            message.setMessageId(messageDoc.getId());
                                            searchResults.add(message);
                                        }
                                    }
                                    updateResults();
                                });
                    }
                });
    }

    private void searchUsers(String query) {
        db.collection("users")
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (var doc : queryDocumentSnapshots.getDocuments()) {
                        User user = doc.toObject(User.class);
                        user.setUserId(doc.getId());

                        // Don't include current user
                        if (!user.getUserId().equals(auth.getCurrentUser().getUid())) {
                            searchResults.add(user);
                        }
                    }
                    updateResults();
                });
    }

    private void updateResults() {
        showLoading(false);

        if (searchResults.isEmpty()) {
            textViewNoResults.setVisibility(View.VISIBLE);
            recyclerViewResults.setVisibility(View.GONE);
        } else {
            textViewNoResults.setVisibility(View.GONE);
            recyclerViewResults.setVisibility(View.VISIBLE);
            searchAdapter.notifyDataSetChanged();
        }
    }

    private void clearResults() {
        searchResults.clear();
        searchAdapter.notifyDataSetChanged();
        recyclerViewResults.setVisibility(View.GONE);
        textViewNoResults.setVisibility(View.GONE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}