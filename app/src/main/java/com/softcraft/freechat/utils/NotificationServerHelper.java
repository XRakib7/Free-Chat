package com.softcraft.freechat.utils;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationServerHelper {
    private static final String TAG = "NotificationServer";
    // Replace with your actual Render URL
    private static final String SERVER_URL = "https://fcm-server-hntp.onrender.com/send-notification";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static OkHttpClient client = new OkHttpClient();

    public static void sendNotification(String recipientToken, String title, String body,
                                        JSONObject data, final NotificationCallback callback) {
        if (recipientToken == null || recipientToken.isEmpty()) {
            Log.e(TAG, "Recipient token is null or empty");
            if (callback != null) callback.onFailure("Token is empty");
            return;
        }

        try {
            JSONObject payload = new JSONObject();
            payload.put("token", recipientToken);
            payload.put("title", title);
            payload.put("body", body);
            if (data != null) {
                payload.put("data", data);
            }

            RequestBody requestBody = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                    .url(SERVER_URL)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network request failed", e);
                    if (callback != null) callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Notification sent successfully");
                        if (callback != null) callback.onSuccess();
                    } else {
                        Log.e(TAG, "Server error: " + response.code() + " - " + responseBody);
                        if (callback != null) callback.onFailure("Server error: " + response.code());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "JSON error", e);
        }
    }

    public interface NotificationCallback {
        void onSuccess();
        void onFailure(String error);
    }
}