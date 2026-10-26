package org.open.quietgate;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Short, edition-aware consent summary; detailed policies remain in About. */
final class ProtectionDisclosure {
    private ProtectionDisclosure() {}

    static View create(Context context) {
        ScrollView scroll = new ScrollView(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int inset = dp(context, 24);
        content.setPadding(inset, dp(context, 12), inset, dp(context, 8));
        scroll.addView(content);
        paragraph(context, content, "QuietGate helps safeguard you against spam and unwanted Google Meet video calls.", false);
        section(context, content, "How it works", "Saved contact matches are allowed. Unknown or missing caller details trigger an attempt to decline through Meet. Save the caller's exact Meet email; a phone-only contact may not match.");
        section(context, content, "Limits", "Blocking is not guaranteed. Calls may ring briefly. Caller identity and completed rejection cannot be verified. Cellular calls are not covered.");
        section(context, content, "Required setup", "Allow Contacts, notification access, and status notifications. Both editions require Wi-Fi and authorized, running Shizuku. If either remains unavailable for 24 hours, protection turns off.");
        section(context, content, Edition.GOOGLE_CONTACTS ? "Optional Google access" : "FOSS edition",
                Edition.GOOGLE_CONTACTS
                ? "Google sign-in (OAuth) separately requests read-only Google Contacts access. Internet refresh is optional; device-contact protection works without connecting Google."
                : "Uses device contacts only. No Google sign-in, Google account permission, or Internet permission is requested.");
        paragraph(context, content, "See About for full permissions and privacy details.", false);
        return scroll;
    }

    private static void section(Context context, LinearLayout content, String title, String body) {
        paragraph(context, content, title, true);
        paragraph(context, content, body, false);
    }

    private static void paragraph(Context context, LinearLayout content, String value, boolean heading) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(heading ? 16 : 15);
        text.setTextColor(0xFF193C43);
        text.setTypeface(Typeface.DEFAULT, heading ? Typeface.BOLD : Typeface.NORMAL);
        text.setLineSpacing(dp(context, 2), 1f);
        text.setPadding(0, dp(context, heading ? 14 : 4), 0, dp(context, 4));
        content.addView(text);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
