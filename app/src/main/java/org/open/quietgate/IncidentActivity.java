package org.open.quietgate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.util.*;

/** A user-started report, with no additional collection permissions. */
public class IncidentActivity extends Activity {
    private static final int PICK=41,SAVE=42;
    private final Set<String> selected=new HashSet<>();
    private final ArrayList<IncidentExport.Attachment> attachments=new ArrayList<>();
    private EditText title,notes;
    private LinearLayout attachmentRows;
    private TextView count;
    private Button export;
    private static final java.util.concurrent.atomic.AtomicBoolean EXPORTING=new java.util.concurrent.atomic.AtomicBoolean();
    private final android.os.Handler uiHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable refreshExport=new Runnable(){public void run(){refreshCount();if(EXPORTING.get())uiHandler.postDelayed(this,500);}};
    @Override protected void onResume(){super.onResume();refreshCount();if(EXPORTING.get())uiHandler.post(refreshExport);}
    @Override protected void onPause(){uiHandler.removeCallbacks(refreshExport);super.onPause();}
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        JSONArray records=CallCatalog.recent(this);
        if(state!=null) {
            ArrayList<String> ids=state.getStringArrayList("selected");if(ids!=null)selected.addAll(ids);
            ArrayList<String> uris=state.getStringArrayList("uris"),names=state.getStringArrayList("names");
            if(uris!=null && names!=null)for(int i=0;i<Math.min(uris.size(),names.size());i++)
                attachments.add(new IncidentExport.Attachment(Uri.parse(uris.get(i)),names.get(i)));
        }
        Set<String> available=new HashSet<>();
        for(int i=0;i<records.length();i++)available.add(records.optJSONObject(i).optString("id"));
        selected.retainAll(available);
        ScrollView scroll=new ScrollView(this);LinearLayout panel=new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);int p=(int)(20*getResources().getDisplayMetrics().density);
        panel.setPadding(p,p,p,p);scroll.addView(panel);setContentView(scroll);
        UiInsets.apply(scroll);
        label(panel,"Create incident report",24);
        label(panel,"Choose attempts to include. Identities and call outcomes are unverified; imported entries remain labeled. The ZIP contains personal data and is not encrypted. Choosing cloud storage may upload it through that provider.",15);
        title=new EditText(this);title.setHint("Report title");title.setSingleLine(true);
        title.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(160)});panel.addView(title);
        notes=new EditText(this);notes.setHint("Incident notes (up to 4,000 characters)");
        notes.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        notes.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4000)});panel.addView(notes);
        if(state!=null){title.setText(state.getString("title",""));notes.setText(state.getString("notes",""));}
        count=label(panel,"",16);export=button(panel,"Review and save ZIP",this::review);
        button(panel,"Add attachments",()->{
            if(attachments.size()>=IncidentExport.MAX_ATTACHMENTS){notice("Maximum 10 attachments.");return;}
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*")
                .addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true),PICK);
        });
        label(panel,"Optional: screenshots or other existing files. Up to 10 files, 10 MiB each, 25 MiB total. Original bytes and embedded metadata are included unchanged. Attachments belong to this report as a whole.",14);
        attachmentRows=new LinearLayout(this);attachmentRows.setOrientation(LinearLayout.VERTICAL);panel.addView(attachmentRows);refreshAttachments();
        label(panel,"Select attempts",20);
        if(records.length()==0)label(panel,"The retained catalog is empty.",16);
        for(int i=0;i<records.length();i++) {
            JSONObject e=records.optJSONObject(i);if(e==null)continue;String id=e.optString("id");CheckBox row=new CheckBox(this);
            row.setText(IncidentExport.utc(e.optLong("time"))+"\n"+e.optString("identity")+"\n"
                +e.optString("category")+" · "+(e.optBoolean("imported")?"Imported claim":"This device")+"\n"+e.optString("action")
                +(e.optString("notes").isEmpty()?"":"\n"+e.optString("notes")));
            row.setChecked(selected.contains(id));
            row.setOnCheckedChangeListener((v,checked)->{if(checked)selected.add(id);else selected.remove(id);refreshCount();});panel.addView(row);
        }
        refreshCount();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);state.putStringArrayList("selected",new ArrayList<>(selected));
        state.putString("title",title.getText().toString());state.putString("notes",notes.getText().toString());
        ArrayList<String> uris=new ArrayList<>(),names=new ArrayList<>();
        for(IncidentExport.Attachment a:attachments){uris.add(a.uri.toString());names.add(a.name);}
        state.putStringArrayList("uris",uris);state.putStringArrayList("names",names);
    }
    private JSONArray chosen() {
        JSONArray result=new JSONArray(),current=CallCatalog.recent(this);
        // Honor catalog clearing, expiry and edits while the document picker was open.
        for(int i=0;i<current.length();i++){JSONObject e=current.optJSONObject(i);if(e!=null && selected.contains(e.optString("id")))result.put(e);}return result;
    }
    private void review() {
        if(EXPORTING.get())return;int n=chosen().length();if(n==0){notice("Select at least one retained attempt.");return;}
        new AlertDialog.Builder(this).setTitle("Save incident report?")
            .setMessage(n+" attempts, "+attachments.size()+" attachments. Includes selected caller identities, categories, notes, UTC timestamps and SHA-256 hashes.\n\n"+IncidentExport.LIMITATIONS)
            .setNegativeButton("Back",null).setPositiveButton("Choose destination",(d,w)->startActivityForResult(
                new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/zip").addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_TITLE,"QuietGate-incident-"+System.currentTimeMillis()+".zip"),SAVE)).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);if(result!=RESULT_OK || data==null)return;
        if(request==PICK) {
            if(data.getClipData()!=null)for(int i=0;i<data.getClipData().getItemCount();i++)addAttachment(data.getClipData().getItemAt(i).getUri());
            else if(data.getData()!=null)addAttachment(data.getData());
            refreshAttachments();refreshCount();
        }else if(request==SAVE && data.getData()!=null)save(data.getData());
    }
    private void addAttachment(Uri uri) {
        if(attachments.size()>=IncidentExport.MAX_ATTACHMENTS)return;
        for(IncidentExport.Attachment a:attachments)if(a.uri.equals(uri))return;
        String name="Attachment "+(attachments.size()+1);
        try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null)) {
            if(c!=null && c.moveToFirst()) {
                int n=c.getColumnIndex(OpenableColumns.DISPLAY_NAME),s=c.getColumnIndex(OpenableColumns.SIZE);
                if(n>=0 && !c.isNull(n))name=c.getString(n);
                if(s>=0 && !c.isNull(s) && c.getLong(s)>IncidentExport.MAX_FILE_BYTES){notice("Skipped attachment larger than 10 MiB.");return;}
            }
        }catch(RuntimeException e){notice("Could not read attachment details.");return;}
        name=name.replaceAll("[\\p{Cntrl}]"," ");if(name.length()>200)name=name.substring(0,200);
        attachments.add(new IncidentExport.Attachment(uri,name));
    }
    private void refreshAttachments() {
        attachmentRows.removeAllViews();for(IncidentExport.Attachment a:new ArrayList<>(attachments))
            button(attachmentRows,"Remove: "+a.name,()->{attachments.remove(a);refreshAttachments();refreshCount();});
    }
    private void refreshCount(){count.setText(selected.size()+" attempts selected · "+attachments.size()+" attachments");export.setEnabled(!EXPORTING.get() && !selected.isEmpty());export.setText(EXPORTING.get()?"Saving report…":"Review and save ZIP");}
    private void save(Uri destination) {
        if(EXPORTING.get())return;final JSONArray snapshot=chosen();
        if(snapshot.length()==0){notice("Selected attempts are no longer available. Nothing exported.");return;}
        final String reportTitle=title.getText().toString().trim(),reportNotes=notes.getText().toString();
        final ArrayList<IncidentExport.Attachment> files=new ArrayList<>(attachments);
        if(!EXPORTING.compareAndSet(false,true))return;
        refreshCount();uiHandler.post(refreshExport);final android.content.Context app=getApplicationContext();
        new Thread(()->{
            File temp=null;String message;boolean writingDestination=false;
            try {
                temp=File.createTempFile("incident-",".zip",app.getCacheDir());
                String version=app.getPackageManager().getPackageInfo(app.getPackageName(),0).versionName;
                IncidentExport.write(temp,app.getContentResolver(),version,reportTitle,reportNotes,snapshot,files);
                writingDestination=true;
                try(InputStream in=new FileInputStream(temp);OutputStream out=app.getContentResolver().openOutputStream(destination,"wt")) {
                    if(out==null)throw new IOException("Destination unavailable.");byte[] buffer=new byte[16384];int n;
                    while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
                }
                message="Incident ZIP saved: "+snapshot.length()+" attempts, attachments, report.txt, report.json and SHA256SUMS.txt. Keep a separate copy of the manifest for later comparison.";
            }catch(Exception e) {
                if(writingDestination)try{android.provider.DocumentsContract.deleteDocument(app.getContentResolver(),destination);}catch(Exception ignored){}
                message="Export failed. Check attachment access and limits (10 MiB per file / 25 MiB total) and destination space. The catalog is unchanged. Delete any empty or partial destination file before retrying.";
            }finally{if(temp!=null)temp.delete();EXPORTING.set(false);}
            final String result=message;runOnUiThread(()->{
                if(isFinishing() || isDestroyed()){Toast.makeText(app,result,Toast.LENGTH_LONG).show();return;}
                refreshCount();notice(result);
            });
        },"QuietGate-incident-export").start();
    }
    private void notice(String message){new AlertDialog.Builder(this).setMessage(message).setPositiveButton("OK",null).show();}
    private TextView label(LinearLayout panel,String text,int size){TextView v=new TextView(this);v.setText(text);v.setTextSize(size);v.setPadding(0,12,0,12);panel.addView(v);return v;}
    private Button button(LinearLayout panel,String text,Runnable action){Button b=new Button(this);b.setText(text);b.setAllCaps(false);panel.addView(b);b.setOnClickListener(v->action.run());return b;}
}
