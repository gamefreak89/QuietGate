# QuietGate 0.13-alpha requirements review

Source review on 2026-10-26; these findings describe current code, not tests with Wi-Fi or Shizuku disabled.

| Requirement | Google edition | FOSS edition |
| --- | --- | --- |
| Contacts and notification access | Required | Required |
| Status notifications | Required by enable flow | Required by enable flow |
| Running, authorized Shizuku | Enforced readiness condition | Enforced readiness condition |
| Active Wi-Fi | Enforced readiness condition | Enforced readiness condition |
| Internet permission | Included for optional Google authorization/refresh | Absent |
| Google OAuth consent | Optional, read-only Google Contacts | Absent |
| Root | Not required; no stronger rejection claimed | Not required; no stronger rejection claimed |

MainActivity.toggleProtection checks Contacts, notification access, Shizuku authorization, Wi-Fi, and status-notification permission. LeaseManager.update starts a saved 24-hour deadline when either readiness condition is lost. Both must return before expiry to reset it. After expiry, protection requires manual re-enabling.

These Wi-Fi/Shizuku conditions are application policy. The actual call path uses Android's Contacts provider, optional local Google fingerprints, and Meet's exposed notification PendingIntent (MeetRejectListener: decline.actionIntent.send). ShizukuStatus only checks its running/authorization state and requests authorization; it does not perform privileged Meet call control. Local matching and notification-action invocation do not inherently depend on Shizuku or Wi-Fi. No gates were removed in this release, and operation without them has not been demonstrated by this review.

GoogleContactsSync uses HTTPS to Google for authorization/refresh support. Manual refresh is not restricted to Wi-Fi. GoogleAutoRefresh defaults to unmetered Wi-Fi; its optional any-network setting allows other network connections. Shizuku does not authorize Google or fetch its contacts. OAuth consent is separate from Android Contacts permission. The FOSS app has no Google integration or Internet permission; Google Meet itself still needs a suitable connection. Document providers may use their own network access when a user imports or exports files.

The user reported a successful live call test with the 0.12-alpha FOSS edition. That report was not independently observed, did not specify a complete test matrix, and does not establish operation without the current readiness conditions. No OAuth grant, outage, 24-hour endurance, or root test was performed for this review.
