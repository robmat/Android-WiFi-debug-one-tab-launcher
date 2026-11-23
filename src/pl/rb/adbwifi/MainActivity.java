package pl.rb.adbwifi;

import android.app.Activity;
import android.app.StatusBarManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Status screen: current state, toggle button, shortcuts to add the widget and the tile. */
public class MainActivity extends Activity {

    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        status = new TextView(this);
        status.setTextSize(16);
        status.setPadding(0, 0, 0, pad / 2);
        root.addView(status);

        root.addView(button("Przełącz", () -> new Thread(() -> {
            ToggleWidget.toggleAndReport(getApplicationContext());
            runOnUiThread(this::refresh);
        }).start()));

        root.addView(button("Dodaj widżet na pulpit", () -> {
            AppWidgetManager awm = getSystemService(AppWidgetManager.class);
            if (awm != null && awm.isRequestPinAppWidgetSupported()) {
                awm.requestPinAppWidget(new ComponentName(this, ToggleWidget.class), null, null);
            }
        }));

        if (Build.VERSION.SDK_INT >= 33) {
            root.addView(button("Dodaj kafelek szybkich ustawień", () -> {
                StatusBarManager sbm = getSystemService(StatusBarManager.class);
                sbm.requestAddTileService(new ComponentName(this, ToggleTile.class), "ADB Wi-Fi",
                        Icon.createWithResource(this, R.drawable.ic_tile), getMainExecutor(),
                        result -> { });
            }));
        }
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean on = AdbWifi.isEnabled(this);
        boolean perm = AdbWifi.hasSecureSettingsPermission(this);
        status.setText("Debugowanie Wi-Fi: " + (on ? "WŁĄCZONE" : "wyłączone")
                + "\nUprawnienie WRITE_SECURE_SETTINGS: " + (perm ? "tak" : "nie (użyję roota)"));
        if (on) {
            new Thread(() -> {
                String addr = AdbWifi.address(this);
                if (!addr.isEmpty()) {
                    runOnUiThread(() -> status.append("\nAdres: " + addr));
                }
            }).start();
        }
    }

    private Button button(String label, Runnable action) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setAllCaps(false);
        btn.setOnClickListener(v -> action.run());
        return btn;
    }
}
