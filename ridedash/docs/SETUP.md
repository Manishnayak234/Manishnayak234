# Setting RideDash up on the phone

Written for the iQOO 9 SE on Funtouch OS 14 (Android 14). Other vivo phones behave the same; the
Android parts are the same everywhere.

## 1. Install

```bash
cd ridedash
./gradlew installDebug
```

## 2. Permissions, in the order the checklist shows them

The launcher icon opens the checklist. Green means granted, amber means still needed, grey means it is
yours to do by hand. **RE-CHECK** re-reads everything after you come back from a settings page.

| Item | What it is for | Where it lands |
|---|---|---|
| Location (precise) | Speed, trip, heading | Normal runtime dialog — choose **Precise** |
| Post notifications | Foreground service notice, turn alerts | Runtime dialog |
| Display over other apps | The speed box over Maps, and letting the charger start the dashboard from the background | Settings page per app |
| Notification access | Reading the Maps turn notification | Notification access list — tick RideDash |
| Ignore battery optimisation | Stops the service being killed mid-ride | Battery optimisation list |
| Location in the background | Only if you want logging with the screen off | App info → Permissions → Location → Allow all the time |
| NFC | The tag on the mount | System NFC toggle |

Without notification access there is no Navigating screen — only Cruising. Without the overlay
permission the Map button still opens Maps, but no speed box appears over it.

## 3. The vivo/iQOO bits, which an app cannot do for you

1. **Autostart**: Settings → Apps → RideDash → **Autostart** → on. Needed for the charger trigger to
   survive a reboot.
2. **Background power**: Settings → Battery → Background power consumption → RideDash → **Allow**.
3. **Lock in Recents**: open Recents, drag the RideDash card down (or use the lock icon) so the system
   stops clearing it.

Skip these and the phone will kill the service on a long ride. It is the single most common reason a
dashboard app "randomly stops" on Funtouch OS.

## 4. The NFC tag on the mount

Write one NDEF tag with two records:

1. A **URI record**: `ridedash://start`
2. An **Android Application Record** for `com.manish.ridedash`

NFC Tools (Android) writes both: *Write → Add a record → URI/URL*, then *Add a record → Android
Application Record*. Stick the tag where the phone's NFC antenna lands when it is seated in the mount.

Tapping it opens the dashboard, and it also stamps the time. That stamp is what the "only start from
the bike charger" option checks (a tap within the last 60 seconds).

## 5. The charger trigger

`TriggerService` keeps a minimum-importance notification and one runtime receiver so it can see
`ACTION_POWER_CONNECTED` — that broadcast has not been available to manifest receivers since
Android 8. Plug in and the dashboard takes the screen.

If you would rather it not start from any charger, set the trigger to want an NFC tap first; without a
recent tap it posts a tap-to-start notice instead of taking over. (The option is stored in DataStore as
`charger_needs_nfc`; there is no switch on the checklist screen for it yet.)

Unplugging does not exit by itself straight away: if the bike is unplugged and standing still for two
minutes, the dashboard steps down on its own.

## 6. Bench testing without a bike

**Mock GPS speed.** Install any "mock location" app, then Developer options → **Select mock location
app**. Set a speed and the gauge, trip and heading all follow.

**Maps parsing.** Start a route in Google Maps (walking is fine, or with the mock location app) and
watch:

```bash
adb logcat -s RideDash/MapsRaw
```

Every notification field is logged verbatim — title, text, bigText, subText and whether a large icon
came with it. That log is the ground truth for `MapsParser`; the unit tests in
`app/src/test/.../MapsParserTest.kt` are a starting set built from the expected shapes, not from your
phone.

Other useful tags:

```bash
adb logcat -s RideDash/Service RideDash/Location RideDash/Overlay RideDash/Trigger RideDash/Main
```

**Lean.** Hold the phone in the mount position, long-press the LEAN tile to zero it, then tilt. If the
sign is inverted, flip `LeanSource.LEAN_SIGN`.

**Night mode.** Cover the light sensor (top of the screen) for five seconds; the theme should drop to
the night tokens, and go back after five seconds in the light.

## 7. Sunlight check

Do it outdoors at midday, phone in the mount, at arm's length:

- The speed number and the turn distance should be readable without leaning in.
- Auto brightness stays **on**. The app deliberately never sets brightness itself, because the
  sunlight boost on this phone only kicks in under auto brightness.
- If the phone gets hot, the status bar shows the battery temperature past 45 °C. Shade it.
