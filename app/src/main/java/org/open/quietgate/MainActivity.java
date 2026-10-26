package org.open.quietgate;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private static final int REQ_CONTACTS = 22;
    private static final int REQ_EXPORT = 25;
    private static final int REQ_IMPORT = 26;
    private static final int REQ_SHIZUKU = 23;
    private static final int REQ_NOTIFICATIONS = 24;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences observedPreferences;
    private final SharedPreferences.OnSharedPreferenceChangeListener stateListener = (prefs, key) -> handler.post(this::refreshState);
    private final Runnable ticker = new Runnable() {
        private long nextCheck;
        @Override public void run() {
            if (state == null) return;
            long now = android.os.SystemClock.elapsedRealtime();
            if (now >= nextCheck || (LeaseManager.isEnabled(MainActivity.this) && LeaseManager.remaining(MainActivity.this) == 0)) {
                LeaseManager.refreshCurrent(MainActivity.this); refreshState(); nextCheck = now + 30_000L;
            }
            refreshTimer(); handler.postDelayed(this, 1_000L);
        }
    };
    private final Shizuku.OnRequestPermissionResultListener permissionListener = (requestCode, grantResult) ->
            runOnUiThread(() -> { if (requestCode == REQ_SHIZUKU) render(); });
    private LinearLayout content;
    private TextView state;
    private TextView countdown;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        Shizuku.addRequestPermissionResultListener(permissionListener);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        GoogleAutoRefresh.reconcile(this);
        observedPreferences = getSharedPreferences(LeaseManager.PREFS, MODE_PRIVATE);
        observedPreferences.registerOnSharedPreferenceChangeListener(stateListener);
        LeaseManager.refreshCurrent(this);
        if (LeaseManager.isEnabled(this)) GuardNotificationService.start(this);
        render();
        handler.postDelayed(this::refreshState, 500L);
        handler.removeCallbacks(ticker); handler.postDelayed(ticker, 1_000L);
    }

    @Override protected void onPause() {
        if (observedPreferences != null) observedPreferences.unregisterOnSharedPreferenceChangeListener(stateListener);
        handler.removeCallbacksAndMessages(null);
        super.onPause();
    }
    @Override protected void onDestroy() { Shizuku.removeRequestPermissionResultListener(permissionListener); super.onDestroy(); }

    private void render() {
        int pad = dp(22);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(0xFFF2F7F6);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(pad, pad, pad, dp(30)); content.setBackgroundColor(0xFFF2F7F6);
        scroll.addView(content); setContentView(scroll);
        UiInsets.apply(scroll);

        TextView title = text("QuietGate", 30, 0xFF102B33, true); content.addView(title);
        TextView subtitle = text("A quiet gate for Google Meet calls.", 16, 0xFF4A5C5E, false);
        subtitle.setPadding(0, dp(5), 0, dp(20)); content.addView(subtitle);
        state = text("", 15, 0xFF163B43, false); state.setPadding(dp(15), dp(15), dp(15), dp(15));
        state.setBackgroundColor(0xFFDDEEEA); content.addView(state); refreshState();
        countdown = text("", 18, 0xFF163B43, true); countdown.setPadding(dp(15), dp(12), dp(15), dp(12));
        content.addView(countdown); refreshTimer();

        addButton(checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
            ? "Contacts access: allowed" : "Grant contacts access", v -> requestContacts());
        addButton("Open notification access", v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        addButton(ShizukuStatus.isAuthorized() ? "Shizuku authorized ✓" : "Authorize through Shizuku", v -> authorizeShizuku());
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            addButton("Allow status notification", v -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS));
        addButton(LeaseManager.isEnabled(this) ? "Pause protection" : "Enable protection", v -> toggleProtection());
        boolean quiet = getSharedPreferences(LeaseManager.PREFS, MODE_PRIVATE).getBoolean(RejectedCallStore.KEY_QUIET, true);
        addButton(quiet ? "Rejected-call alerts: quiet" : "Rejected-call alerts: show", v -> {
            getSharedPreferences(LeaseManager.PREFS, MODE_PRIVATE).edit().putBoolean(RejectedCallStore.KEY_QUIET, !quiet).apply(); render();
        });
        addButton("Rejected-call catalog", v -> showCatalog());
        addButton("Connected Google saved contacts (optional)", v -> startActivity(new Intent(this, GoogleContactsActivity.class)));
        addButton("Refresh status", v -> { LeaseManager.refreshCurrent(this); render(); });
        addButton("About, privacy, permissions & licenses", v -> startActivity(new Intent(this, AboutActivity.class)));
        addButton("Clear local history & fingerprints", v -> new AlertDialog.Builder(this)
            .setTitle("Clear local history and fingerprints?")
            .setMessage("Deletes recent decisions, the call catalog, and rejection fingerprints on this device. Exported files and connected Google contacts are kept.")
            .setNegativeButton("Cancel", null).setPositiveButton("Clear", (dialog, which) -> {
            getSharedPreferences(LeaseManager.PREFS, MODE_PRIVATE).edit().remove("recent_events")
                    .remove(CallCatalog.KEY).remove(RejectedCallStore.KEY_RECORDS).remove("rejected_identity_salt").apply(); render();
        }).show());

        // Detailed help and recent decisions are available from the About hub.
    }



    private void showCatalog() { showCatalog("All categories"); }
    private void showCatalog(String filter) {
        org.json.JSONArray entries=CallCatalog.recent(this);
        LinearLayout panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18),dp(12),dp(18),dp(12));
        TextView info=text("Local catalog: 500 newest attempts, retained for 90 days. Labels are your assessment, not verified scam findings. Imported entries never change blocking. Export before clearing to keep an archive.",14,0xFF47585A,false);
        panel.addView(info);
        catalogButton(panel,"Create incident report (ZIP)",()->startActivity(new Intent(this,IncidentActivity.class)));
        catalogButton(panel,"Import call logs",()->startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),REQ_IMPORT));
        catalogButton(panel,"Export call logs (JSON)",()->startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
            .setType("application/json").addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_TITLE,"QuietGate-call-catalog.json"),REQ_EXPORT));
        String[] filters=new String[CallCatalog.CATEGORIES.length+1]; filters[0]="All categories";
        System.arraycopy(CallCatalog.CATEGORIES,0,filters,1,CallCatalog.CATEGORIES.length);
        android.widget.Spinner selector=new android.widget.Spinner(this);
        selector.setAdapter(new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,filters));
        selector.setSelection(java.util.Arrays.asList(filters).indexOf(filter)); panel.addView(selector);
        LinearLayout rows=new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL); panel.addView(rows);
        Runnable populate=()->{
            rows.removeAllViews(); int shown=0;
            String selected=selector.getSelectedItem().toString();
            java.text.DateFormat format=java.text.DateFormat.getDateTimeInstance();
            org.json.JSONArray latest=CallCatalog.recent(this);
            for(int i=0;i<latest.length();i++) {
                org.json.JSONObject e=latest.optJSONObject(i); if(e==null) continue;
                String category=e.optString("category",CallCatalog.CATEGORIES[0]);
                if(!selected.equals("All categories") && !selected.equals(category)) continue;
                shown++;
                String label=format.format(new java.util.Date(e.optLong("time")))+"\n"+e.optString("identity")+"\n"
                    +category+" · "+(e.optBoolean("imported")?"Imported":"This device")+"\n"+e.optString("action");
                if(!e.optString("notes").isEmpty()) label+="\n"+e.optString("notes");
                final org.json.JSONObject entry=e;
                catalogButton(rows,label,()->editCatalogEntry(entry,selected));
            }
            if(shown==0) rows.addView(text("No records in this category.",15,0xFF47585A,false));
        };
        selector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> a,View v,int position,long id) { populate.run(); }
            public void onNothingSelected(android.widget.AdapterView<?> a) {}
        });
        populate.run();
        ScrollView scroll=new ScrollView(this); scroll.addView(panel);
        catalogDialog=new AlertDialog.Builder(this).setTitle("Call logs · "+entries.length()).setView(scroll)
            .setPositiveButton("Close",null)
            .setNegativeButton("Clear catalog",(d,w)->new AlertDialog.Builder(this).setTitle("Clear call catalog?")
                .setMessage("Delete stored caller identities, categories and notes? Exported copies are not deleted.")
                .setPositiveButton("Delete",(a,b)->{CallCatalog.clear(this);showCatalog();}).setNegativeButton("Cancel",null).show()).create();
        catalogDialog.show();
    }
    private AlertDialog catalogDialog;
    private void catalogButton(LinearLayout parent,String label,Runnable action) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        b.setTextColor(0xFFFFFFFF); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF17545A));
        b.setMinHeight(dp(48)); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(8);
        parent.addView(b,lp); b.setOnClickListener(v->action.run());
    }
    private void editCatalogEntry(org.json.JSONObject entry,String filter) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18),dp(12),dp(18),dp(12));
        android.widget.Spinner category=new android.widget.Spinner(this);
        category.setAdapter(new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,CallCatalog.CATEGORIES));
        int index=java.util.Arrays.asList(CallCatalog.CATEGORIES).indexOf(entry.optString("category")); category.setSelection(Math.max(0,index)); box.addView(category);
        android.widget.EditText notes=new android.widget.EditText(this); notes.setHint("Your notes (up to 1,000 characters)");
        notes.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        notes.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1000)});
        notes.setText(entry.optString("notes")); box.addView(notes);
        new AlertDialog.Builder(this).setTitle("Categorize attempt").setMessage(entry.optString("identity")+"\nThis does not change call protection.").setView(box)
            .setPositiveButton("Save",(d,w)->{CallCatalog.edit(this,entry.optString("id"),category.getSelectedItem().toString(),notes.getText().toString());
                if(catalogDialog!=null) catalogDialog.dismiss();showCatalog(filter);}).setNegativeButton("Cancel",null).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if((request!=REQ_EXPORT && request!=REQ_IMPORT) || result!=RESULT_OK || data==null || data.getData()==null) return;
        final android.net.Uri uri=data.getData();
        // Snapshot export now; document IO and import parsing run off the UI thread.
        String snapshot;
        try {snapshot=request==REQ_EXPORT?CallCatalog.exportJson(this):null;}
        catch(Exception e) {return;}
        new Thread(()->{
            String message;
            try {
                if(request==REQ_EXPORT) {
                    try(java.io.OutputStream out=getContentResolver().openOutputStream(uri,"wt")) {
                        if(out==null) throw new java.io.IOException();
                        out.write(snapshot.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                    message="Export complete. The file contains caller identities, categories and notes.";
                } else {
                    String json;
                    try(java.io.InputStream in=getContentResolver().openInputStream(uri); java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()) {
                        if(in==null) throw new java.io.IOException(); byte[] buffer=new byte[4096]; int n;
                        while((n=in.read(buffer))!=-1) {if(out.size()+n>2*1024*1024) throw new java.io.IOException("File too large");out.write(buffer,0,n);}
                        json=new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);
                    }
                    if(json.startsWith("\uFEFF")) json=json.substring(1);
                    message=CallCatalog.merge(this,json);
                }
            } catch(Exception e) {message=request==REQ_IMPORT?"Import failed. Choose a QuietGate JSON catalog (up to 2 MB / 5,000 input records).":"Export failed. The local catalog remains available.";}
            final String notice=message;
            runOnUiThread(()->{if(isFinishing() || isDestroyed()) return;
                if(catalogDialog!=null) catalogDialog.dismiss();
                new AlertDialog.Builder(this).setTitle(request==REQ_IMPORT?"Import result":"Export result").setMessage(notice)
                    .setPositiveButton("Open catalog",(d,w)->showCatalog()).setNegativeButton("Close",null).show();});
        },"QuietGate-catalog-IO").start();
    }

    private void refreshState() {
        if (state == null) return;
        boolean contacts = checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        boolean notifications = notificationAccess();
        boolean statusNotificationAllowed = statusNotificationAllowed();
        boolean shizuku = ShizukuStatus.isAuthorized();
        boolean wifi = LeaseManager.isWifiConnected(this);
        boolean enabled = LeaseManager.isEnabled(this);
        long remaining = LeaseManager.remaining(this);
        boolean listenerConnected = MeetRejectListener.isConnected();
        boolean statusService = GuardNotificationService.isRunning();
        state.setText("Protection: " + (enabled ? "On" : "Off") + "\nContacts: " + yesNo(contacts) +
                "\nNotification access: " + yesNo(notifications) + "\nMeet listener: " + (listenerConnected ? "connected" : "not confirmed") +
                "\nStatus service: " + (statusService ? "running" : "not running") + "\nStatus notifications: " + yesNo(statusNotificationAllowed) +
                "\nShizuku: " + (shizuku ? "authorized and running" : "not ready") +
                "\nWi-Fi: " + (wifi ? "connected" : "disconnected"));
        refreshTimer();
    }

    private void refreshTimer() {
        if (countdown == null) return;
        long remaining = LeaseManager.remaining(this);
        countdown.setText(!LeaseManager.isEnabled(this) ? "Protection paused\nReconnect Wi-Fi and Shizuku to enable" : remaining < 0
                ? "24-hour grace: not running\nWi-Fi and Shizuku ready" : "Grace remaining\n" + duration(remaining));
    }


    private void requestContacts() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) { render(); return; }
        requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQ_CONTACTS);
    }

    private void authorizeShizuku() {
        if (ShizukuStatus.isAuthorized()) { render(); return; }
        if (!ShizukuStatus.isRunning()) {
            new AlertDialog.Builder(this).setTitle("Start Shizuku first")
                    .setMessage("Open Shizuku or Sui, start its service, then return here. On non-rooted phones, Shizuku usually must be started again after a reboot.")
                    .setNegativeButton("Later", null)
                    .setPositiveButton("Open Shizuku", (d, w) -> {
                        Intent launch = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
                        if (launch != null) startActivity(launch);
                    }).show(); return;
        }
        try { Shizuku.requestPermission(REQ_SHIZUKU); }
        catch (RuntimeException e) {
            new AlertDialog.Builder(this).setMessage("Could not request Shizuku authorization. Check that its service is running, then try again.").setPositiveButton("OK", null).show();
        }
    }

    private void toggleProtection() {
        if (LeaseManager.isEnabled(this)) {
            LeaseManager.disable(this, "Paused by user"); render(); return;
        }
        boolean contacts = checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        if (!contacts || !notificationAccess() || !ShizukuStatus.isAuthorized() || !LeaseManager.isWifiConnected(this)) {
            new AlertDialog.Builder(this).setTitle("Finish setup first")
                    .setMessage("Grant Contacts and Notification access, connect Wi-Fi, and start/authorize Shizuku before enabling protection.")
                    .setPositiveButton("OK", null).show(); return;
        }
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this).setTitle("Allow the status notification?")
                    .setMessage("Android requires notification permission to show QuietGate's ongoing protection status in the notification drawer.")
                    .setNegativeButton("Not now", null)
                    .setPositiveButton("Allow", (d, w) -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS)).show();
            return;
        }
        new AlertDialog.Builder(this).setTitle("Enable QuietGate?")
                .setMessage("QuietGate allows a caller when Meet exposes an email or phone number that matches Contacts on this phone. Save the caller's exact Meet email; a phone-only contact may not match. If the identifier is missing or not saved, it attempts Meet’s explicit Decline/Reject action. The call can ring briefly, and rejection is unconfirmed. If Wi-Fi or Shizuku stays unavailable for 24 hours, protection turns off.")
                .setNegativeButton("Keep off", null)
                .setPositiveButton("Enable", (d, w) -> {
                    LeaseManager.enable(this, ShizukuStatus.isAuthorized(), LeaseManager.isWifiConnected(this));
                    boolean statusStarted = GuardNotificationService.start(this);
                    if (!statusStarted) GuardLog.add(this, "Status notification unavailable", "Protection is enabled, but Android did not start the visible status service");
                    GuardLog.add(this, "Protection enabled", "24-hour Wi-Fi/Shizuku grace armed"); render();
                }).show();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CONTACTS || requestCode == REQ_NOTIFICATIONS) render();
    }

    private boolean notificationAccess() {
        String listeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (listeners == null) return false;
        android.content.ComponentName expected = new android.content.ComponentName(this, MeetRejectListener.class);
        for (String value : listeners.split(":")) {
            if (expected.equals(android.content.ComponentName.unflattenFromString(value))) return true;
        }
        return false;
    }
    private boolean statusNotificationAllowed() {
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return false;
        android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
        if (manager == null || !manager.areNotificationsEnabled()) return false;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            android.app.NotificationChannel channel = manager.getNotificationChannel(GuardNotificationService.CHANNEL_ID);
            return channel == null || channel.getImportance() != android.app.NotificationManager.IMPORTANCE_NONE;
        }
        return true;
    }
    private String yesNo(boolean v) { return v ? "allowed" : "not allowed"; }
    private String duration(long ms) {
        long seconds = Math.max(0, ms / 1000); long hours = seconds / 3600; long minutes = (seconds % 3600) / 60;
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds % 60);
    }
    private void section(String label) {
        TextView heading = text(label, 18, 0xFF16434A, true); heading.setPadding(0, dp(23), 0, dp(7)); content.addView(heading);
    }
    private void paragraph(String value) {
        TextView p = text(value, 15, 0xFF47585A, false); p.setPadding(0, dp(4), 0, dp(7)); content.addView(p);
    }
    private void addButton(String label, View.OnClickListener click) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(0xFFFFFFFF);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF17545A));
        b.setMinHeight(dp(52)); b.setPadding(dp(16), dp(10), dp(16), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(9);
        content.addView(b, lp); b.setOnClickListener(click);
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.START); return t;
    }
    private int dp(int x) { return (int)(x * getResources().getDisplayMetrics().density + .5f); }
}
