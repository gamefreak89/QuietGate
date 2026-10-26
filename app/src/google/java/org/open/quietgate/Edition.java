package org.open.quietgate;

import android.content.Context;
import android.content.Intent;

/** Google integration is compiled only into the Google edition. */
final class Edition {
    static final boolean GOOGLE_CONTACTS=true;
    static final String NAME="QuietGate";
    static void reconcile(Context context) { GoogleAutoRefresh.reconcile(context); }
    static boolean matches(Context context,String identifier) {
        try { return GoogleContactStore.matches(context,identifier); }
        catch(Exception unavailable) { return false; }
    }
    static Intent contactSettings(Context context) { return new Intent(context,GoogleContactsActivity.class); }
}
