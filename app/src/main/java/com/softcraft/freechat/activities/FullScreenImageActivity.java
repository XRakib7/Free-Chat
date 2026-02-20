package com.softcraft.freechat.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.softcraft.freechat.R;
import com.softcraft.freechat.utils.ImageUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FullScreenImageActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView imageViewFull;
    private TextView textViewInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        // Get data from intent
        String imageBase64 = getIntent().getStringExtra("image_base64");
        String senderName = getIntent().getStringExtra("sender_name");
        long timestamp = getIntent().getLongExtra("timestamp", 0);

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        imageViewFull = findViewById(R.id.imageViewFull);
        textViewInfo = findViewById(R.id.textViewInfo);

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Image");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Load and display image
        if (imageBase64 != null) {
            Bitmap bitmap = ImageUtils.base64ToBitmap(imageBase64);
            if (bitmap != null) {
                imageViewFull.setImageBitmap(bitmap);
            }
        }

        // Set info text
        String timeString = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                .format(new Date(timestamp));
        textViewInfo.setText("From: " + senderName + " • " + timeString);
    }
}