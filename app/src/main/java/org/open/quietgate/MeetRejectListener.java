package org.open.quietgate;

import android.app.Notification;
import android.app.Notification.Action;
import android.app.Person;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ComponentName;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Patterns;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import rikka.shizuku.Shizuku;

public class MeetRejectListener extends NotificationListenerService {
    private static volatile boolean connected;
    private static final String MEET_PACKAGE = "com.google.android.apps.tachyon";
    private static final String PREFS = "quiet_gate";
    private static final String KEY_ENABLED = "strict_enabled";
    private static final int NOTIFICATION_ID_CALL_PERSON = 0;
    private static final Pattern PHONE = Pattern.compile("(?<![\\w])(?:\\+?\\d[\\d ().-]{5,}\\d)(?!\\w)");
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Set<String> processed = Collections.synchronizedSet(new HashSet<>());
    private ConnectivityManager connectivity;
    private ConnectivityManager.NetworkCallback networkCallback;
    private final Shizuku.OnBinderReceivedListener binderReceived = this::refreshLease;
    private final Shizuku.OnBinderDeadListener binderDead = this::refreshLease;

    static boolean isConnected() { return connected; }

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        connected = true;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("listener_connected", true).apply();
        GuardLog.add(this, "Meet listener connected", "Android delivered notification-listener connection");
        connectivity = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override public void onAvailable(Network network) { refreshLease(); }
                @Override public void onLost(Network network) { refreshLease(); }
                @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) { refreshLease(); }
            };
            try { connectivity.registerDefaultNetworkCallback(networkCallback); } catch (RuntimeException ignored) { }
        }
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceived);
            Shizuku.addBinderDeadListener(binderDead);
        } catch (RuntimeException ignored) { }
        refreshLease();
        main.post(this::cleanupRejectedAlerts);
    }

    @Override public void onListenerDisconnected() {
        connected = false;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("listener_connected", false).apply();
        GuardLog.add(this, "Meet listener disconnected", "Android notification monitoring is not currently confirmed");
        super.onListenerDisconnected();
    }

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !MEET_PACKAGE.equals(sbn.getPackageName())) return;
        refreshLease();
        if (RejectedCallStore.shouldDismiss(this, sbn, missedCallerIdentifier(sbn.getNotification()))) {
            dismissRejectedAlert(sbn);
            return;
        }
        String key = sbn.getKey();
        if (key == null || !processed.add(key)) return;
        Notification n = sbn.getNotification();
        if (n != null && "missed_call".equals(n.category)) { processed.remove(key); return; }
        if (n == null || !looksLikeCall(n)) { processed.remove(key); return; }
        String identifier = findIdentifier(n);
        // A new call supersedes the earlier identity-only cleanup window for that caller.
        RejectedCallStore.clearIdentity(this, identifier);
        refreshLease();
        if (!getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ENABLED, false)) {
            GuardLog.add(this, "Observed while paused", "Meet call notification; no action taken"); return;
        }
        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            GuardLog.add(this, "Paused: contacts access missing", "No call action taken"); return;
        }
        Action decline = findDeclineAction(n);
        if (decline == null || decline.actionIntent == null) {
            processed.remove(key); // Meet may add its action in a subsequent update of the same notification.
            GuardLog.add(this, "Cannot reject", "Meet did not expose a Decline/Reject action"); return;
        }
        worker.execute(() -> {
            if (!mayActNow()) return;
            boolean match = identifier != null && ContactMatcher.isSavedContact(this, identifier);
            if (match) {
                RejectedCallStore.clearIdentity(this, identifier);
                GuardLog.add(this, "Allowed", "Caller " + identifierKind(identifier) + " matches available saved contacts");
                return;
            }
            if (!mayActNow()) return;
            // A saved action may outlive the ringing notification while contact lookup runs.
            // Only invoke the same action while it is still exposed by an active Meet call.
            if (!currentDecline(sbn, decline)) {
                GuardLog.add(this, "Queued action skipped", "The original Meet call action is no longer current");
                return;
            }
            try {
                decline.actionIntent.send();
                RejectedCallStore.record(this, sbn, identifier);
                CallCatalog.add(this, identifier, true);
                main.post(this::cleanupRejectedAlerts);
                GuardLog.add(this, "Decline invoked · unconfirmed", identifier == null
                        ? "Caller identifier unavailable; Meet action was invoked" : "Caller " + identifierKind(identifier) + " not found in available saved contacts; Meet action was invoked");
            } catch (PendingIntent.CanceledException | RuntimeException e) {
                CallCatalog.add(this, identifier, false);
                GuardLog.add(this, "Decline failed", "Meet action could not be invoked");
            }
        });
    }

    private void cleanupRejectedAlerts() {
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) for (StatusBarNotification sbn : active) {
                if (RejectedCallStore.shouldDismiss(this, sbn, missedCallerIdentifier(sbn.getNotification()))) dismissRejectedAlert(sbn);
            }
        } catch (RuntimeException ignored) { }
    }

    private void dismissRejectedAlert(StatusBarNotification sbn) {
        try {
            cancelMatchedNotification(sbn.getKey());
            GuardLog.add(this, "Rejected-call alert dismissed", "Matched missed-call alert only; the call outcome is not inferred from dismissal");
        } catch (RuntimeException e) {
            GuardLog.add(this, "Alert cleanup unavailable", "Android did not accept the missed-call dismissal request");
        }
    }

    protected void cancelMatchedNotification(String key) { cancelNotification(key); }

    private boolean currentDecline(StatusBarNotification original, Action expected) {
        try {
            if (!MEET_PACKAGE.equals(expected.actionIntent.getCreatorPackage())) return false;
            StatusBarNotification[] active=getActiveNotifications(new String[]{original.getKey()});
            if (active == null) return false;
            for (StatusBarNotification current : active) {
                if (!MEET_PACKAGE.equals(current.getPackageName()) || current.getPostTime()!=original.getPostTime()) continue;
                Notification notification=current.getNotification();
                if (notification == null || !looksLikeCall(notification) || "missed_call".equals(notification.category)) continue;
                Action action=findDeclineAction(notification);
                if (action!=null && expected.actionIntent.equals(action.actionIntent)) return true;
            }
        } catch (RuntimeException unavailable) { /* Fail closed if Android cannot confirm the current action. */ }
        return false;
    }

    private String missedCallerIdentifier(Notification n) {
        if (n == null || !"missed_call".equals(n.category) || n.extras == null) return null;
        CharSequence title = n.extras.getCharSequence(Notification.EXTRA_TITLE);
        if (title == null) return null;
        String value = title.toString().trim();
        // Identity fallback accepts only a whole, single caller identifier, never arbitrary message text.
        if (Patterns.EMAIL_ADDRESS.matcher(value).matches() || PHONE.matcher(value).matches()) return value;
        return null;
    }

    private String identifierKind(String identifier) { return identifier != null && identifier.contains("@") ? "email" : "phone number"; }

    private boolean mayActNow() {
        LeaseManager.refreshCurrent(this);
        if (!LeaseManager.isEnabled(this)) {
            GuardLog.add(this, "Queued action skipped", "Protection was paused or its grace expired before the action");
            return false;
        }
        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            GuardLog.add(this, "Queued action skipped", "Contacts permission is no longer available");
            return false;
        }
        return true;
    }

    @Override public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn != null && sbn.getKey() != null) processed.remove(sbn.getKey());
    }

    @Override public void onDestroy() {
        main.removeCallbacksAndMessages(null);
        connected = false;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("listener_connected", false).apply();
        if (connectivity != null && networkCallback != null) {
            try { connectivity.unregisterNetworkCallback(networkCallback); } catch (RuntimeException ignored) { }
        }
        try {
            Shizuku.removeBinderReceivedListener(binderReceived);
            Shizuku.removeBinderDeadListener(binderDead);
        } catch (RuntimeException ignored) { }
        worker.shutdownNow(); super.onDestroy();
    }

    private void refreshLease() {
        boolean wifi = false;
        if (connectivity != null) {
            Network active = connectivity.getActiveNetwork();
            NetworkCapabilities caps = active == null ? null : connectivity.getNetworkCapabilities(active);
            wifi = caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        }
        LeaseManager.update(this, ShizukuStatus.isAuthorized(), wifi);
    }

    private boolean looksLikeCall(Notification n) {
        if (Notification.CATEGORY_CALL.equals(n.category)) return true;
        Bundle e = n.extras;
        if (e == null) return false;
        if (Build.VERSION.SDK_INT >= 31 && e.getBoolean("android.callIsVideo", false)) return true;
        String text = notificationText(n).toLowerCase(Locale.ROOT);
        return text.contains("incoming call") || text.contains("video call") || text.contains("calling you")
                || text.contains("wants to call") || text.contains("meet call");
    }

    private Action findDeclineAction(Notification n) {
        if (n.actions == null) return null;
        for (Action a : n.actions) {
            if (a == null || a.title == null) continue;
            String label = a.title.toString().trim().toLowerCase(Locale.ROOT);
            if (label.equals("decline") || label.equals("reject") || label.startsWith("decline ") || label.startsWith("reject ")) return a;
        }
        return null;
    }

    private String findIdentifier(Notification n) {
        if (Build.VERSION.SDK_INT >= 31 && n.extras != null) {
            Object p = n.extras.get("android.callPerson");
            if (p instanceof Person) {
                String uri = ((Person)p).getUri();
                String fromUri = identifierFromUri(uri);
                if (fromUri != null) return fromUri;
            }
        }
        String text = notificationText(n);
        Matcher mail = Patterns.EMAIL_ADDRESS.matcher(text);
        if (mail.find()) return mail.group().trim();
        Matcher phone = PHONE.matcher(text);
        if (phone.find()) return phone.group().trim();
        return null;
    }

    private String identifierFromUri(String uri) {
        if (TextUtils.isEmpty(uri)) return null;
        if (uri.regionMatches(true, 0, "mailto:", 0, 7)) return uri.substring(7).trim();
        if (uri.regionMatches(true, 0, "tel:", 0, 4)) return uri.substring(4).trim();
        return null;
    }

    private String notificationText(Notification n) {
        StringBuilder out = new StringBuilder();
        Bundle e = n.extras;
        if (e != null) {
            append(out, e.getCharSequence(Notification.EXTRA_TITLE));
            append(out, e.getCharSequence(Notification.EXTRA_TEXT));
            append(out, e.getCharSequence(Notification.EXTRA_SUB_TEXT));
            append(out, e.getCharSequence(Notification.EXTRA_BIG_TEXT));
            append(out, e.getCharSequence(Notification.EXTRA_SUMMARY_TEXT));
            CharSequence[] lines = e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (lines != null) for (CharSequence line : lines) append(out, line);
        }
        return out.toString();
    }

    private void append(StringBuilder b, CharSequence value) {
        if (!TextUtils.isEmpty(value)) b.append(value).append('\n');
    }
}
