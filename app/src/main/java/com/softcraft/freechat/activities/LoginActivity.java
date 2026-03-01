package com.softcraft.freechat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.softcraft.freechat.R;
import com.softcraft.freechat.managers.AuthManager;
import com.softcraft.freechat.models.AuthState;
import com.softcraft.freechat.utils.NotificationHelper;

public class LoginActivity extends AppCompatActivity implements AuthManager.AuthListener {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private TextView textViewSignUp, textViewForgotPassword;
    private ProgressBar progressBar;

    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize AuthManager
        authManager = new AuthManager();
        authManager.setAuthListener(this);

        // Check if user is already logged in
        if (authManager.getCurrentUser() != null) {
            // User is already logged in, go to MainActivity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

        // Initialize views
        initViews();

        // Set click listeners
        setupClickListeners();
    }

    private void initViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textViewSignUp = findViewById(R.id.textViewSignUp);
        textViewForgotPassword = findViewById(R.id.textViewForgotPassword);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        textViewSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go to Sign Up activity
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }
        });

        textViewForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetPassword();
            }
        });
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        buttonLogin.setEnabled(false);

        // Attempt login
        authManager.signIn(email, password);
    }

    private void resetPassword() {
        String email = editTextEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        authManager.resetPassword(email);
    }

    // AuthManager.AuthListener methods
    @Override
    public void onAuthSuccess(AuthState authState) {
        progressBar.setVisibility(View.GONE);
        buttonLogin.setEnabled(true);

        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
        NotificationHelper.updateFCMToken();
        // Go to main activity
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
        NotificationHelper.updateFCMToken();
    }

    @Override
    public void onAuthFailure(String error) {
        progressBar.setVisibility(View.GONE);
        buttonLogin.setEnabled(true);

        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
    }
}