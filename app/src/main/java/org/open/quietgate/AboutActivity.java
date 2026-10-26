package org.open.quietgate;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.widget.*;
import java.nio.charset.StandardCharsets;

/** Offline reference hub. Legal documents remain separate and unchanged. */
public class AboutActivity extends Activity {
    private LinearLayout panel;
    private String page="";
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        page=saved==null?"":saved.getString("page", "");
        if (page.isEmpty()) hub(); else open(page);
    }
    @Override protected void onSaveInstanceState(Bundle out) { out.putString("page",page); super.onSaveInstanceState(out); }
    @Override public void onBackPressed() { if(page.isEmpty()) super.onBackPressed(); else hub(); }
    private void layout(String heading, boolean detail) {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(0xFFF2F7F6);
        panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(22),dp(16),dp(22),dp(24));
        scroll.addView(panel); setContentView(scroll); UiInsets.apply(scroll);
        button(detail?"Back to About":"Back to QuietGate",()->{if(detail)hub();else finish();});
        TextView title=text(heading,26,true); title.setPadding(0,dp(20),0,dp(12)); panel.addView(title);
    }
    private void hub() {
        page=""; layout("About "+Edition.NAME,false);
        panel.addView(text("QuietGate helps reduce unwanted Google Meet calls using saved contacts and Meet's notification actions.",16,false));
        TextView intro=text("Free to use, with MIT-licensed project source. Explore its purpose, required and optional access, privacy policy, open-source licensing, and changelog below.",16,false);
        intro.setPadding(0,dp(12),0,dp(12)); panel.addView(intro);
        for(String label: new String[]{"Purpose & limitations","Permissions & optional access","Privacy & data deletion","Background operation & grace timer","Contacts & quiet alerts","Google contacts & automatic refresh","Call catalog & import/export","Incident reports","Free & open-source software","MIT License","Third-party notices & licenses","Changelog","Recent decisions"}) {
            if (!Edition.GOOGLE_CONTACTS && label.equals("Google contacts & automatic refresh")) continue;
            button(label,()->open(label));
        }
        String versionName;
        try { versionName=getPackageManager().getPackageInfo(getPackageName(),0).versionName; }
        catch(android.content.pm.PackageManager.NameNotFoundException e) { versionName="unavailable"; }
        TextView version=text("Version "+versionName+" · Independent software",14,false); version.setPadding(0,dp(16),0,0);panel.addView(version);
    }
    private void open(String label) {
        page=label; layout(label,true);
        if(label.equals("Third-party notices & licenses")) {
            for(String[] item:new String[][]{{"Dependency notices","THIRD_PARTY_NOTICES.txt"},{"Google integration notices","GOOGLE-AUTH-THIRD-PARTY-LICENSES.txt"},{"Shizuku MIT License","SHIZUKU-LICENSE.txt"},{"Apache License 2.0","APACHE-2.0.txt"}}) {
                if (!Edition.GOOGLE_CONTACTS && item[1].startsWith("GOOGLE-")) continue;
                button(item[0],()->document(item[0],item[1]));
            }
            return;
        }
        if(label.equals("Recent decisions")) {
            java.util.List<String> events=GuardLog.recent(this);
            panel.addView(text(events.isEmpty()?"No decisions yet.":android.text.TextUtils.join("\n\n",events),16,false));
            button("Clear recent decisions",()->new android.app.AlertDialog.Builder(this).setTitle("Clear recent decisions?")
                .setMessage("This removes the local decision history. The call catalog is kept."
                    + (Edition.GOOGLE_CONTACTS ? " Connected Google contacts are kept." : ""))
                .setNegativeButton("Cancel",null).setPositiveButton("Clear",(d,w)->{GuardLog.clear(this);open(label);}).show());return;
        }
        String asset;
        switch(label) {
            case "Purpose & limitations": asset="help/PURPOSE.txt"; break;
            case "Permissions & optional access": asset="help/PERMISSIONS.txt"; break;
            case "Privacy & data deletion": asset="help/PRIVACY.txt"; break;
            case "Background operation & grace timer": asset="help/OPERATION.txt"; break;
            case "Contacts & quiet alerts": asset="help/CONTACTS.txt"; break;
            case "Google contacts & automatic refresh": asset="help/GOOGLE.txt"; break;
            case "Call catalog & import/export": asset="help/CATALOG.txt"; break;
            case "Incident reports": asset="help/INCIDENTS.txt"; break;
            case "Free & open-source software": asset="help/FOSS.txt"; break;
            case "MIT License": asset="LICENSE"; break;
            case "Changelog": asset="CHANGELOG.md"; break;
            default: hub(); return;
        }
        showAsset(asset);
    }
    private void document(String title,String asset) { layout(title,true); showAsset(asset); }
    private void showAsset(String name) {
        String body;
        try(java.io.InputStream in=getAssets().open(name);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()) {
            byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))!=-1)out.write(bytes,0,n);
            body=new String(out.toByteArray(),StandardCharsets.UTF_8);
        }catch(java.io.IOException e){body="This document could not be opened. Please consult the documentation included with this release.";}
        TextView view=text(body,16,false); view.setTextIsSelectable(true); panel.addView(view);
    }
    private TextView text(String value,int size,boolean bold) {
        TextView view=new TextView(this);view.setText(value);view.setTextSize(size);view.setTextColor(0xFF163B43);
        view.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL);view.setLineSpacing(dp(3),1f);return view;
    }
    private void button(String label,Runnable action) {
        Button b=new Button(this); b.setText(label);b.setAllCaps(false);b.setTextColor(0xFFFFFFFF);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF17545A)); b.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);
        b.setMinHeight(dp(52));b.setPadding(dp(16),dp(12),dp(16),dp(12));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);panel.addView(b,lp);b.setOnClickListener(v->action.run());
    }
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
