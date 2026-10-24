package org.open.quietgate;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/** A visible, best-effort readiness indicator; it cannot guarantee delivery or rejection of Meet calls. */
public final class GuardNotificationService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    static final String CHANNEL_ID = "quietgate_protection";
    static final int NOTIFICATION_ID = 4810;
    private static final String KEY_LISTENER_CONNECTED = "listener_connected";
    private static volatile boolean running;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    static boolean isRunning() { return running; }
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            LeaseManager.refreshCurrent(GuardNotificationService.this);
            refreshNotification();
            if (LeaseManager.isEnabled(GuardNotificationService.this)) handler.postDelayed(this, 60_000L);
        }
    };

    static boolean start(Context context) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return false;
        try {
            Intent intent = new Intent(context, GuardNotificationService.class);
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
            else context.startService(intent);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(LeaseManager.PREFS, MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!LeaseManager.isEnabled(this)) { stopSelf(startId); return START_NOT_STICKY; }
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        running = true;
        publishState();
        handler.removeCallbacks(ticker);
        handler.postDelayed(ticker, 60_000L);
        return START_STICKY;
    }

    @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (LeaseManager.KEY_ENABLED.equals(key) && !LeaseManager.isEnabled(this)) {
            running = false;
            publishState();
            handler.removeCallbacks(ticker);
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        } else if (LeaseManager.KEY_ENABLED.equals(key) || "offline_since".equals(key) || KEY_LISTENER_CONNECTED.equals(key)) {
            handler.post(this::refreshNotification);
        }
    }

    private void refreshNotification() {
        if (!LeaseManager.isEnabled(this)) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(NOTIFICATION_ID, buildNotification());
    }

    private Notification buildNotification() {
        boolean connected = MeetRejectListener.isConnected();
        boolean shizuku = ShizukuStatus.isAuthorized();
        boolean wifi = LeaseManager.isWifiConnected(this);
        boolean contacts = checkSelfPermission(android.Manifest.permission.READ_CONTACTS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        long remaining = LeaseManager.remaining(this);
        String detail;
        if (!contacts) {
            detail = "Contacts access missing · caller matching is unavailable. Open QuietGate to restore permission.";
        } else if (!connected) {
            detail = "Listener not connected · Meet alert monitoring is not confirmed. Open QuietGate to check access.";
        } else if (remaining >= 0) {
            detail = "Grace period · Wi-Fi or Shizuku unavailable · turns off in " + duration(remaining);
        } else if (shizuku && wifi) {
            detail = "Checks ready · Meet alerts monitored; rejection is best-effort, not guaranteed.";
        } else {
            detail = "Status refreshing · checking Wi-Fi and Shizuku readiness.";
        }
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(this, 4810, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_status_quietgate)
                .setContentTitle("QuietGate protection")
                .setContentText(detail)
                .setStyle(new Notification.BigTextStyle().bigText(detail))
                .setContentIntent(content)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(remaining >= 0)
                .setUsesChronometer(remaining >= 0)
                .setChronometerCountDown(remaining >= 0);
        if (remaining >= 0) builder.setWhen(System.currentTimeMillis() + remaining).setSubText("Grace remaining");
        return builder.build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "QuietGate protection", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Ongoing status for user-enabled QuietGate call screening");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private String duration(long millis) {
        long minutes = Math.max(0, millis / 60_000L);
        return (minutes / 60) + "h " + (minutes % 60) + "m";
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(ticker);
        if (prefs != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(this);
        }
        running = false;
        publishState();
        super.onDestroy();
    }

    private void publishState() {
        if (prefs != null) prefs.edit().putLong("status_state_changed", android.os.SystemClock.elapsedRealtimeNanos()).apply();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

}
