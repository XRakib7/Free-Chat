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

public class SignUpActivity extends AppCompatActivity implements AuthManager.AuthListener {

    private EditText editTextName, editTextEmail, editTextPassword, editTextConfirmPassword;
    private Button buttonSignUp;
    private TextView textViewLogin;
    private ProgressBar progressBar;

    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Initialize AuthManager
        authManager = new AuthManager();
        authManager.setAuthListener(this);

        // Initialize views
        initViews();

        // Set click listeners
        setupClickListeners();
    }

    private void initViews() {
        editTextName = findViewById(R.id.editTextName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword);
        buttonSignUp = findViewById(R.id.buttonSignUp);
        textViewLogin = findViewById(R.id.textViewLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        buttonSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUpUser();
            }
        });

        textViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go back to login
                finish();
            }
        });
    }

    private void signUpUser() {
        String name = editTextName.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(name)) {
            editTextName.setError("Name is required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            editTextPassword.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirmPassword)) {
            editTextConfirmPassword.setError("Passwords do not match");
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        buttonSignUp.setEnabled(false);

        // Attempt sign up
        authManager.signUp(email, password, name);
    }

    // AuthManager.AuthListener methods
    @Override
    public void onAuthSuccess(AuthState authState) {
        progressBar.setVisibility(View.GONE);
        buttonSignUp.setEnabled(true);

        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
        NotificationHelper.updateFCMToken();
        // Go to main activity
        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
        finish();
    }

    @Override
    public void onAuthFailure(String error) {
        progressBar.setVisibility(View.GONE);
        buttonSignUp.setEnabled(true);

        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
    }
}