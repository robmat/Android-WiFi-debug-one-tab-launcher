package pl.rb.adbwifi;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.RemoteViews;
import android.widget.Toast;

/** Home-screen widget: one tap toggles wireless debugging. */
public class ToggleWidget extends AppWidgetProvider {

    static final String ACTION_TOGGLE = "pl.rb.adbwifi.TOGGLE";

    @Override
    public void onUpdate(Context ctx, AppWidgetManager awm, int[] ids) {
        PendingResult pr = goAsync();
        new Thread(() -> {
            try {
                update(ctx, awm, ids);
            } finally {
                pr.finish();
            }
        }).start();
    }

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!ACTION_TOGGLE.equals(intent.getAction())) {
            super.onReceive(ctx, intent);
            return;
        }
        PendingResult pr = goAsync();
        new Thread(() -> {
            try {
                toggleAndReport(ctx);
            } finally {
                pr.finish();
            }
        }).start();
    }

    /** Toggles, waits for adbd to settle, refreshes the UI and shows a toast. Blocking. */
    static void toggleAndReport(Context ctx) {
        boolean wasOn = AdbWifi.isEnabled(ctx);
        boolean ok = AdbWifi.toggle(ctx);
        if (ok && !wasOn) {
            sleep(1500);
        }
        AdbWifi.refreshAll(ctx);
        String msg;
        if (!ok) {
            msg = "Brak uprawnień (WRITE_SECURE_SETTINGS / root)";
        } else if (AdbWifi.isEnabled(ctx)) {
            String addr = AdbWifi.address(ctx);
            msg = "Debugowanie Wi-Fi włączone" + (addr.isEmpty() ? "" : "\n" + addr);
        } else if (!wasOn) {
            msg = "Nie udało się włączyć - czy jesteś w sieci Wi-Fi?";
        } else {
            msg = "Debugowanie Wi-Fi wyłączone";
        }
        new Handler(Looper.getMainLooper()).post(
                () -> Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show());
    }

    /** Renders the widgets with the current state. Blocking (reads the port). */
    static void update(Context ctx, AppWidgetManager awm, int[] ids) {
        if (ids == null || ids.length == 0) {
            return;
        }
        boolean on = AdbWifi.isEnabled(ctx);
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget);
        rv.setInt(R.id.root, "setBackgroundResource", on ? R.drawable.bg_on : R.drawable.bg_off);
        rv.setTextViewText(R.id.state, on ? "ADB Wi-Fi: ON" : "ADB Wi-Fi: OFF");
        rv.setTextViewText(R.id.addr, on ? AdbWifi.address(ctx) : "dotknij, aby włączyć");
        Intent toggle = new Intent(ctx, ToggleWidget.class).setAction(ACTION_TOGGLE);
        rv.setOnClickPendingIntent(R.id.root, PendingIntent.getBroadcast(ctx, 0, toggle,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        awm.updateAppWidget(ids, rv);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
