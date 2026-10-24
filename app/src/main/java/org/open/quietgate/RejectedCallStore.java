package org.open.quietgate;

import android.app.Notification;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Iterator;
import org.json.JSONObject;

/** Remembers only short-lived hashed call keys/tags, never caller identities or account credentials. */
final class RejectedCallStore {
    static final String KEY_QUIET = "quiet_rejected_alerts";
    static final String KEY_RECORDS = "rejected_call_fingerprints";
    static final long RETENTION_MS = 15 * 60_000L;
    static final long IDENTITY_RETENTION_MS = 2 * 60_000L;
    private static final int MAX_FINGERPRINTS = 128;

    static synchronized void record(Context context, StatusBarNotification call) {
        record(context, call, System.currentTimeMillis());
    }

    static synchronized void record(Context context, StatusBarNotification call, String identifier) {
        record(context, call, identifier, System.currentTimeMillis());
    }

    static synchronized void record(Context context, StatusBarNotification call, String identifier, long at) {
        record(context, call, at);
        if (identifier == null) return;
        JSONObject records = read(context);
        try {
            records.put(identity(context, identifier), at);
            bound(records);
            context.getSharedPreferences(LeaseManager.PREFS, Context.MODE_PRIVATE).edit().putString(KEY_RECORDS, records.toString()).apply();
        } catch (org.json.JSONException ignored) { }
    }

    static synchronized void clearIdentity(Context context, String identifier) {
        if (identifier == null) return;
        JSONObject records = read(context); records.remove(identity(context, identifier));
        context.getSharedPreferences(LeaseManager.PREFS, Context.MODE_PRIVATE).edit().putString(KEY_RECORDS, records.toString()).apply();
    }

    static synchronized void record(Context context, StatusBarNotification call, long at) {
        JSONObject records = read(context);
        prune(records, System.currentTimeMillis());
        try {
            records.put(key(call), at);
            String tag = tag(call);
            if (tag != null) records.put(tag, at);
            bound(records);
            context.getSharedPreferences(LeaseManager.PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY_RECORDS, records.toString()).apply();
        } catch (org.json.JSONException ignored) { }
    }

    static synchronized boolean shouldDismiss(Context context, StatusBarNotification sbn) {
        return shouldDismiss(context, sbn, null);
    }

    static synchronized boolean shouldDismiss(Context context, StatusBarNotification sbn, String singleCaller) {
        if (sbn == null || !"com.google.android.apps.tachyon".equals(sbn.getPackageName()) || !LeaseManager.isEnabled(context)) return false;
        if (!context.getSharedPreferences(LeaseManager.PREFS, Context.MODE_PRIVATE).getBoolean(KEY_QUIET, true)) return false;
        Notification n = sbn.getNotification();
        if (n == null || sbn.isOngoing() || (n.flags & (Notification.FLAG_FOREGROUND_SERVICE | Notification.FLAG_GROUP_SUMMARY)) != 0) return false;
        // A ring, active call, group summary, or general message must never be hidden by this feature.
        if (!"missed_call".equals(n.category) || Notification.CATEGORY_CALL.equals(n.category)) return false;
        JSONObject records = read(context);
        long now = System.currentTimeMillis();
        if (fresh(records.optLong(key(sbn), 0), now) || (tag(sbn) != null && fresh(records.optLong(tag(sbn), 0), now))) return true;
        if (singleCaller == null || (n.extras != null && n.extras.getBoolean("android.isGroupConversation", false))) return false;
        long at = records.optLong(identity(context, singleCaller), 0);
        return at > 0 && now >= at && now - at < IDENTITY_RETENTION_MS;
    }

    private static boolean fresh(long at, long now) { return at > 0 && now >= at && now - at < RETENTION_MS; }
    private static void bound(JSONObject records) {
        while (records.length() > MAX_FINGERPRINTS) {
            String oldest = null; long oldestAt = Long.MAX_VALUE;
            for (Iterator<String> it = records.keys(); it.hasNext();) {
                String candidate = it.next(); long time = records.optLong(candidate);
                if (time < oldestAt) { oldestAt = time; oldest = candidate; }
            }
            if (oldest == null) break;
            records.remove(oldest);
        }
    }
    private static JSONObject read(Context context) {
        try { return new JSONObject(context.getSharedPreferences(LeaseManager.PREFS, Context.MODE_PRIVATE).getString(KEY_RECORDS, "{}")); }
        catch (Exception ignored) { return new JSONObject(); }
    }
    private static void prune(JSONObject records, long now) {
        java.util.List<String> remove = new java.util.ArrayList<>();
        for (Iterator<String> it = records.keys(); it.hasNext();) { String k = it.next(); if (!fresh(records.optLong(k), now)) remove.add(k); }
        for (String k : remove) records.remove(k);
    }
    private static String key(StatusBarNotification sbn) { return hash("key:" + sbn.getKey()); }
    private static String identity(Context context, String identifier) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(LeaseManager.PREFS, Context.MODE_PRIVATE);
        String salt = prefs.getString("rejected_identity_salt", null);
        if (salt == null) { salt = java.util.UUID.randomUUID().toString(); prefs.edit().putString("rejected_identity_salt", salt).apply(); }
        String canonical = identifier.contains("@") ? identifier.trim().toLowerCase(java.util.Locale.ROOT) : identifier.replaceAll("[^0-9]", "");
        return hash("caller:" + salt + ":" + canonical);
    }
    private static String tag(StatusBarNotification sbn) {
        return sbn.getTag() == null || sbn.getTag().isEmpty() ? null
                : hash("tag:" + sbn.getUserId() + ":" + sbn.getPackageName() + ":" + sbn.getTag());
    }
    private static String hash(String text) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
