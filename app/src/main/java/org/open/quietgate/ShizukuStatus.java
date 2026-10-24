package org.open.quietgate;

import android.content.pm.PackageManager;
import rikka.shizuku.Shizuku;

final class ShizukuStatus {
    private ShizukuStatus() {}

    static boolean isRunning() {
        try { return Shizuku.pingBinder(); }
        catch (RuntimeException ignored) { return false; }
    }

    static boolean isAuthorized() {
        if (!isRunning()) return false;
        try { return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED; }
        catch (RuntimeException ignored) { return false; }
    }

    static void requestPermission(int requestCode) {
        if (isRunning() && !isAuthorized()) Shizuku.requestPermission(requestCode);
    }
}
