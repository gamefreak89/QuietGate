package org.open.quietgate;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import android.view.*;
import org.json.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Synthetic incident checks. Restores the original catalog in finally. */
public final class IncidentVerification extends Instrumentation {
    private int passed,failed;
    private final StringBuilder report=new StringBuilder();
    private Context app;
    private File folder;
    private IncidentActivity activity;
    private interface Check {void run()throws Exception;}
    private void check(String name,Check action){try{action.run();passed++;report.append("PASS: ").append(name).append('\n');}
        catch(Throwable e){failed++;report.append("FAIL: ").append(name).append(" — ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append('\n');}}
    private void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        app=getTargetContext();SharedPreferences prefs=app.getSharedPreferences(LeaseManager.PREFS,0);
        String original=prefs.getString(CallCatalog.KEY,null);
        folder=new File(app.getCacheDir(),"incident-verification-"+UUID.randomUUID());folder.mkdirs();
        try{
            long now=System.currentTimeMillis();
            JSONArray fixture=new JSONArray().put(record("incident-test-local",now,false)).put(record("incident-test-imported",now-1000,true));
            prefs.edit().putString(CallCatalog.KEY,fixture.toString()).commit();
            activity=(IncidentActivity)startActivitySync(new Intent(app,IncidentActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            check("Report screen opens with export disabled until selection",()->runOnMainSync(()->require(!((Button)field("export")).isEnabled(),"Export enabled without selection")));
            check("Checkbox selects only the requested imported record",()->{
                runOnMainSync(()->{CheckBox box=findCheckbox(activity.getWindow().getDecorView(),"incident-test-imported@example.invalid");require(box!=null,"Missing synthetic checkbox");box.performClick();});
                JSONArray[] selected={null};runOnMainSync(()->selected[0]=(JSONArray)invoke("chosen",new Class<?>[0]));
                require(selected[0].length()==1 && selected[0].getJSONObject(0).optBoolean("imported"),"Wrong record selection");
            });
            File attachment=new File(folder,"attachment.txt");
            byte[] payload="Synthetic attachment\nUnicode: café\n".getBytes(StandardCharsets.UTF_8);
            try(FileOutputStream out=new FileOutputStream(attachment)){out.write(payload);}
            check("Attachment picker result adds a selected file",()->runOnMainSync(()->{
                invoke("onActivityResult",new Class<?>[]{int.class,int.class,Intent.class},41,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(attachment)));
                require(((List<?>)field("attachments")).size()==1,"Attachment not added");
            }));
            check("Duplicate attachment selection is ignored",()->runOnMainSync(()->{
                invoke("onActivityResult",new Class<?>[]{int.class,int.class,Intent.class},41,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(attachment)));
                require(((List<?>)field("attachments")).size()==1,"Duplicate attachment added");
            }));
            File exported=new File(folder,"ui-report.zip");
            check("Activity export handler saves ZIP in background",()->{
                runOnMainSync(()->{
                    ((EditText)field("title")).setText("Synthetic device verification");
                    ((EditText)field("notes")).setText("Not a real incident.");
                    invoke("onActivityResult",new Class<?>[]{int.class,int.class,Intent.class},42,Activity.RESULT_OK,new Intent().setData(Uri.fromFile(exported)));
                });
                long deadline=System.currentTimeMillis()+15000;
                Field lock=IncidentActivity.class.getDeclaredField("EXPORTING");lock.setAccessible(true);
                while(((java.util.concurrent.atomic.AtomicBoolean)lock.get(null)).get() && System.currentTimeMillis()<deadline)Thread.sleep(50);
                require(exported.isFile() && exported.length()>0,"Missing ZIP");
                require(!((java.util.concurrent.atomic.AtomicBoolean)lock.get(null)).get(),"Export did not finish");waitForIdleSync();
            });
            check("ZIP preserves selected record, imported label, notes, UTC and epoch time",()->{
                try(ZipFile zip=new ZipFile(exported)){
                    JSONObject r=new JSONObject(new String(read(zip,"report.json"),StandardCharsets.UTF_8));
                    JSONArray entries=r.getJSONArray("records");require(entries.length()==1,"Unselected records exported");
                    JSONObject e=entries.getJSONObject(0);require(e.getString("id").equals("incident-test-imported") && e.getBoolean("imported"),"Imported provenance lost");
                    require(e.getLong("time")==now-1000 && e.getString("timeUtc").endsWith("Z"),"Timestamp lost");
                    require(e.getString("notes").equals("Synthetic record notes") && r.getString("userNotes").equals("Not a real incident."),"Notes lost");
                    require(r.getString("title").equals("Synthetic device verification"),"Title lost");
                    require(new String(read(zip,"report.txt"),StandardCharsets.UTF_8).contains("Imported: true"),"Readable provenance missing");
                }
            });
            check("Attachment bytes are unchanged and every ZIP payload hash matches",()->{
                try(ZipFile zip=new ZipFile(exported)){
                    require(Arrays.equals(payload,read(zip,"attachments/01.bin")),"Attachment changed");
                    String manifest=new String(read(zip,"SHA256SUMS.txt"),StandardCharsets.UTF_8);Set<String> covered=new HashSet<>();
                    for(String line:manifest.trim().split("\n")){String[] bits=line.split("  ",2);require(bits[0].equals(hex(MessageDigest.getInstance("SHA-256").digest(read(zip,bits[1])))),"Hash mismatch");covered.add(bits[1]);}
                    Enumeration<? extends ZipEntry> files=zip.entries();while(files.hasMoreElements()){String name=files.nextElement().getName();require(name.equals("SHA256SUMS.txt") || covered.contains(name),"Unhashed payload");}
                    require(!covered.contains("SHA256SUMS.txt") && covered.size()==4,"Unexpected checksum coverage");
                }
            });
            check("Unsafe original filenames cannot create archive traversal paths",()->{
                File out=new File(folder,"safe-paths.zip");IncidentExport.write(out,app.getContentResolver(),"0.8-alpha","","",fixture,
                    Collections.singletonList(new IncidentExport.Attachment(Uri.fromFile(attachment),"../../outside.txt")));
                try(ZipFile zip=new ZipFile(out)){Enumeration<? extends ZipEntry> files=zip.entries();while(files.hasMoreElements())require(!files.nextElement().getName().contains(".."),"Unsafe ZIP path");}
            });
            check("Empty record export is rejected",()->expectFailure(new JSONArray(),Collections.emptyList()));
            check("More than ten attachments is rejected",()->{List<IncidentExport.Attachment> files=new ArrayList<>();for(int i=0;i<11;i++)files.add(new IncidentExport.Attachment(Uri.fromFile(attachment),"test"));expectFailure(fixture,files);});
            check("Unreadable attachment fails instead of reporting success",()->expectFailure(fixture,Collections.singletonList(new IncidentExport.Attachment(Uri.fromFile(new File(folder,"missing")),"missing"))));
            check("Per-file 10 MiB limit is enforced on streamed bytes",()->{
                File large=new File(folder,"too-large.bin");try(RandomAccessFile f=new RandomAccessFile(large,"rw")){f.setLength(IncidentExport.MAX_FILE_BYTES+1);}
                expectFailure(fixture,Collections.singletonList(new IncidentExport.Attachment(Uri.fromFile(large),"large")));large.delete();
            });
            check("Total 25 MiB attachment limit is enforced",()->{
                File large=new File(folder,"nine-mib.bin");try(RandomAccessFile f=new RandomAccessFile(large,"rw")){f.setLength(9L*1024*1024);}
                List<IncidentExport.Attachment> files=new ArrayList<>();for(int i=0;i<3;i++)files.add(new IncidentExport.Attachment(Uri.fromFile(large),"nine-mib"));expectFailure(fixture,files);large.delete();
            });
            check("Android system file picker saves a valid incident ZIP",()->{
                String filename="QuietGate-synthetic-check-"+UUID.randomUUID()+".zip";
                String devicePath="/sdcard/Download/"+filename;
                try {
                    tapText("OK");
                    runOnMainSync(()->invoke("review",new Class<?>[0]));
                    tapText("Choose destination");
                    android.view.accessibility.AccessibilityNodeInfo editor=waitNode(null,"android.widget.EditText");
                    Bundle value=new Bundle();value.putCharSequence(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,filename);
                    require(editor.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,value),"Could not enter ZIP filename");
                    tapText("Save");
                    waitNode("Incident ZIP saved:",null);
                    File saved=new File(folder,"system-picker.zip");
                    try(android.os.ParcelFileDescriptor p=getUiAutomation().executeShellCommand("cat "+devicePath);
                        InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(p);
                        FileOutputStream out=new FileOutputStream(saved)) {
                        byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);
                    }
                    try(ZipFile zip=new ZipFile(saved)){
                        JSONObject r=new JSONObject(new String(read(zip,"report.json"),StandardCharsets.UTF_8));
                        require(r.getJSONArray("records").length()==1,"Wrong system-picker export");
                        require(Arrays.equals(payload,read(zip,"attachments/01.bin")),"System-picker attachment bytes changed");
                    }
                } finally {
                    // This exact UUID-named synthetic file was created by this check only.
                    try(android.os.ParcelFileDescriptor p=getUiAutomation().executeShellCommand("rm -f "+devicePath);
                        InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(p)){while(in.read()!=-1){}}
                }
            });
            check("Cleared records are excluded when export selection is resolved",()->{
                prefs.edit().putString(CallCatalog.KEY,"[]").commit();JSONArray[] selected={null};runOnMainSync(()->selected[0]=(JSONArray)invoke("chosen",new Class<?>[0]));require(selected[0].length()==0,"Cleared record retained");
            });
            check("Catalog JSON round-trip and duplicate import preserve edits",()->{
                prefs.edit().putString(CallCatalog.KEY,fixture.toString()).commit();String json=CallCatalog.exportJson(app);
                prefs.edit().remove(CallCatalog.KEY).commit();CallCatalog.merge(app,json);require(CallCatalog.recent(app).length()==2,"Round-trip count");
                CallCatalog.edit(app,"incident-test-imported","Other","Edited synthetic notes");CallCatalog.merge(app,json);
                JSONArray records=CallCatalog.recent(app);require(records.length()==2,"Duplicates added");boolean found=false;
                for(int i=0;i<records.length();i++)if(records.getJSONObject(i).getString("id").equals("incident-test-imported"))found=records.getJSONObject(i).getString("notes").equals("Edited synthetic notes");
                require(found,"Existing edits overwritten");
            });
        }catch(Throwable e){failed++;report.append("FAIL: setup/execution: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append('\n');}
        finally{
            if(activity!=null)runOnMainSync(()->activity.finish());
            SharedPreferences.Editor edit=prefs.edit();if(original==null)edit.remove(CallCatalog.KEY);else edit.putString(CallCatalog.KEY,original);edit.commit();
            require(Objects.equals(original,prefs.getString(CallCatalog.KEY,null)),"Original catalog restoration failed");
            File[] files=folder.listFiles();if(files!=null)for(File f:files)f.delete();folder.delete();
        }
        Bundle result=new Bundle();result.putString("stream",report.toString()+"\n"+passed+" passed; "+failed+" failed. Original catalog restored.\n");finish(failed==0?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }
    private JSONObject record(String id,long time,boolean imported)throws Exception{return new JSONObject().put("id",id).put("time",time).put("identity",id+"@example.invalid").put("source",imported?"Imported catalog; claims not verified":"Synthetic test fixture").put("action","Decline sent; outcome unconfirmed").put("category","Suspected spam").put("notes","Synthetic record notes").put("imported",imported);}
    private Object field(String name){try{Field f=IncidentActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception e){throw new RuntimeException(e);}}
    private Object invoke(String name,Class<?>[] types,Object...args){try{Method m=IncidentActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(activity,args);}catch(Exception e){throw new RuntimeException(e);}}
    private CheckBox findCheckbox(View v,String text){if(v instanceof CheckBox && ((CheckBox)v).getText().toString().contains(text))return(CheckBox)v;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++){CheckBox found=findCheckbox(g.getChildAt(i),text);if(found!=null)return found;}}return null;}
    private android.view.accessibility.AccessibilityNodeInfo findNode(android.view.accessibility.AccessibilityNodeInfo node,String text,String cls){
        if(node==null)return null;
        if((text==null || (node.getText()!=null && (text.endsWith(":")
            ?node.getText().toString().toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT))
            :node.getText().toString().equalsIgnoreCase(text))))
            && (cls==null || (cls.contentEquals(node.getClassName()) && node.getPackageName()!=null
                && node.getPackageName().toString().contains("documentsui"))))return node;
        for(int i=0;i<node.getChildCount();i++){android.view.accessibility.AccessibilityNodeInfo found=findNode(node.getChild(i),text,cls);if(found!=null)return found;}return null;
    }
    private android.view.accessibility.AccessibilityNodeInfo waitNode(String text,String cls)throws Exception{
        long deadline=System.currentTimeMillis()+12000;
        while(System.currentTimeMillis()<deadline){android.view.accessibility.AccessibilityNodeInfo found=findNode(getUiAutomation().getRootInActiveWindow(),text,cls);if(found!=null)return found;Thread.sleep(100);}
        throw new IOException("Expected UI control not found: "+(text==null?cls:text));
    }
    private void tapText(String text)throws Exception{
        android.view.accessibility.AccessibilityNodeInfo node=waitNode(text,null);
        while(node!=null && !node.isClickable())node=node.getParent();
        require(node!=null && node.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK),"Could not click "+text);waitForIdleSync();
    }
    private void expectFailure(JSONArray records,List<IncidentExport.Attachment> files)throws Exception{boolean failed=false;try{IncidentExport.write(new File(folder,"rejected.zip"),app.getContentResolver(),"0.8-alpha","","",records,files);}catch(IOException expected){failed=true;}require(failed,"Invalid input accepted");}
    private static byte[] read(ZipFile zip,String name)throws Exception{try(InputStream in=zip.getInputStream(zip.getEntry(name));ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
    private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.US,"%02x",v&255));return s.toString();}
}
