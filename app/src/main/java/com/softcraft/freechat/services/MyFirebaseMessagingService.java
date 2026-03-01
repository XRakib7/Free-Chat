package com.softcraft.freechat.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.softcraft.freechat.R;
import com.softcraft.freechat.activities.ChatActivity;
import com.softcraft.freechat.activities.MainActivity;
import com.softcraft.freechat.utils.NotificationHelper;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "freechat_messages";
    private static final String CHANNEL_NAME = "Chat Messages";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Data payload: " + remoteMessage.getData());
            sendNotification(remoteMessage.getData());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "New FCM token: " + token);
        // Update token in Firestore
        NotificationHelper.updateFCMToken();
    }

    private void sendNotification(Map<String, String> data) {
        String title = data.get("title");
        String body = data.get("body");
        String chatId = data.get("chatId");
        String senderId = data.get("senderId");
        String senderName = data.get("senderName");

        if (title == null) title = "New Message";
        if (body == null) body = "You have a new message";

        Intent intent;
        if (chatId != null && !chatId.isEmpty()) {
            intent = new Intent(this, ChatActivity.class);
            intent.putExtra("chatId", chatId);
            intent.putExtra("otherUserId", senderId);
            intent.putExtra("otherUserName", senderName != null ? senderName : "User");
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else {
            intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (body != null && body.length() > 40) {
            notificationBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(body));
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("New message notifications");
            channel.enableLights(true);
            channel.setLightColor(getResources().getColor(R.color.primary));
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            notificationManager.createNotificationChannel(channel);
        }

        int notificationId = chatId != null ? chatId.hashCode() : 0;
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}