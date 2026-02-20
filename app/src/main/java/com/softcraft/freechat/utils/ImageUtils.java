package com.softcraft.freechat.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;

public class ImageUtils {
    private static final String TAG = "ImageUtils";
    private static final int MAX_IMAGE_SIZE = 500 * 1024; // 500KB max
    private static final int MAX_DIMENSION = 1024; // Max width/height

    /**
     * Compress and resize bitmap to fit within size limits
     */
    public static Bitmap compressImage(Bitmap original, int maxDimension, int maxSizeBytes) {
        try {
            int width = original.getWidth();
            int height = original.getHeight();

            // Calculate scale factor
            float scaleFactor = 1.0f;
            if (width > maxDimension || height > maxDimension) {
                scaleFactor = Math.min(
                        (float) maxDimension / width,
                        (float) maxDimension / height
                );
            }

            // Resize bitmap
            int newWidth = Math.round(width * scaleFactor);
            int newHeight = Math.round(height * scaleFactor);
            Bitmap resized = Bitmap.createScaledBitmap(original, newWidth, newHeight, true);

            // Compress to JPEG with quality adjustment
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int quality = 90;
            resized.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);

            // Reduce quality until size is under limit
            while (outputStream.size() > maxSizeBytes && quality > 10) {
                outputStream.reset();
                quality -= 10;
                resized.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            }

            Log.d(TAG, "Compressed image: " + outputStream.size() + " bytes, quality: " + quality);

            // Recycle original if different from resized
            if (resized != original) {
                original.recycle();
            }

            return resized;
        } catch (Exception e) {
            Log.e(TAG, "Error compressing image", e);
            return original;
        }
    }

    /**
     * Convert bitmap to Base64 string
     */
    public static String bitmapToBase64(Bitmap bitmap) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
            byte[] imageBytes = outputStream.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Error converting bitmap to base64", e);
            return null;
        }
    }

    /**
     * Convert Base64 string to bitmap
     */
    public static Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            Log.e(TAG, "Error converting base64 to bitmap", e);
            return null;
        }
    }

    /**
     * Validate image size before sending
     */
    public static boolean isValidImageSize(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
        return outputStream.size() <= MAX_IMAGE_SIZE;
    }

    /**
     * Get estimated size of Base64 string
     */
    public static int getEstimatedSize(String base64String) {
        return (base64String.length() * 3) / 4; // Approximate original size
    }
}