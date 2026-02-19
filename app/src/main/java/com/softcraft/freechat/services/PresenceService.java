package com.softcraft.freechat.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.softcraft.freechat.managers.PresenceManager;

public class PresenceService extends Service {
    private static final String TAG = "PresenceService";
    private PresenceManager presenceManager;

    @Override
    public void onCreate() {
        super.onCreate();
        presenceManager = PresenceManager.getInstance();
        Log.d(TAG, "PresenceService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Keep the service running
        presenceManager.setUserOnline();
        Log.d(TAG, "PresenceService started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Set offline when service is destroyed
        presenceManager.setUserOffline();
        Log.d(TAG, "PresenceService destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}