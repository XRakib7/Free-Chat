package com.softcraft.freechat.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.softcraft.freechat.R;
import com.softcraft.freechat.managers.AuthManager;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private Toolbar toolbar;

    // Account settings
    private LinearLayout layoutProfile;
    private LinearLayout layoutPrivacy;
    private LinearLayout layoutSecurity;

    // Notification settings
    private SwitchCompat switchMessageNotifications;
    private SwitchCompat switchVibration;
    private LinearLayout layoutNotificationSound;
    private TextView textViewSound;

    // Chat settings
    private SwitchCompat switchEnterSends;
    private LinearLayout layoutFontSize;
    private TextView textViewFontSize;

    // Data settings
    private LinearLayout layoutStorage;
    private TextView textViewStorage;
    private LinearLayout layoutAutoDownload;
    private TextView textViewAutoDownload;
    private LinearLayout layoutClearCache;

    // About
    private LinearLayout layoutTerms;
    private LinearLayout layoutPrivacyPolicy;
    private TextView textViewVersion;

    // Sign out
    private Button buttonSignOut;

    private FirebaseAuth auth;
    private AuthManager authManager;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        authManager = new AuthManager();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("FreeChatPrefs", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Load saved settings
        loadSettings();

        // Setup click listeners
        setupClickListeners();

        // Calculate storage
        calculateStorageUsage();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);

        // Account settings
        layoutProfile = findViewById(R.id.layoutProfile);
        layoutPrivacy = findViewById(R.id.layoutPrivacy);
        layoutSecurity = findViewById(R.id.layoutSecurity);

        // Notification views
        switchMessageNotifications = findViewById(R.id.switchMessageNotifications);
        switchVibration = findViewById(R.id.switchVibration);
        layoutNotificationSound = findViewById(R.id.layoutNotificationSound);
        textViewSound = findViewById(R.id.textViewSound);

        // Chat views
        switchEnterSends = findViewById(R.id.switchEnterSends);
        layoutFontSize = findViewById(R.id.layoutFontSize);
        textViewFontSize = findViewById(R.id.textViewFontSize);

        // Data views
        layoutStorage = findViewById(R.id.layoutStorage);
        textViewStorage = findViewById(R.id.textViewStorage);
        layoutAutoDownload = findViewById(R.id.layoutAutoDownload);
        textViewAutoDownload = findViewById(R.id.textViewAutoDownload);
        layoutClearCache = findViewById(R.id.layoutClearCache);

        // About views
        layoutTerms = findViewById(R.id.layoutTerms);
        layoutPrivacyPolicy = findViewById(R.id.layoutPrivacyPolicy);
        textViewVersion = findViewById(R.id.textViewVersion);

        // Sign out
        buttonSignOut = findViewById(R.id.buttonSignOut);

        // Set version
        textViewVersion.setText("Version " + BuildConfig.VERSION_NAME);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadSettings() {
        // Load notification settings
        boolean notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true);
        boolean vibrationEnabled = sharedPreferences.getBoolean("vibration_enabled", true);
        String soundUri = sharedPreferences.getString("notification_sound", "default");

        switchMessageNotifications.setChecked(notificationsEnabled);
        switchVibration.setChecked(vibrationEnabled);

        if (soundUri.equals("default")) {
            textViewSound.setText("Default");
        } else {
            try {
                Ringtone ringtone = RingtoneManager.getRingtone(this, Uri.parse(soundUri));
                textViewSound.setText(ringtone.getTitle(this));
            } catch (Exception e) {
                textViewSound.setText("Default");
            }
        }

        // Load chat settings
        boolean enterSends = sharedPreferences.getBoolean("enter_sends", false);
        String fontSize = sharedPreferences.getString("font_size", "Medium");

        switchEnterSends.setChecked(enterSends);
        textViewFontSize.setText(fontSize);

        // Load data settings
        String autoDownload = sharedPreferences.getString("auto_download", "Wi-Fi only");
        textViewAutoDownload.setText(autoDownload);
    }

    private void setupClickListeners() {
        // Account settings
        layoutProfile.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, ProfileActivity.class));
        });

        layoutPrivacy.setOnClickListener(v -> {
            Toast.makeText(this, "Privacy settings coming soon", Toast.LENGTH_SHORT).show();
        });

        layoutSecurity.setOnClickListener(v -> {
            showSecurityDialog();
        });

        // Notification sound
        layoutNotificationSound.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    getCurrentNotificationSound());
            startActivityForResult(intent, 1001);
        });

        // Font size
        layoutFontSize.setOnClickListener(v -> {
            showFontSizeDialog();
        });

        // Storage usage
        layoutStorage.setOnClickListener(v -> {
            showStorageDialog();
        });

        // Auto-download
        layoutAutoDownload.setOnClickListener(v -> {
            showAutoDownloadDialog();
        });

        // Clear cache
        layoutClearCache.setOnClickListener(v -> {
            showClearCacheDialog();
        });

        // Terms of service
        layoutTerms.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://yourwebsite.com/terms"));
            startActivity(intent);
        });

        // Privacy policy
        layoutPrivacyPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://yourwebsite.com/privacy"));
            startActivity(intent);
        });

        // Sign out
        buttonSignOut.setOnClickListener(v -> {
            showSignOutDialog();
        });

        // Save preferences when switches change
        switchMessageNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("notifications_enabled", isChecked);
            editor.apply();
        });

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("vibration_enabled", isChecked);
            editor.apply();
        });

        switchEnterSends.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("enter_sends", isChecked);
            editor.apply();
        });
    }

    private Uri getCurrentNotificationSound() {
        String soundUri = sharedPreferences.getString("notification_sound", "default");
        if (soundUri.equals("default")) {
            return Settings.System.DEFAULT_NOTIFICATION_URI;
        } else {
            return Uri.parse(soundUri);
        }
    }

    private void showSecurityDialog() {
        String[] options = {"Change Password", "Two-Factor Authentication", "Login Alerts"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Security")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            // Navigate to change password in Profile
                            startActivity(new Intent(SettingsActivity.this, ProfileActivity.class));
                            break;
                        case 1:
                            Toast.makeText(this, "2FA coming soon", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            Toast.makeText(this, "Login alerts coming soon", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void showFontSizeDialog() {
        String[] sizes = {"Small", "Medium", "Large"};
        int currentIndex = 1; // Default Medium

        String currentSize = textViewFontSize.getText().toString();
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i].equals(currentSize)) {
                currentIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Font Size")
                .setSingleChoiceItems(sizes, currentIndex, (dialog, which) -> {
                    String selected = sizes[which];
                    textViewFontSize.setText(selected);
                    editor.putString("font_size", selected);
                    editor.apply();
                    dialog.dismiss();
                })
                .show();
    }

    private void showAutoDownloadDialog() {
        String[] options = {"Never", "Wi-Fi only", "Always"};
        int currentIndex = 1; // Default Wi-Fi only

        String currentOption = textViewAutoDownload.getText().toString();
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(currentOption)) {
                currentIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Auto-download Media")
                .setSingleChoiceItems(options, currentIndex, (dialog, which) -> {
                    String selected = options[which];
                    textViewAutoDownload.setText(selected);
                    editor.putString("auto_download", selected);
                    editor.apply();
                    dialog.dismiss();
                })
                .show();
    }

    private void showStorageDialog() {
        long totalSize = getStorageUsage();
        String formattedSize = formatSize(totalSize);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Storage Usage")
                .setMessage("Total cache: " + formattedSize + "\n\n" +
                        "Clear cache to free up space?")
                .setPositiveButton("Clear Cache", (dialog, which) -> {
                    clearCache();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearCacheDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear Cache")
                .setMessage("Are you sure you want to clear the cache? " +
                        "This will remove temporary files but won't delete your messages.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    clearCache();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSignOutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (dialog, which) -> {
                    signOut();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void signOut() {
        authManager.signOut();
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void calculateStorageUsage() {
        long totalSize = getStorageUsage();
        textViewStorage.setText(formatSize(totalSize));
    }

    private long getStorageUsage() {
        long totalSize = 0;

        // Add cache size
        try {
            File cacheDir = getCacheDir();
            totalSize += getFolderSize(cacheDir);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                File[] externalCacheDirs = getExternalCacheDirs();
                for (File dir : externalCacheDirs) {
                    if (dir != null) {
                        totalSize += getFolderSize(dir);
                    }
                }
            } else {
                File externalCacheDir = getExternalCacheDir();
                if (externalCacheDir != null) {
                    totalSize += getFolderSize(externalCacheDir);
                }
            }

            // Add image cache if exists
            File imageCacheDir = new File(getCacheDir(), "image_cache");
            if (imageCacheDir.exists()) {
                totalSize += getFolderSize(imageCacheDir);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return totalSize;
    }

    private long getFolderSize(File folder) {
        long length = 0;
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    length += file.length();
                } else {
                    length += getFolderSize(file);
                }
            }
        }
        return length;
    }

    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    private void clearCache() {
        try {
            // Clear app cache
            File cacheDir = getCacheDir();
            deleteFolderContents(cacheDir);

            // Clear external cache
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                File[] externalCacheDirs = getExternalCacheDirs();
                for (File dir : externalCacheDirs) {
                    if (dir != null) {
                        deleteFolderContents(dir);
                    }
                }
            } else {
                File externalCacheDir = getExternalCacheDir();
                if (externalCacheDir != null) {
                    deleteFolderContents(externalCacheDir);
                }
            }

            // Clear image cache
            File imageCacheDir = new File(getCacheDir(), "image_cache");
            if (imageCacheDir.exists()) {
                deleteFolderContents(imageCacheDir);
            }

            Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show();

            // Recalculate storage
            calculateStorageUsage();
        } catch (Exception e) {
            Toast.makeText(this, "Error clearing cache: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteFolderContents(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolderContents(file);
                }
                file.delete();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            Uri soundUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

            if (soundUri != null) {
                try {
                    Ringtone ringtone = RingtoneManager.getRingtone(this, soundUri);
                    String soundName = ringtone.getTitle(this);

                    textViewSound.setText(soundName);
                    editor.putString("notification_sound", soundUri.toString());
                    editor.apply();
                } catch (Exception e) {
                    textViewSound.setText("Default");
                    editor.putString("notification_sound", "default");
                    editor.apply();
                }
            } else {
                textViewSound.setText("Default");
                editor.putString("notification_sound", "default");
                editor.apply();
            }
        }
    }
}