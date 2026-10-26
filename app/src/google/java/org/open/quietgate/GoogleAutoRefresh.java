package org.open.quietgate;

import android.app.job.*;
import android.content.*;
import android.net.*;
import android.os.PersistableBundle;
import java.util.concurrent.TimeUnit;

/** Android-managed jobs; persisted extras contain only an opaque connection reference. */
final class GoogleAutoRefresh {
    static final String REF="connectionReference";
    static final long PERIOD_MS=TimeUnit.HOURS.toMillis(12),FLEX_MS=TimeUnit.HOURS.toMillis(1);
    static final int NOW_OFFSET=1000000000;
    private static JobScheduler scheduler(Context c){return c.getSystemService(JobScheduler.class);}
    static JobInfo build(Context c,GoogleContactStore.Entry e,boolean now){
        PersistableBundle extras=new PersistableBundle();extras.putString(REF,e.scheduleKey);
        JobInfo.Builder job=new JobInfo.Builder(e.jobId+(now?NOW_OFFSET:0),new ComponentName(c,GoogleContactsRefreshService.class))
            .setExtras(extras).setPersisted(true).setRequiresBatteryNotLow(true)
            .setRequiredNetworkType(e.wifiOnly?JobInfo.NETWORK_TYPE_UNMETERED:JobInfo.NETWORK_TYPE_ANY)
            .setBackoffCriteria(TimeUnit.MINUTES.toMillis(15),JobInfo.BACKOFF_POLICY_EXPONENTIAL);
        if(!now)job.setPeriodic(PERIOD_MS,FLEX_MS);return job.build();
    }
    static void schedule(Context c,GoogleContactStore.Entry e,boolean now){
        if(!e.autoRefresh){cancel(c,e);return;}
        JobScheduler jobs=scheduler(c);if(jobs==null)throw new IllegalStateException("Android job scheduler unavailable");
        if(jobs.schedule(build(c,e,false))!=JobScheduler.RESULT_SUCCESS)throw new IllegalStateException("Could not schedule refresh");
        if(now && jobs.schedule(build(c,e,true))!=JobScheduler.RESULT_SUCCESS)throw new IllegalStateException("Could not queue refresh");
    }
    static void reconcile(Context c){
        try{JobScheduler jobs=scheduler(c);if(jobs==null)return;
            for(GoogleContactStore.Entry e:GoogleContactStore.entries(c)){
                if(!e.autoRefresh || "Reconnect required".equals(e.refreshState)){cancel(c,e);continue;}
                // Do not reset the periodic clock each time the app opens.
                if(jobs.getPendingJob(e.jobId)==null)schedule(c,e,false);
            }
        }catch(Exception ignored){}
    }
    static void cancel(Context c,GoogleContactStore.Entry e){JobScheduler jobs=scheduler(c);if(jobs!=null){jobs.cancel(e.jobId);jobs.cancel(e.jobId+NOW_OFFSET);}}
    static void cancelAll(Context c){JobScheduler jobs=scheduler(c);if(jobs!=null)for(JobInfo job:jobs.getAllPendingJobs())if(job.getService().getClassName().equals(GoogleContactsRefreshService.class.getName()))jobs.cancel(job.getId());}
    static boolean networkAllowed(Context c,boolean wifiOnly){
        ConnectivityManager manager=c.getSystemService(ConnectivityManager.class);if(manager==null)return false;
        NetworkCapabilities caps=manager.getNetworkCapabilities(manager.getActiveNetwork());
        return caps!=null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            && (!wifiOnly || (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !manager.isActiveNetworkMetered()));
    }
}
