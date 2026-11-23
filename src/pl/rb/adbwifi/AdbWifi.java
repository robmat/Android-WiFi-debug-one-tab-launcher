package pl.rb.adbwifi;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.provider.Settings;
import android.service.quicksettings.TileService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Inet4Address;

/** Reads and changes the wireless debugging state. */
final class AdbWifi {

    static final String ADB_WIFI_ENABLED = "adb_wifi_enabled";

    private AdbWifi() {
    }

    static boolean isEnabled(Context ctx) {
        return Settings.Global.getInt(ctx.getContentResolver(), ADB_WIFI_ENABLED, 0) == 1;
    }

    static boolean hasSecureSettingsPermission(Context ctx) {
        return ctx.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Turns wireless debugging on or off. Uses WRITE_SECURE_SETTINGS when granted,
     * otherwise falls back to root. Blocking - call off the main thread.
     */
    static boolean setEnabled(Context ctx, boolean on) {
        ContentResolver cr = ctx.getContentResolver();
        try {
            if (on) {
                Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1);
            }
            Settings.Global.putInt(cr, ADB_WIFI_ENABLED, on ? 1 : 0);
            return true;
        } catch (SecurityException e) {
            String cmd = (on ? "settings put global adb_enabled 1; " : "")
                    + "settings put global " + ADB_WIFI_ENABLED + " " + (on ? 1 : 0);
            return exec("su", "-c", cmd) != null;
        }
    }

    static boolean toggle(Context ctx) {
        return setEnabled(ctx, !isEnabled(ctx));
    }

    /** IPv4 address of the active network, or null. */
    static String ip(Context ctx) {
        ConnectivityManager cm = ctx.getSystemService(ConnectivityManager.class);
        if (cm == null) {
            return null;
        }
        Network net = cm.getActiveNetwork();
        LinkProperties lp = net == null ? null : cm.getLinkProperties(net);
        if (lp == null) {
            return null;
        }
        for (LinkAddress la : lp.getLinkAddresses()) {
            if (la.getAddress() instanceof Inet4Address) {
                return la.getAddress().getHostAddress();
            }
        }
        return null;
    }

    /** Port adbd listens on for wireless debugging (TLS), or null if unknown. */
    static String port() {
        String p = exec("getprop", "service.adb.tls.port");
        if (isBlankPort(p)) {
            p = exec("su", "-c", "getprop service.adb.tls.port");
        }
        return isBlankPort(p) ? null : p;
    }

    private static boolean isBlankPort(String p) {
        return p == null || p.isEmpty() || "0".equals(p);
    }

    /** "ip:port", "ip" or "" - whatever is known. Blocking. */
    static String address(Context ctx) {
        String ip = ip(ctx);
        if (ip == null) {
            return "";
        }
        String port = port();
        return port == null ? ip : ip + ":" + port;
    }

    /** Refreshes the home-screen widgets and the quick settings tile. Blocking. */
    static void refreshAll(Context ctx) {
        AppWidgetManager awm = AppWidgetManager.getInstance(ctx);
        int[] ids = awm.getAppWidgetIds(new ComponentName(ctx, ToggleWidget.class));
        ToggleWidget.update(ctx, awm, ids);
        TileService.requestListeningState(ctx, new ComponentName(ctx, ToggleTile.class));
    }

    private static String exec(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line);
                }
            }
            return p.waitFor() == 0 ? sb.toString().trim() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
