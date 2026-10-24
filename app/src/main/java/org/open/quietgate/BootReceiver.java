package org.open.quietgate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            LeaseManager.refreshCurrent(context.getApplicationContext());
            if (LeaseManager.isEnabled(context) && GuardNotificationService.start(context))
                GuardLog.add(context, "Status service restart requested", "Android boot completed; service start still depends on OS policy");
        }
    }
}
