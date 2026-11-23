# ADB Wi-Fi toggle

Tiny Android app (no Gradle, no AndroidX) that turns **Wireless debugging** on and off with one tap:

- **Home-screen widget**: tap to toggle. Green = on, with `ip:port` for `adb connect`; grey = off.
- **Quick settings tile**: same toggle from the notification shade.
- **App screen**: current state, a toggle button, and buttons to add the widget/tile.

Requires Android 11+ (API 30), the first version with Wireless debugging.

## How it works

The app flips the `adb_wifi_enabled` global setting (and `adb_enabled` when turning on).
Writing global settings needs `WRITE_SECURE_SETTINGS`, which you grant once over adb:

```sh
adb shell pm grant pl.rb.adbwifi android.permission.WRITE_SECURE_SETTINGS
```

Without that permission it falls back to `su` (root).
The wireless debugging port (`service.adb.tls.port`) can only be read as root. Without root
the widget shows just the IP.

Android turns Wireless debugging off by itself when the phone is not on Wi-Fi, and picks a new
random port each time it is turned on.

## Build

Needs the Android SDK (build-tools 36.1.0, platform android-36) and JDK 17+:

```powershell
.\build.ps1          # -> build\adb-wifi-toggle.apk, signed with the debug keystore
adb install -r build\adb-wifi-toggle.apk
adb shell pm grant pl.rb.adbwifi android.permission.WRITE_SECURE_SETTINGS
```
