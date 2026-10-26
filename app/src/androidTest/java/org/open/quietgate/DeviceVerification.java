package org.open.quietgate;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.ContactsContract;
import android.service.notification.StatusBarNotification;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Device tests invoke the real parsing/decision path with synthetic notifications and a test-owned action.
 * No Meet call is placed, no real notification is posted, and contact values never leave the device. */
public final class DeviceVerification extends Instrumentation {
    private Context target;
    private TestListener listener;
    private ExecutorService worker;
    private final AtomicInteger actions = new AtomicInteger();
    private final StringBuilder report = new StringBuilder();
    private int failed;
    private int sequence;
    private Bundle arguments;
    private static final String SIGNAL = "org.open.quietgate.DEVICE_TEST_ACTION";
    private static final String UNKNOWN = "quietgate-device-test@example.invalid";

    @Override public void onCreate(Bundle args) { super.onCreate(args); arguments = args; start(); }
    // The legacy overload below is reached only before API 33; newer versions use NOT_EXPORTED.
    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override public void onStart() {
        target = getTargetContext();
        if (arguments != null && "true".equals(arguments.getString("probe"))) { probeContacts(); return; }
        SharedPreferences prefs = target.getSharedPreferences(LeaseManager.PREFS, Context.MODE_PRIVATE);
        boolean originalEnabled = prefs.getBoolean(LeaseManager.KEY_ENABLED, false);
        long originalOffline = prefs.getLong("offline_since", 0L);
        String originalRecords = prefs.getString(RejectedCallStore.KEY_RECORDS, "{}");
        String originalCatalog = prefs.getString(CallCatalog.KEY, null);
        boolean originalQuiet = prefs.getBoolean(RejectedCallStore.KEY_QUIET, true);
        GuardLog.add(target, "Synthetic verification started", "The following test decisions do not represent real calls");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) { actions.incrementAndGet(); }
        };
        try {
            if (Build.VERSION.SDK_INT >= 33) target.registerReceiver(receiver, new IntentFilter(SIGNAL), Context.RECEIVER_NOT_EXPORTED);
            else target.registerReceiver(receiver, new IntentFilter(SIGNAL));
            listener = new TestListener(target);
            Field connectivity = MeetRejectListener.class.getDeclaredField("connectivity");
            connectivity.setAccessible(true);
            connectivity.set(listener, target.getSystemService(ConnectivityManager.class));
            Field executor = MeetRejectListener.class.getDeclaredField("worker");
            executor.setAccessible(true); worker = (ExecutorService) executor.get(listener);
            LeaseManager.enable(target, ShizukuStatus.isAuthorized(), LeaseManager.isWifiConnected(target));

            check("Unknown caller invokes the supplied decline action", () -> {
                require(!ContactMatcher.isSavedContact(target, UNKNOWN), "Reserved test identifier unexpectedly matched");
                int before = actions.get(); post("com.google.android.apps.tachyon", UNKNOWN, "Decline");
                awaitActions(before + 1); require(latest().contains("unconfirmed"), "Outcome should remain unconfirmed");
            });
            check("Missing caller identifier invokes supplied reject action", () -> {
                int before = actions.get(); post("com.google.android.apps.tachyon", "Unknown caller", "Reject");
                awaitActions(before + 1); require(latest().contains("identifier unavailable"), "Missing identifier not reported");
            });
            check("A saved contact is allowed without invoking decline", () -> {
                String saved = firstContact(); require(saved != null, "No saved email or phone available for this test");
                int before = actions.get(); post("com.google.android.apps.tachyon", saved, "Decline");
                drain(); require(actions.get() == before, "Action invoked for saved contact");
                require(latest().contains("Allowed"), "Saved contact was not recognized");
            });
            check("A saved phone number matches without punctuation", () -> {
                String saved = firstPhone(); require(saved != null, "No phone contact available for this test");
                String digits = saved.replaceAll("[^0-9]", "");
                require(ContactMatcher.isSavedContact(target, digits), "Saved phone did not match its digits");
                int before = actions.get(); post("com.google.android.apps.tachyon", digits, "Decline");
                drain(); require(actions.get() == before && latest().contains("Allowed"), "Saved phone was not allowed");
            });
            check("An unsaved phone number invokes the supplied decline action", () -> {
                String unknownPhone = "+999 000 000 0199";
                require(!ContactMatcher.isSavedContact(target, unknownPhone), "Test phone unexpectedly matched");
                int before = actions.get(); post("com.google.android.apps.tachyon", unknownPhone, "Decline");
                awaitActions(before + 1);
                require(latest().contains("phone number not found"), "Phone decision was not identified");
            });
            check("Hang up is never treated as an incoming-call decline", () -> {
                int before = actions.get(); post("com.google.android.apps.tachyon", UNKNOWN, "Hang up");
                drain(); require(actions.get() == before, "Hang-up action invoked");
                require(latest().contains("Cannot reject"), "Missing decline action not reported");
            });
            check("Notifications from other packages are ignored", () -> {
                int before = actions.get(); post("org.open.quietgate.test", UNKNOWN, "Decline");
                drain(); require(actions.get() == before, "Another package was processed");
            });
            check("Paused protection takes no call action", () -> {
                LeaseManager.disable(target, "Device verification: pause test");
                int before = actions.get(); post("com.google.android.apps.tachyon", UNKNOWN, "Decline");
                drain(); require(actions.get() == before, "Action invoked while paused");
                require(latest().contains("Observed while paused"), "Pause decision not logged");
                LeaseManager.enable(target, ShizukuStatus.isAuthorized(), LeaseManager.isWifiConnected(target));
            });
            check("Pausing while contact lookup is queued cancels the pending action", () -> {
                CountDownLatch gate = new CountDownLatch(1);
                worker.execute(() -> { try { gate.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
                int before = actions.get();
                try {
                    post("com.google.android.apps.tachyon", UNKNOWN, "Decline");
                    LeaseManager.disable(target, "Device verification: queued-action pause test");
                } finally { gate.countDown(); }
                drain(); require(actions.get() == before, "Queued action still executed after protection was paused");
            });
            check("Only a matched rejected missed-call alert is dismissed", () -> {
                LeaseManager.enable(target, ShizukuStatus.isAuthorized(), LeaseManager.isWifiConnected(target));
                prefs.edit().putBoolean(RejectedCallStore.KEY_QUIET, true).commit();
                StatusBarNotification call = post("com.google.android.apps.tachyon", UNKNOWN, "Decline");
                drain(); int before = listener.dismissed.get();
                StatusBarNotification missed = missed(call, false);
                runOnMainSync(() -> listener.onNotificationPosted(missed));
                require(listener.dismissed.get() == before + 1, "Matched missed call was not dismissed");
                require(!RejectedCallStore.shouldDismiss(target, call), "Active call qualified for cleanup");
                require(!RejectedCallStore.shouldDismiss(target, missed(call, true)), "Ongoing notification qualified for cleanup");
            });
            check("Unrelated missed calls and contact missed calls remain visible", () -> {
                int before = listener.dismissed.get();
                StatusBarNotification unrelated = post("com.google.android.apps.tachyon", UNKNOWN, "Hang up");
                runOnMainSync(() -> listener.onNotificationPosted(missed(unrelated, false)));
                String saved = firstContact();
                StatusBarNotification known = post("com.google.android.apps.tachyon", saved, "Decline"); drain();
                runOnMainSync(() -> listener.onNotificationPosted(missed(known, false)));
                require(listener.dismissed.get() == before, "Unrelated or contact missed call dismissed");
            });
            check("Quiet cleanup honors its toggle and retention deadline", () -> {
                StatusBarNotification call = post("com.google.android.apps.tachyon", UNKNOWN, "Decline"); drain();
                StatusBarNotification missed = missed(call, false);
                prefs.edit().putBoolean(RejectedCallStore.KEY_QUIET, false).commit();
                require(!RejectedCallStore.shouldDismiss(target, missed), "Quiet toggle ignored");
                prefs.edit().putBoolean(RejectedCallStore.KEY_QUIET, true).commit();
                RejectedCallStore.record(target, call, System.currentTimeMillis() - RejectedCallStore.RETENTION_MS - 1_000);
                require(!RejectedCallStore.shouldDismiss(target, missed), "Expired rejection matched");
            });
            check("Changed notification keys match only the recent exact caller", () -> {
                StatusBarNotification call = post("com.google.android.apps.tachyon", UNKNOWN, "Decline"); drain();
                StatusBarNotification changed = new StatusBarNotification(call.getPackageName(), call.getPackageName(), 998877,
                        "changed-key", Process.myUid(), Process.myPid(), 0, missed(call, false).getNotification(),
                        Process.myUserHandle(), System.currentTimeMillis());
                require(RejectedCallStore.shouldDismiss(target, changed, UNKNOWN), "Changed-key fallback did not match");
                require(!RejectedCallStore.shouldDismiss(target, changed, "other-device-test@example.invalid"), "Different caller matched");
                changed.getNotification().flags |= Notification.FLAG_GROUP_SUMMARY;
                require(!RejectedCallStore.shouldDismiss(target, changed, UNKNOWN), "Group summary matched");
                changed.getNotification().flags &= ~Notification.FLAG_GROUP_SUMMARY;
                RejectedCallStore.record(target, call, UNKNOWN, System.currentTimeMillis() - RejectedCallStore.IDENTITY_RETENTION_MS - 1_000);
                require(!RejectedCallStore.shouldDismiss(target, changed, UNKNOWN), "Old identity fallback remained active");
            });
            check("An outage starts grace and recovery resets it", () -> {
                LeaseManager.enable(target, true, true);
                LeaseManager.update(target, false, true);
                long remaining = LeaseManager.remaining(target);
                require(remaining > 0 && remaining <= LeaseManager.GRACE_MS, "Grace was not armed");
                long start = prefs.getLong("offline_since", 0L);
                LeaseManager.update(target, false, true);
                require(prefs.getLong("offline_since", 0L) == start, "Repeated checks restarted the timer");
                LeaseManager.update(target, true, true);
                require(LeaseManager.remaining(target) == -1L, "Recovery did not reset the timer");
            });
            check("A grace deadline older than 24 hours disables protection", () -> {
                LeaseManager.enable(target, true, true);
                prefs.edit().putLong("offline_since", System.currentTimeMillis() - LeaseManager.GRACE_MS - 1_000).commit();
                LeaseManager.update(target, false, true);
                require(!LeaseManager.isEnabled(target), "Expired protection remained enabled");
            });
            check("Recovery after the deadline stays off until manual enable", () -> {
                LeaseManager.enable(target, true, true);
                prefs.edit().putLong("offline_since", System.currentTimeMillis() - LeaseManager.GRACE_MS - 1_000).commit();
                LeaseManager.update(target, true, true);
                require(!LeaseManager.isEnabled(target), "Late recovery bypassed the deadline");
                LeaseManager.update(target, true, true);
                require(!LeaseManager.isEnabled(target), "Recovery automatically re-enabled protection");
                LeaseManager.enable(target, true, true);
                require(LeaseManager.isEnabled(target) && LeaseManager.remaining(target) == -1L, "Manual re-enable failed");
            });
            check("Purpose, permissions and license documents are bundled", () -> {
                for (String file : new String[]{"ABOUT.txt", "LICENSE", "THIRD_PARTY_NOTICES.txt", "SHIZUKU-LICENSE.txt", "APACHE-2.0.txt"}) {
                    try (java.io.InputStream in = target.getAssets().open(file)) { require(in.read() != -1, "Empty license document"); }
                }
            });
        } catch (Throwable e) { failed++; report.append("FAIL test harness: ").append(e.getClass().getSimpleName()).append('\n'); }
        finally {
            if (worker != null) worker.shutdownNow();
            try { target.unregisterReceiver(receiver); } catch (RuntimeException ignored) { }
            if (originalEnabled) LeaseManager.enable(target, ShizukuStatus.isAuthorized(), LeaseManager.isWifiConnected(target));
            else LeaseManager.disable(target, null);
            prefs.edit().putLong("offline_since", originalOffline).commit();
            if (originalCatalog == null) prefs.edit().remove(CallCatalog.KEY).commit();
            else prefs.edit().putString(CallCatalog.KEY, originalCatalog).commit();
            prefs.edit().putString(RejectedCallStore.KEY_RECORDS, originalRecords).putBoolean(RejectedCallStore.KEY_QUIET, originalQuiet).commit();
            GuardLog.add(target, "Synthetic verification finished", "Device test failures: " + failed + "; original protection setting restored");
        }
        Bundle results = new Bundle(); results.putString("stream", "\n" + report + "Failures: " + failed + "\n");
        finish(failed == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, results);
    }

    private StatusBarNotification post(String packageName, String identifier, String actionLabel) {
        int id = ++sequence;
        PendingIntent signal = PendingIntent.getBroadcast(target, id, new Intent(SIGNAL).setPackage(target.getPackageName()),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_CANCEL_CURRENT);
        Notification n = new Notification.Builder(target, GuardNotificationService.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info).setCategory(Notification.CATEGORY_CALL)
                .setContentTitle("Incoming video call").setContentText(identifier)
                .addAction(new Notification.Action.Builder(0, actionLabel, signal).build()).build();
        StatusBarNotification sbn = new StatusBarNotification(packageName, packageName, id, "quietgate-device-test-" + id,
                Process.myUid(), Process.myPid(), 0, n, Process.myUserHandle(), System.currentTimeMillis());
        runOnMainSync(() -> listener.onNotificationPosted(sbn));
        return sbn;
    }
    private StatusBarNotification missed(StatusBarNotification original, boolean ongoing) {
        Notification n = new Notification.Builder(target, GuardNotificationService.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info).setCategory("missed_call")
                .setContentTitle(UNKNOWN).setOngoing(ongoing).build();
        return new StatusBarNotification(original.getPackageName(), original.getPackageName(), original.getId(), original.getTag(),
                Process.myUid(), Process.myPid(), 0, n, Process.myUserHandle(), System.currentTimeMillis());
    }
    private String firstContact() {
        try (Cursor c = target.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Email.ADDRESS}, null, null, null)) {
            if (c != null) while (c.moveToNext()) { String value = c.getString(0); if (value != null && !value.trim().isEmpty()) return value; }
        }
        try (Cursor c = target.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null)) {
            if (c != null) while (c.moveToNext()) { String value = c.getString(0); if (value != null && !value.trim().isEmpty()) return value; }
        }
        return null;
    }
    private String firstPhone() {
        try (Cursor c = target.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null)) {
            if (c != null) while (c.moveToNext()) {
                String value = c.getString(0);
                if (value != null && value.matches("[+0-9 ().-]+") && value.replaceAll("[^0-9]", "").length() >= 7) return value;
            }
        }
        return null;
    }
    private String latest() { java.util.List<String> log = GuardLog.recent(target); return log.isEmpty() ? "" : log.get(0); }
    private void probeContacts() {
        StringBuilder output = new StringBuilder();
        java.io.File input = new java.io.File(target.getFilesDir(), "quietgate_probe.json");
        try {
            String json = new String(java.nio.file.Files.readAllBytes(input.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            org.json.JSONArray samples = new org.json.JSONArray(json);
            for (int i = 0; i < samples.length(); i++) {
                org.json.JSONObject sample = samples.getJSONObject(i);
                String value = sample.getString("value");
                boolean lookupMatch = ContactMatcher.isSavedContact(target, value);
                boolean directMatch = false;
                try (Cursor c = target.getContentResolver().query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Email._ID},
                        ContactsContract.CommonDataKinds.Email.ADDRESS + " = ? COLLATE NOCASE", new String[]{value.trim()}, null)) {
                    directMatch = c != null && c.moveToFirst();
                }
                output.append(sample.getString("source")).append(": email; ContactMatcher=").append(lookupMatch)
                        .append("; direct exact email query=").append(directMatch).append('\n');
            }
        } catch (Exception e) { output.append("Probe failed: ").append(e.getClass().getSimpleName()).append('\n'); }
        finally { input.delete(); }
        Bundle result = new Bundle(); result.putString("stream", "\n" + output); finish(Activity.RESULT_OK, result);
    }
    private void drain() throws Exception { worker.submit(() -> {}).get(8, TimeUnit.SECONDS); Thread.sleep(350); }
    private void awaitActions(int count) throws Exception {
        drain(); long until = System.currentTimeMillis() + 3_000;
        while (actions.get() < count && System.currentTimeMillis() < until) Thread.sleep(50);
        require(actions.get() == count, "Expected test action was not received");
    }
    private void check(String name, Checked action) {
        try { action.run(); report.append("PASS ").append(name).append('\n'); }
        catch (Throwable e) { failed++; report.append("FAIL ").append(name).append(": ").append(e.getMessage()).append('\n'); }
    }
    private void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private interface Checked { void run() throws Exception; }
    private static final class TestListener extends MeetRejectListener {
        final AtomicInteger dismissed = new AtomicInteger();
        TestListener(Context context) { attachBaseContext(context); }
        @Override protected void cancelMatchedNotification(String key) { dismissed.incrementAndGet(); }
    }
}
