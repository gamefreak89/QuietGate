package org.open.quietgate;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;

/** Synthetic local-cache verification only. Never chooses a real Google account or uses OAuth tokens. */
public final class GoogleContactsVerification extends Instrumentation {
    private int passed,failed;
    private final StringBuilder report=new StringBuilder();
    private Context app;
    private GoogleContactsActivity activity;
    private interface Check {void run()throws Exception;}
    private void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private void check(String name,Check action){try{action.run();passed++;report.append("PASS: ").append(name).append('\n');}
        catch(Throwable e){failed++;report.append("FAIL: ").append(name).append(" — ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append('\n');}}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        app=getTargetContext();boolean owned=false;
        final String a="quietgate-synthetic-a@example.invalid",b="quietgate-synthetic-b@example.invalid",caller="quietgate-synthetic-caller@example.invalid";
        try{
            // Refuse destructive cache checks if this device already has real connections or unreadable prior cache bytes.
            File cache=new File(app.getNoBackupFilesDir(),"google-saved-contacts.enc");
            require(!cache.exists() && GoogleContactStore.entries(app).isEmpty(),"Existing connected-contact data found; refusing to overwrite it");owned=true;
            check("No accounts means no cloud matches",()->require(!GoogleContactStore.matches(app,caller),"Unexpected cloud match"));
            check("Google contacts screen opens with optional/setup disclosure",()->{
                activity=(GoogleContactsActivity)startActivitySync(new Intent(app,GoogleContactsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
                runOnMainSync(()->require(findText(activity.getWindow().getDecorView(),"Google Cloud setup is required"),"Missing setup limitation"));
            });
            check("Only saved-contact read-only OAuth scope is requested",()->{
                Method m=GoogleContactsActivity.class.getDeclaredMethod("scopes");m.setAccessible(true);
                List<?> scopes=(List<?>)m.invoke(activity);require(scopes.size()==1 && scopes.get(0).toString().equals("https://www.googleapis.com/auth/contacts.readonly"),"Unexpected OAuth scope");
            });
            check("Consent disclosure can be canceled without connecting an account",()->{
                runOnMainSync(()->{Button button=findButton(activity.getWindow().getDecorView(),"Connect a Google account");require(button!=null,"Missing connect button");button.performClick();});
                tap("Cancel");require(GoogleContactStore.entries(app).isEmpty(),"Cancel connected an account");
            });
            check("Pending refresh never grants a caller match",()->{GoogleContactStore.begin(app,a);require(!GoogleContactStore.matches(app,caller),"Pending list matched");});
            check("Completed saved email matches ignoring case",()->{
                String gen=GoogleContactStore.begin(app,a);Set<String> hashes=new HashSet<>();hashes.add(GoogleContactStore.fingerprint(caller));hashes.add(GoogleContactStore.fingerprint("+1 (202) 555-0199"));
                require(GoogleContactStore.complete(app,a,gen,hashes),"Completion rejected");require(GoogleContactStore.matches(app," QUIETGATE-SYNTHETIC-CALLER@EXAMPLE.INVALID "),"Email case/trim mismatch");
            });
            check("Phone punctuation normalizes but suffix-only numbers do not match",()->{
                require(GoogleContactStore.matches(app,"+1 202-555-0199"),"Phone punctuation mismatch");require(!GoogleContactStore.matches(app,"5550199"),"Unsafe suffix match");
            });
            check("ContactMatcher uses completed local Google cache as fallback",()->require(ContactMatcher.isSavedContact(app,caller),"Integrated matcher missed cached contact"));
            check("Unlisted and malformed identifiers do not match",()->{
                require(!GoogleContactStore.matches(app,"unlisted-synthetic@example.invalid"),"Unknown caller matched");require(!GoogleContactStore.matches(app,"+() -"),"Empty phone matched");require(GoogleContactStore.normalized("not a phone")==null,"Letters accepted as phone");
            });
            check("Connected cache is encrypted in no-backup storage",()->{
                require(cache.isFile() && cache.length()>29,"Missing encrypted cache");byte[] bytes=read(cache);
                String raw=new String(bytes,StandardCharsets.UTF_8);require(!raw.contains(a) && !raw.contains(caller) && !raw.contains("202"),"Plaintext contact/account content in cache");
                require(bytes[0]==1,"Unexpected cache envelope");
            });
            check("Cache decrypts after clearing in-process state and key handles",()->{
                resetMemory();require(GoogleContactStore.matches(app,caller),"Persisted encrypted cache could not reload");require(GoogleContactStore.entries(app).get(0).fresh(),"Fresh status lost");
            });
            check("Last success time is recorded",()->{long updated=GoogleContactStore.entries(app).get(0).updated;require(updated>0 && Math.abs(System.currentTimeMillis()-updated)<60000,"Invalid last success time");});
            check("A refresh immediately pauses previous cloud matches",()->{GoogleContactStore.begin(app,a);require(!GoogleContactStore.matches(app,caller),"Old match survived refresh start");});
            check("Failed refresh remains paused and retains no active hashes",()->{
                String gen=GoogleContactStore.begin(app,a);GoogleContactStore.failed(app,a,gen);require(!GoogleContactStore.matches(app,caller) && GoogleContactStore.entries(app).get(0).hashes.isEmpty(),"Failed refresh retained matches");
            });
            check("Older authorization generation cannot activate or replace newer state",()->{
                String old=GoogleContactStore.begin(app,a),current=GoogleContactStore.begin(app,a);Set<String> hashes=Collections.singleton(GoogleContactStore.fingerprint(caller));
                require(!GoogleContactStore.complete(app,a,old,hashes),"Stale completion accepted");require(GoogleContactStore.complete(app,a,current,hashes),"Current completion rejected");
            });
            check("24-hour expired snapshot and future-dated snapshot cannot match",()->{
                Field f=GoogleContactStore.class.getDeclaredField("state");f.setAccessible(true);
                @SuppressWarnings("unchecked") Map<String,GoogleContactStore.Entry> map=(Map<String,GoogleContactStore.Entry>)f.get(null);
                GoogleContactStore.Entry e=map.get(a);
                map.put(a,new GoogleContactStore.Entry(a,e.generation,"ready",System.currentTimeMillis()-GoogleContactStore.FRESH_MS-1,e.hashes));
                require(!GoogleContactStore.matches(app,caller),"Expired match allowed");
                map.put(a,new GoogleContactStore.Entry(a,e.generation,"ready",System.currentTimeMillis()+60000,e.hashes));require(!GoogleContactStore.matches(app,caller),"Future-dated match allowed");
            });
            check("Multiple selected account snapshots contribute independently",()->{
                String ag=GoogleContactStore.begin(app,a),bg=GoogleContactStore.begin(app,b);
                GoogleContactStore.complete(app,a,ag,Collections.singleton(GoogleContactStore.fingerprint(caller)));
                GoogleContactStore.complete(app,b,bg,Collections.singleton(GoogleContactStore.fingerprint("second-synthetic@example.invalid")));
                require(GoogleContactStore.matches(app,caller) && GoogleContactStore.matches(app,"second-synthetic@example.invalid"),"Account union failed");
                GoogleContactStore.remove(app,a);require(!GoogleContactStore.matches(app,caller) && GoogleContactStore.matches(app,"second-synthetic@example.invalid"),"Disconnect affected wrong account");
            });
            check("Disconnect invalidates in-flight fetch before any HTTP request",()->{
                String gen=GoogleContactStore.begin(app,a);GoogleContactStore.remove(app,a);
                require(!GoogleContactStore.complete(app,a,gen,Collections.singleton(GoogleContactStore.fingerprint(caller))),"Removed account revived");
                boolean rejected=false;try{GoogleContactsSync.fetch(app,a,gen,"synthetic-non-token");}catch(IOException expected){rejected=true;}require(rejected,"Removed connection fetch accepted");
            });
            check("Partial or oversized identifier sets are rejected without activation",()->{
                String gen=GoogleContactStore.begin(app,a);Set<String> many=new HashSet<>();for(int i=0;i<=GoogleContactStore.MAX_IDENTIFIERS;i++)many.add("synthetic-hash-"+i);
                boolean rejected=false;try{GoogleContactStore.complete(app,a,gen,many);}catch(IOException expected){rejected=true;}
                require(rejected && !GoogleContactStore.matches(app,caller),"Oversized partial list activated");
            });
            check("Ten-account bound is enforced",()->{
                GoogleContactStore.clear(app);for(int i=0;i<GoogleContactStore.MAX_ACCOUNTS;i++)GoogleContactStore.begin(app,"synthetic-"+i+"@example.invalid");
                boolean rejected=false;try{GoogleContactStore.begin(app,"synthetic-overflow@example.invalid");}catch(IOException expected){rejected=true;}require(rejected && GoogleContactStore.entries(app).size()==10,"Account cap failed");
            });
            check("Call catalog export excludes connected accounts and contact cache",()->{
                String export=CallCatalog.exportJson(app);require(!export.contains("synthetic-0@example.invalid") && !export.contains("google-saved-contacts") && !export.contains("quietgate.google.saved"),"Connected data leaked to catalog export");
            });
            check("Tampered encrypted cache is rejected and cannot allow matching",()->{
                byte[] bytes=read(cache);bytes[bytes.length-1]^=1;try(FileOutputStream out=new FileOutputStream(cache)){out.write(bytes);}resetMemory();
                boolean rejected=false;try{GoogleContactStore.entries(app);}catch(Exception expected){rejected=true;}
                require(rejected && !ContactMatcher.isSavedContact(app,caller),"Corrupted cache trusted");
            });
            check("Delete-all removes cache and both Keystore keys, even after corruption",()->{
                GoogleContactStore.clear(app);require(!cache.exists() && GoogleContactStore.entries(app).isEmpty(),"Cache not deleted");
                KeyStore keys=KeyStore.getInstance("AndroidKeyStore");keys.load(null);require(!keys.containsAlias("quietgate.google.saved.aes.v1") && !keys.containsAlias("quietgate.google.saved.hmac.v1"),"Keys retained");
            });
            check("Google third-party license asset is bundled",()->{try(InputStream in=app.getAssets().open("GOOGLE-AUTH-THIRD-PARTY-LICENSES.txt")){require(in.read()!=-1,"License asset empty");}});
        }catch(Throwable e){failed++;report.append("FAIL: setup/execution: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append('\n');}
        finally{
            if(activity!=null)runOnMainSync(()->activity.finish());
            if(owned)try{GoogleContactStore.clear(app);}catch(Exception e){failed++;report.append("FAIL: synthetic cache cleanup\n");}
        }
        Bundle results=new Bundle();results.putString("stream",report.toString()+"\n"+passed+" passed; "+failed+" failed. Synthetic account data/keys removed. No real Google account authorized.\n");finish(failed==0?Activity.RESULT_OK:Activity.RESULT_CANCELED,results);
    }
    private void resetMemory()throws Exception{for(String name:new String[]{"state","encryptionKey","hashKey"}){Field f=GoogleContactStore.class.getDeclaredField(name);f.setAccessible(true);f.set(null,null);}}
    private byte[] read(File file)throws Exception{try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
    private boolean findText(View v,String text){if(v instanceof TextView && ((TextView)v).getText().toString().contains(text))return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(findText(g.getChildAt(i),text))return true;}return false;}
    private Button findButton(View v,String text){if(v instanceof Button && ((Button)v).getText().toString().equals(text))return(Button)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){Button b=findButton(g.getChildAt(i),text);if(b!=null)return b;}}return null;}
    private AccessibilityNodeInfo findNode(AccessibilityNodeInfo n,String text){if(n==null)return null;if(n.getText()!=null && n.getText().toString().equalsIgnoreCase(text))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo result=findNode(n.getChild(i),text);if(result!=null)return result;}return null;}
    private void tap(String text)throws Exception{long deadline=System.currentTimeMillis()+8000;while(System.currentTimeMillis()<deadline){AccessibilityNodeInfo n=findNode(getUiAutomation().getRootInActiveWindow(),text);if(n!=null){while(n!=null && !n.isClickable())n=n.getParent();require(n!=null && n.performAction(AccessibilityNodeInfo.ACTION_CLICK),"UI click failed");waitForIdleSync();return;}Thread.sleep(100);}throw new IOException("UI control missing: "+text);}
}
