package org.open.quietgate;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Non-destructive to existing connections: synthetic row is individually removed, never clear-all. */
public final class AutoRefreshVerification extends Instrumentation {
    private Context app;private Bundle args;private int passed,failed;private final StringBuilder report=new StringBuilder();
    private GoogleContactsActivity activity;
    private interface Check{void run()throws Exception;}
    private void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private void check(String name,Check test){try{test.run();passed++;report.append("PASS: ").append(name).append('\n');}catch(Throwable e){failed++;report.append("FAIL: ").append(name).append(" — ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append('\n');}}
    @Override public void onCreate(Bundle b){super.onCreate(b);args=b;start();}
    @Override public void onStart(){app=getTargetContext();String mode=args==null?"local":args.getString("mode","local");try{if("enable".equals(mode))enable();else if("status".equals(mode))status(false);else if("verify".equals(mode))status(true);else local();}catch(Throwable e){failed++;report.append("FAIL: diagnostic execution: ").append(e.getClass().getSimpleName()).append('\n');}
        Bundle result=new Bundle();result.putString("stream",report.toString()+"\nChecks passed: "+passed+"; failed: "+failed+".\n");finish(failed==0?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
    private GoogleContactStore.Entry find(String account)throws Exception{for(GoogleContactStore.Entry e:GoogleContactStore.entries(app))if(e.account.equals(account))return e;throw new IOException("Fixture missing");}
    private void local()throws Exception{
        String account="quietgate-auto-fixture-"+UUID.randomUUID()+"@example.invalid",caller="quietgate-auto-caller@example.invalid";
        List<GoogleContactStore.Entry> originals=GoogleContactStore.entries(app);
        try{
            check("Migration preserves original encrypted-backup hashes, dates and connection state",()->verifyBackup(originals));
            check("Existing account snapshots remain opted out until enabled",()->{for(GoogleContactStore.Entry e:originals)require(!e.autoRefresh,"Unexpected auto opt-in");});
            String initial=GoogleContactStore.begin(app,account);GoogleContactStore.complete(app,account,initial,Collections.singleton(GoogleContactStore.fingerprint(caller)));
            check("New account defaults to auto off and Wi-Fi only",()->{GoogleContactStore.Entry e=find(account);require(!e.autoRefresh && e.wifiOnly,"Unsafe defaults");});
            check("Auto UI disclosure can be canceled without enabling jobs",()->{
                activity=(GoogleContactsActivity)startActivitySync(new Intent(app,GoogleContactsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
                runOnMainSync(()->{try{java.lang.reflect.Field f=GoogleContactsActivity.class.getDeclaredField("panel");f.setAccessible(true);LinearLayout panel=(LinearLayout)f.get(activity);boolean row=false;Switch target=null;
                    for(int i=0;i<panel.getChildCount();i++){View v=panel.getChildAt(i);if(v instanceof TextView && ((TextView)v).getText().toString().startsWith(account+"\n"))row=true;else if(row && v instanceof Switch){target=(Switch)v;break;}}
                    require(target!=null,"Fixture auto switch missing");target.performClick();
                }catch(Exception e){throw new RuntimeException(e);}});tap("Cancel");require(!find(account).autoRefresh,"Cancel enabled auto refresh");
            });
            GoogleContactStore.Entry enabled=GoogleContactStore.configureAuto(app,account,true,true);
            check("Periodic request is persisted at 12h with 1h flex and battery guard",()->{JobInfo j=GoogleAutoRefresh.build(app,enabled,false);require(j.isPeriodic() && j.isPersisted() && j.getIntervalMillis()==TimeUnit.HOURS.toMillis(12) && j.getFlexMillis()==TimeUnit.HOURS.toMillis(1) && j.isRequireBatteryNotLow(),"Incorrect periodic policy");});
            check("Job extras contain an opaque reference, not an account or token",()->{PersistableBundle e=GoogleAutoRefresh.build(app,enabled,false).getExtras();require(e.size()==1 && enabled.scheduleKey.equals(e.getString(GoogleAutoRefresh.REF)) && !e.toString().contains(account),"Sensitive scheduler extras");});
            check("Wi-Fi policy requests unmetered network; mobile option requests any",()->{
                require(GoogleAutoRefresh.build(app,enabled,false).getNetworkType()==JobInfo.NETWORK_TYPE_UNMETERED,"Wrong Wi-Fi constraint");
                GoogleContactStore.Entry any=GoogleContactStore.configureAuto(app,account,true,false);require(GoogleAutoRefresh.build(app,any,false).getNetworkType()==JobInfo.NETWORK_TYPE_ANY,"Wrong any-network constraint");GoogleContactStore.configureAuto(app,account,true,true);
            });
            check("Register/reconcile keeps a single periodic account job",()->{GoogleContactStore.Entry e=find(account);GoogleAutoRefresh.schedule(app,e,false);GoogleAutoRefresh.reconcile(app);long n=app.getSystemService(JobScheduler.class).getAllPendingJobs().stream().filter(j->j.getId()==e.jobId).count();require(n==1,"Duplicate/missing periodic job");});
            check("Options and opaque job identity survive encrypted reload",()->{GoogleContactStore.Entry before=find(account);java.lang.reflect.Field state=GoogleContactStore.class.getDeclaredField("state");state.setAccessible(true);state.set(null,null);GoogleContactStore.Entry after=find(account);require(after.autoRefresh && after.wifiOnly && after.jobId==before.jobId && after.scheduleKey.equals(before.scheduleKey),"Options lost on reload");});
            check("Background refresh retains a fresh snapshot during download",()->{GoogleContactStore.Entry e=find(account);String gen=GoogleContactStore.beginBackground(app,e.scheduleKey);require(gen!=null && GoogleContactStore.matches(app,caller),"Fresh snapshot lost at background start");});
            check("Transient retry retains fresh matches and records retry state",()->{GoogleContactStore.Entry e=find(account);GoogleContactStore.backgroundState(app,e.scheduleKey,e.generation,"Retry pending",false);require(GoogleContactStore.matches(app,caller) && find(account).refreshState.equals("Retry pending"),"Transient failure dropped fresh data");});
            check("Successful background completion updates status and snapshot",()->{GoogleContactStore.Entry e=find(account);String gen=GoogleContactStore.beginBackground(app,e.scheduleKey);require(GoogleContactStore.completeBackground(app,e.scheduleKey,gen,Collections.singleton(GoogleContactStore.fingerprint(caller))) && find(account).refreshState.equals("Success"),"Background result rejected");});
            check("Disabling auto cancels job and rejects in-flight completion",()->{GoogleContactStore.Entry e=find(account);String gen=GoogleContactStore.beginBackground(app,e.scheduleKey);GoogleContactStore.Entry off=GoogleContactStore.configureAuto(app,account,false,true);GoogleAutoRefresh.schedule(app,off,false);require(!GoogleContactStore.completeBackground(app,e.scheduleKey,gen,Collections.emptySet()) && app.getSystemService(JobScheduler.class).getPendingJob(e.jobId)==null && GoogleContactStore.matches(app,caller),"Off failed to cancel or preserve fresh data");});
            check("Renewed consent pauses matches and cancels automatic work",()->{GoogleContactStore.Entry e=GoogleContactStore.configureAuto(app,account,true,true);GoogleAutoRefresh.schedule(app,e,false);String gen=GoogleContactStore.beginBackground(app,e.scheduleKey);require(GoogleContactStore.requireReconnect(app,e.scheduleKey,gen),"Missing attention transition");require(!GoogleContactStore.matches(app,caller) && app.getSystemService(JobScheduler.class).getPendingJob(e.jobId)==null,"Consent pause did not stop matches/work");});
            check("Repeated consent state does not repeat attention transition",()->{GoogleContactStore.Entry e=find(account);require(!GoogleContactStore.backgroundState(app,e.scheduleKey,e.generation,"Reconnect required",true),"Repeated attention transition");});
            check("Manual reconnect preserves auto options and restores schedule",()->{String gen=GoogleContactStore.begin(app,account);GoogleContactStore.complete(app,account,gen,Collections.singleton(GoogleContactStore.fingerprint(caller)));GoogleAutoRefresh.reconcile(app);GoogleContactStore.Entry e=find(account);require(e.autoRefresh && e.wifiOnly && app.getSystemService(JobScheduler.class).getPendingJob(e.jobId)!=null,"Reconnect did not restore auto schedule");});
            check("Late consent/retry result cannot cancel a newer successful connection",()->{GoogleContactStore.Entry e=find(account);String old=GoogleContactStore.beginBackground(app,e.scheduleKey),manual=GoogleContactStore.begin(app,account);GoogleContactStore.complete(app,account,manual,Collections.singleton(GoogleContactStore.fingerprint(caller)));GoogleAutoRefresh.reconcile(app);require(!GoogleContactStore.requireReconnect(app,e.scheduleKey,old) && !GoogleContactStore.retryBackground(app,e.scheduleKey,old) && app.getSystemService(JobScheduler.class).getPendingJob(e.jobId)!=null && GoogleContactStore.matches(app,caller),"Stale result affected newer connection");});
            check("Disconnect cancels periodic and delayed one-off work",()->{
                GoogleContactStore.Entry e=find(account);PersistableBundle extras=new PersistableBundle();extras.putString(GoogleAutoRefresh.REF,e.scheduleKey);
                app.getSystemService(JobScheduler.class).schedule(new JobInfo.Builder(e.jobId+GoogleAutoRefresh.NOW_OFFSET,new ComponentName(app,GoogleContactsRefreshService.class)).setExtras(extras).setMinimumLatency(TimeUnit.HOURS.toMillis(1)).build());
                GoogleContactStore.remove(app,account);require(app.getSystemService(JobScheduler.class).getPendingJob(e.jobId)==null && app.getSystemService(JobScheduler.class).getPendingJob(e.jobId+GoogleAutoRefresh.NOW_OFFSET)==null,"Disconnect left queued work");
            });
            check("Existing live connections and contact hashes are preserved",()->{List<GoogleContactStore.Entry> after=GoogleContactStore.entries(app);require(after.size()==originals.size(),"Unexpected account count");for(GoogleContactStore.Entry before:originals){GoogleContactStore.Entry e=find(before.account);require(before.hashes.equals(e.hashes) && before.updated==e.updated && before.generation.equals(e.generation),"Existing snapshot changed by synthetic checks");}});
        }finally{if(activity!=null)runOnMainSync(()->activity.finish());GoogleContactStore.remove(app,account);}
    }
    private File baseline(){return new File(app.getCacheDir(),"quietgate-auto-verification-baseline.json");}
    private void verifyBackup(List<GoogleContactStore.Entry> entries)throws Exception{
        byte[] encrypted=null;File backup=new File(app.getFilesDir(),"quietgate-backups/before-0.10-alpha/app-private-data.tar");
        try(java.io.DataInputStream in=new java.io.DataInputStream(new FileInputStream(backup))){
            byte[] header=new byte[512];
            while(true){in.readFully(header);if(header[0]==0)break;
                int end=0;while(end<100 && header[end]!=0)end++;
                String name=new String(header,0,end,StandardCharsets.UTF_8),sizeText=new String(header,124,12,StandardCharsets.US_ASCII).replace("\u0000","").trim();
                long size=sizeText.isEmpty()?0:Long.parseLong(sizeText,8);require(size>=0 && size<32L*1024*1024,"Unexpected backup entry size");
                if(name.equals("./no_backup/google-saved-contacts.enc")){encrypted=new byte[(int)size];in.readFully(encrypted);break;}
                long left=((size+511)/512)*512;while(left>0){long skipped=in.skip(left);if(skipped==0){require(in.read()!=-1,"Truncated backup");skipped=1;}left-=skipped;}
            }
        }
        require(encrypted!=null && encrypted.length>29,"Encrypted snapshot not in backup");
        java.security.KeyStore keys=java.security.KeyStore.getInstance("AndroidKeyStore");keys.load(null);
        javax.crypto.Cipher cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");cipher.init(javax.crypto.Cipher.DECRYPT_MODE,keys.getKey("quietgate.google.saved.aes.v1",null),new javax.crypto.spec.GCMParameterSpec(128,Arrays.copyOfRange(encrypted,1,13)));
        JSONArray original=new JSONObject(new String(cipher.doFinal(encrypted,13,encrypted.length-13),StandardCharsets.UTF_8)).getJSONArray("accounts");require(original.length()==entries.size(),"Connected accounts changed during migration");
        for(int i=0;i<original.length();i++){JSONObject row=original.getJSONObject(i);GoogleContactStore.Entry e=find(row.getString("account"));Set<String> hashes=new HashSet<>();JSONArray h=row.getJSONArray("hashes");for(int j=0;j<h.length();j++)hashes.add(h.getString(j));
            require(e.hashes.equals(hashes) && e.updated==row.getLong("updated") && e.status.equals(row.getString("status")) && e.generation.equals(row.getString("generation")) && e.scheduleKey!=null && e.jobId>=100000,"Snapshot changed during migration");}
    }
    private void enable()throws Exception{
        JSONArray baseline=new JSONArray();long started=System.currentTimeMillis();int count=0;
        for(GoogleContactStore.Entry e:GoogleContactStore.entries(app)){
            require(!e.account.endsWith("@example.invalid"),"Synthetic account not cleaned up");
            baseline.put(new JSONObject().put("jobId",e.jobId).put("updated",e.updated));
            GoogleContactStore.Entry enabled=GoogleContactStore.configureAuto(app,e.account,true,true);GoogleAutoRefresh.schedule(app,enabled,false);
            report.append("JOB_ID=").append(enabled.jobId).append('\n');count++;
        }
        require(count>0,"No connected accounts");
        try(FileOutputStream out=new FileOutputStream(baseline())){out.write(new JSONObject().put("started",started).put("accounts",baseline).toString().getBytes(StandardCharsets.UTF_8));}
        report.append("Auto enabled for ").append(count).append(" existing accounts; unmetered Wi-Fi only. No consent UI or token persistence. Periodic jobs registered; immediate execution will be triggered through Android.\n");
    }
    private void status(boolean verify)throws Exception{
        JSONObject baseline=null;if(verify)baseline=new JSONObject(new String(java.nio.file.Files.readAllBytes(baseline().toPath()),StandardCharsets.UTF_8));
        for(GoogleContactStore.Entry e:GoogleContactStore.entries(app)){
            report.append("JOB_ID=").append(e.jobId).append(" AUTO=").append(e.autoRefresh).append(" WIFI_ONLY=").append(e.wifiOnly).append(" STATE=").append(e.refreshState).append(" IDENTIFIERS=").append(e.hashes.size()).append(" LAST_SUCCESS=").append(e.updated).append(" LAST_ATTEMPT=").append(e.lastAttempt).append('\n');
            if(verify){final JSONObject b=baseline;check("Android background job completed successfully with prior grant",()->{long old=-1;JSONArray rows=b.getJSONArray("accounts");for(int i=0;i<rows.length();i++)if(rows.getJSONObject(i).getInt("jobId")==e.jobId)old=rows.getJSONObject(i).getLong("updated");require(old>=0 && e.updated>old && e.lastAttempt>=b.getLong("started") && e.refreshState.equals("Success") && e.autoRefresh && e.wifiOnly,"Live refresh not confirmed");});
                check("Periodic schedule remains registered after background completion",()->{JobInfo j=app.getSystemService(JobScheduler.class).getPendingJob(e.jobId);require(j!=null && j.isPeriodic() && j.isPersisted(),"Periodic schedule missing");});}
        }
        if(verify && failed==0)baseline().delete();
    }
    private AccessibilityNodeInfo find(AccessibilityNodeInfo n,String text){if(n==null)return null;if(n.getText()!=null && n.getText().toString().equalsIgnoreCase(text))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo r=find(n.getChild(i),text);if(r!=null)return r;}return null;}
    private void tap(String text)throws Exception{long deadline=System.currentTimeMillis()+8000;while(System.currentTimeMillis()<deadline){AccessibilityNodeInfo n=find(getUiAutomation().getRootInActiveWindow(),text);if(n!=null){while(n!=null && !n.isClickable())n=n.getParent();require(n!=null && n.performAction(AccessibilityNodeInfo.ACTION_CLICK),"Click failed");waitForIdleSync();return;}Thread.sleep(100);}throw new IOException("Control missing");}
}
