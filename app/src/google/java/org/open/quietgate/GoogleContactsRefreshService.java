package org.open.quietgate;

import android.Manifest;
import android.accounts.Account;
import android.app.*;
import android.app.job.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import com.google.android.gms.auth.api.identity.*;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Tasks;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Never opens account/consent UI in the background and never persists an OAuth token. */
public final class GoogleContactsRefreshService extends JobService {
    private final ExecutorService executor=Executors.newFixedThreadPool(2);
    private final ConcurrentHashMap<Integer,AtomicBoolean> running=new ConcurrentHashMap<>();
    @Override public boolean onStartJob(JobParameters parameters){
        AtomicBoolean stopped=new AtomicBoolean();running.put(parameters.getJobId(),stopped);
        executor.execute(()->{boolean retry=false;try{retry=refresh(parameters,stopped);}finally{running.remove(parameters.getJobId(),stopped);if(!stopped.get())jobFinished(parameters,retry);}});return true;
    }
    @Override public boolean onStopJob(JobParameters parameters){AtomicBoolean stopped=running.remove(parameters.getJobId());if(stopped!=null)stopped.set(true);return true;}
    @Override public void onDestroy(){for(AtomicBoolean stopped:running.values())stopped.set(true);executor.shutdownNow();super.onDestroy();}
    private boolean refresh(JobParameters parameters,AtomicBoolean stopped){
        String reference=parameters.getExtras().getString(GoogleAutoRefresh.REF),generation=null,token=null;
        try{
            GoogleContactStore.Entry e=GoogleContactStore.bySchedule(this,reference);if(e==null || !e.autoRefresh || stopped.get())return false;
            if(!GoogleAutoRefresh.networkAllowed(this,e.wifiOnly))return true;
            generation=GoogleContactStore.beginBackground(this,reference);if(generation==null)return true;
            AuthorizationRequest request=AuthorizationRequest.builder().setAccount(new Account(e.account,"com.google"))
                .setRequestedScopes(Collections.singletonList(new Scope(GoogleContactsSync.SCOPE))).build();
            AuthorizationResult result=Tasks.await(Identity.getAuthorizationClient(this).authorize(request),30,TimeUnit.SECONDS);
            if(stopped.get())return false;
            if(result.hasResolution() || result.getAccessToken()==null || result.getGrantedScopes()==null || !result.getGrantedScopes().contains(GoogleContactsSync.SCOPE)){
                boolean notify=GoogleContactStore.requireReconnect(this,reference,generation);
                if(notify)notifyReconnect(e.jobId);return false;
            }
            token=result.getAccessToken();Set<String> hashes=GoogleContactsSync.fetch(this,e.account,generation,token,stopped::get);
            if(stopped.get())return false;GoogleContactStore.completeBackground(this,reference,generation,hashes);return false;
        }catch(Exception error){
            if(stopped.get())return false;
            Throwable cause=error instanceof java.util.concurrent.ExecutionException?error.getCause():error;
            if(cause instanceof com.google.android.gms.common.api.ApiException){
                int code=((com.google.android.gms.common.api.ApiException)cause).getStatusCode();
                if((code==4 || code==6 || code==10) && generation!=null)try{
                    GoogleContactStore.Entry e=GoogleContactStore.bySchedule(this,reference);
                    boolean notify=GoogleContactStore.requireReconnect(this,reference,generation);
                    if(e!=null && notify)notifyReconnect(e.jobId);return false;
                }catch(Exception ignored){}
            }
            if(error instanceof GoogleContactsSync.Unauthorized && token!=null)Identity.getAuthorizationClient(this).clearToken(ClearTokenRequest.builder().setToken(token).build());
            if(generation!=null)try{return GoogleContactStore.retryBackground(this,reference,generation);}catch(Exception ignored){}
            return true;
        }
    }
    private void notifyReconnect(int id){
        NotificationManager manager=getSystemService(NotificationManager.class);if(manager==null || !manager.areNotificationsEnabled())return;
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        String channel="google-contact-refresh";manager.createNotificationChannel(new NotificationChannel(channel,"Google contact refresh",NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent open=PendingIntent.getActivity(this,id,new Intent(this,GoogleContactsActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        manager.notify(id,new Notification.Builder(this,channel).setSmallIcon(R.drawable.ic_status_quietgate).setContentTitle("QuietGate contacts need attention")
            .setContentText("Open QuietGate to reconnect a Google account. Device-contact matching remains available.")
            .setContentIntent(open).setAutoCancel(true).build());
    }
}
