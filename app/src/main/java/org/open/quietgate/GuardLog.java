package org.open.quietgate;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class GuardLog {
    private static final String PREFS = "quiet_gate";
    private static final String KEY_LOG = "recent_events";
    private static final int MAX_EVENTS = 30;
    private GuardLog() {}

    static synchronized void add(Context context, String result, String detail) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        JSONArray old;
        try { old = new JSONArray(p.getString(KEY_LOG, "[]")); }
        catch (Exception ignored) { old = new JSONArray(); }
        JSONArray next = new JSONArray();
        String when = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(new Date());
        next.put(when + " · " + result + " · " + detail);
        for (int i = 0; i < old.length() && i < MAX_EVENTS - 1; i++) next.put(old.optString(i));
        p.edit().putString(KEY_LOG, next.toString()).apply();
    }

    static synchronized List<String> recent(Context context) {
        List<String> rows = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(KEY_LOG, "[]"));
            for (int i = 0; i < array.length(); i++) rows.add(array.optString(i));
        } catch (Exception ignored) { }
        return rows;
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_LOG).apply();
    }
}
