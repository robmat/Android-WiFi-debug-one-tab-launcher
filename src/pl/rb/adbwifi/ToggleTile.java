package pl.rb.adbwifi;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick settings tile mirroring the widget. */
public class ToggleTile extends TileService {

    @Override
    public void onStartListening() {
        Tile t = getQsTile();
        if (t == null) {
            return;
        }
        boolean on = AdbWifi.isEnabled(this);
        t.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.setLabel("ADB Wi-Fi");
        t.setSubtitle(on ? "ON" : "OFF");
        t.updateTile();
        if (on) {
            new Thread(() -> {
                String addr = AdbWifi.address(this);
                Tile tt = getQsTile();
                if (tt != null && !addr.isEmpty()) {
                    tt.setSubtitle(addr);
                    tt.updateTile();
                }
            }).start();
        }
    }

    @Override
    public void onClick() {
        Tile t = getQsTile();
        if (t != null) {
            t.setState(Tile.STATE_UNAVAILABLE);
            t.updateTile();
        }
        new Thread(() -> ToggleWidget.toggleAndReport(getApplicationContext())).start();
    }
}
