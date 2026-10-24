package org.open.quietgate;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.content.SharedPreferences;

final class LeaseManager {
    static final String PREFS = "quiet_gate";
    static final String KEY_ENABLED = "strict_enabled";
    private static final String KEY_OFFLINE_SINCE = "offline_since";
    static final long GRACE_MS = 24L * 60L * 60L * 1000L;
    private static final int ALARM_REQUEST = 4819;
    private LeaseManager() {}

    static synchronized void enable(Context context, boolean shizuku, boolean wifi) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putBoolean(KEY_ENABLED, true).putLong(KEY_OFFLINE_SINCE, 0L).apply();
        update(context, shizuku, wifi);
    }

    static synchronized void disable(Context context, String reason) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putBoolean(KEY_ENABLED, false).putLong(KEY_OFFLINE_SINCE, 0L).apply();
        cancelAlarm(context);
        if (reason != null) GuardLog.add(context, "Protection off", reason);
    }

    static synchronized void update(Context context, boolean shizuku, boolean wifi) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!p.getBoolean(KEY_ENABLED, false)) { cancelAlarm(context); return; }
        long now = System.currentTimeMillis();
        long start = p.getLong(KEY_OFFLINE_SINCE, 0L);
        if (start != 0L && now >= start + GRACE_MS) {
            disable(context, "24-hour grace elapsed; reconnect Wi-Fi and Shizuku, then enable again");
            return;
        }
        if (shizuku && wifi) {
            p.edit().putLong(KEY_OFFLINE_SINCE, 0L).apply();
            cancelAlarm(context);
            return;
        }
        if (start == 0L) {
            start = now;
            p.edit().putLong(KEY_OFFLINE_SINCE, start).apply();
            GuardLog.add(context, "24-hour grace started", !wifi ? "Wi-Fi is disconnected" : "Shizuku is unavailable or not authorized");
        }
        long expiry = start + GRACE_MS;
        if (now >= expiry) {
            disable(context, "24-hour Wi-Fi/Shizuku grace elapsed");
        } else {
            schedule(context, expiry);
        }
    }

    static long remaining(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!p.getBoolean(KEY_ENABLED, false)) return 0L;
        long start = p.getLong(KEY_OFFLINE_SINCE, 0L);
        return start == 0L ? -1L : Math.max(0L, start + GRACE_MS - System.currentTimeMillis());
    }

    static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false);
    }

    static void refreshCurrent(Context context) {
        update(context, ShizukuStatus.isAuthorized(), isWifiConnected(context));
    }

    static boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network n = cm.getActiveNetwork();
        NetworkCapabilities caps = n == null ? null : cm.getNetworkCapabilities(n);
        return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    private static void schedule(Context context, long atMillis) {
        AlarmManager alarms = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, alarmIntent(context));
    }

    private static void cancelAlarm(Context context) {
        AlarmManager alarms = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if (alarms != null) alarms.cancel(alarmIntent(context));
    }

    private static PendingIntent alarmIntent(Context context) {
        Intent intent = new Intent(context, LeaseAlarmReceiver.class);
        return PendingIntent.getBroadcast(context, ALARM_REQUEST, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
