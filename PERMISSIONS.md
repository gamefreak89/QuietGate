# Permissions and access

0.12-alpha has two editions. FOSS excludes INTERNET and all Google account/refresh components; Google retains them for the optional connection feature. Both request device Contacts, notification access, status-notification/foreground-service access, network-state checks for Wi-Fi readiness, boot handling, and Shizuku authorization. The FOSS app ID requires its own grants. No device permission was changed while creating these builds.

0.11-alpha adds no permission or dependency. Permission explanations now have their own About page, and notification-access readiness compares the exact listener component.

0.10-alpha uses native Android JobScheduler for explicitly enabled background saved-contact refresh. No new Android permission or library is added. INTERNET includes these requests; ACCESS_NETWORK_STATE checks the chosen network policy; existing RECEIVE_BOOT_COMPLETED permits persisted jobs. BIND_JOB_SERVICE is a component guard restricting the refresh service to Android, not a permission granted to QuietGate. Refresh-attention notices use the existing notification grant.

0.8-alpha adds incident ZIP exports without new Android permissions. Android's document picker grants access to user-selected attachments and the chosen ZIP destination. A cloud provider may download attachments or upload the report. Original attachment bytes and metadata are retained in exports.

| Permission/access | Purpose |
| --- | --- |
| READ_CONTACTS | Match Meet-exposed caller email/number against locally available Contacts. |
| INTERNET (new in 0.9-alpha) | Optional Google authorization and user-started saved-contact refresh over HTTPS. No caller identifier is uploaded for matching. |
| Google OAuth contacts.readonly (optional, per chosen account) | Google permits read-only saved-contact access; requests fetch only email/phone fields. Not Gmail, Other contacts or directory access. |
| Android Notification Access / BIND_NOTIFICATION_LISTENER_SERVICE | Android binds the listener after approval; read Meet call notices and invoke an explicit Decline/Reject action. Access is broader than the package filter in code. |
| POST_NOTIFICATIONS | Display the ongoing QuietGate status notice on Android 13+. |
| ACCESS_NETWORK_STATE | Determine whether the active network is Wi-Fi for the grace deadline. |
| RECEIVE_BOOT_COMPLETED | Request timer/service restoration after boot when Android allows it. |
| FOREGROUND_SERVICE | Run the visible protection status service. |
| FOREGROUND_SERVICE_SPECIAL_USE | Declare that status service on recent Android versions. |
| Shizuku API authorization | Check running/authorized readiness. It does not strengthen Meet call control. |
| Android document picker | Read only the selected import file or write the selected export document, without broad storage access. |

The Shizuku provider has an INTERACT_ACROSS_USERS_FULL component guard; this is not a runtime request for QuietGate to gain that system privilege. The Shizuku library contributes its authorization declaration to the merged manifest.

Not requested: GET_ACCOUNTS, RECORD_AUDIO, READ_CALL_LOG, READ_PHONE_STATE, location, SYSTEM_ALERT_WINDOW. Android's account chooser supplies only the user's chosen account; no silent account enumeration is added. Call catalogs are app data, not Android system call logs.
