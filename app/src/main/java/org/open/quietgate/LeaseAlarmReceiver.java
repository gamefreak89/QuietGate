package org.open.quietgate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LeaseAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        LeaseManager.refreshCurrent(context.getApplicationContext());
    }
}
