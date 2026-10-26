package org.open.quietgate;

import android.content.Context;
import android.content.Intent;

/** No network client, account integration, or Google library is compiled into this edition. */
final class Edition {
    static final boolean GOOGLE_CONTACTS=false;
    static final String NAME="QuietGate FOSS";
    static void reconcile(Context context) {}
    static boolean matches(Context context,String identifier) { return false; }
    static Intent contactSettings(Context context) { return null; }
}
