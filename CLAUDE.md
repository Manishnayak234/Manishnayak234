# RideDash — Project Brief (for Claude Code)

> The Gradle project lives in [`ridedash/`](ridedash/); its build, install and device notes are in
> [`ridedash/README.md`](ridedash/README.md). This repository is also Manish's GitHub profile
> repository, which is why the app is in a subdirectory rather than at the root.

## 1. What we're building
- Android app that turns an old phone into a **handlebar navigation dashboard** for a **Triumph Speed 400**.
- It is an **add-on**, not a cluster replacement. The bike's own cluster still shows speed, fuel and gear.
- The bike has **no Bluetooth or CAN access**, so all data comes from the **phone's own sensors**, plus Google Maps turn-by-turn.
- There are **two modes**:
  - **Dashboard mode:** full screen, always on, pinned.
  - **Normal mode:** the phone works like a normal phone.

## 2. Hardware and environment
- **Phone:** iQOO 9 SE, Android 14 (Funtouch OS 14), AMOLED, 2400×1080.
  - About **914 × 412 dp** in landscape.
- **Mount:** handlebar, **landscape only**.
- **Power:** the bike's USB-C port.
- **Watch:** goBoult Mustang Racer (Bluetooth).
  - It only mirrors phone notifications. There is no SDK.
- **Audio:** helmet intercom. Google Maps voice guidance plays through it.
- **Dev machine:** Ubuntu laptop (RTX 4060), with Android Studio.
  - **No emulator.** Test on the real phone over USB / wireless ADB.

## 3. Tech stack
- **Language and UI:** Kotlin + Jetpack Compose (Material 3, but a custom theme).
- **Architecture:** single Activity, MVVM, `StateFlow` → Compose.
- **Background work:** `DashboardService` (foreground service) owns the sensors, location, overlay and triggers.
- **SDK levels:** `minSdk 29`, `targetSdk 35`, `compileSdk 35`.
- **Location:** Google Play Services `FusedLocationProviderClient`, high accuracy, 1 s interval.
  - Fallback: `LocationManager.GPS_PROVIDER`.
- **Fonts:** bundle **Barlow Condensed** (600/700, 700/800 italic) and **Barlow** (500/600/700) in `res/font` (OFL licence).
- **Distribution:** sideload only, not on the Play Store.
- **Package name:** `com.manish.ridedash`.

## 4. Screens (design is final for v1)
The mockup is on the claude.ai design canvas "Bike Dashboard v1". All sizes below are in dp at 914×412.

### Common to all screens
- **Background:** pure black `#000000`, for sunlight contrast on AMOLED.
- **Status bar** (height 40, bottom border 1 px in `line` colour):
  - **Left:** GPS crosshair icon + "GPS · <satellites>", in `ok` green.
    - Grey or red when there's no fix.
  - **Center:** clock, Barlow Condensed 700, 26 sp.
  - **Right:** watch icon, Bluetooth icon (`nav` cyan), battery icon + %.
- **Bottom bar** (height 64): 3 equal buttons, 48 tall, radius 10, `tile` background, 1 px `line` border, 17 sp bold, glove-sized.
  1. **Map** → opens the Map view (section 4.3).
  2. **Ride stats** → trip summary screen (v2, stub for now).
  3. **Hold to exit** (`sub` colour) → press and hold 2 s to leave dashboard mode.

### 4.1 Navigating screen (route active)
- **Left panel** (width 360, right border):
  - A 290×290 **arc speed gauge**:
    - Circle radius 120 in a 300 viewBox, stroke 24, sweeping 270° from 135° clockwise.
    - `track` background arc, `accent` filled arc.
  - **Scale 0–180 km/h**, with labels every 30 (0, 30 … 180), Barlow Condensed 17 sp, `sub` colour.
  - **Center:** speed number, Barlow Condensed **italic 800, 116 sp**, with "km/h" at 20 sp in `sub` below.
  - **Bottom of the gauge:** "`<trip km>` · `<ride time>`" at 17 sp in `sub`.
- **Right panel** (padding 12/28, gap 16):
  - **Maneuver row:**
    - Turn arrow, 112 dp, `nav` cyan.
    - Distance at 92 sp Barlow Condensed 700, with the unit at 30 sp in `sub`.
    - Street name at 32 sp bold.
  - **Progress bar** (8 dp, `nav` on `track`): fills as you approach the turn.
  - **"Then"** + small arrow + next street, 20 sp.
  - **Bottom row** (top border): **ETA / LEFT / TIME**, with 14 sp labels and 32 sp Barlow Condensed values.

### 4.2 Cruising screen (no route)
- Same status bar, gauge and bottom bar as 4.1.
- The **right panel** is a 3×2 grid (gap 12), with tiles of radius 14 on a `tile` background:
  - **HEADING:** compass needle + "NE 30°".
  - **LEAN:** "L 23°" + a small semicircle needle, with "Max L xx° · R xx°" below.
  - **WEATHER:** "27°" + "Part cloud · Feels 30°".
  - **ALTITUDE:** "560 m".
  - **MAX · AVG SPEED:** "96 / 41 km/h".
  - **RAIN · WIND:** "40%" + "12 km/h NE". The number turns `accent` once rain is likely
    (raining now, or 60% or more in the next hour), which is the one weather fact worth catching out
    of the corner of an eye.
- Tile values are Barlow Condensed 700, 46 sp (52 sp was right for two columns, not three). Labels are
  14 sp, letter-spacing 1, in `sub`.
- **Auto-switch:** show 4.2 when there's no active Google Maps navigation notification, and 4.1 when there is one.

### 4.3 Map view (chosen approach: "Option 1")
- Tapping **Map** brings **the Google Maps app** to the front, full screen, with its live map, route and your position.
- Our app draws a **floating speed box** over it, using `SYSTEM_ALERT_WINDOW` from the service:
  - 150×150, radius 22, black background, 3 dp `accent` border, shadow.
  - Speed in Barlow Condensed italic 800, 72 sp, with "km/h" underneath.
  - A small "‹ Dashboard" label in `accent`.
  - **Draggable**, and it remembers where you left it.
  - **Tap** brings the dashboard back to the front.
- The overlay shows only while Maps is in front during dashboard mode.

### 4.4 Key-on sweep
- Entering dashboard mode sweeps the gauge **0 → 180 → 0** before live speed takes over: 800 ms up,
  600 ms back, `FastOutSlowIn`, the number counting with the arc.
- It runs **once per key-on**, like the bike's own cluster — not once per install, and not again on the
  way back from the stats screen.
- **A tap anywhere skips it.** It doubles as a self-test: the whole arc and every scale label are on
  screen before the ride starts.

### 4.5 Recording mode
- For explaining a route out loud: the camera records and the **speed is burned into the frames**, so
  one file has the rider on one side and the numbers on the other with nothing to edit afterwards.
- **CameraX** `VideoCapture` at FHD (falling back to HD), plus `OverlayEffect` from
  `androidx.camera:camera-effects` targeting **both** preview and video capture, so what is on screen
  is exactly what lands in the file.
- The panel is plain `Canvas` drawing (`SpeedPanelPainter`), sized as fractions of the frame so 1080p
  and 4K both come out right. It takes the left **40%** of the width; set `PANEL_FRACTION` to `0.5`
  for a true half-and-half split.
- Panel contents: speed (Barlow Condensed italic), "km/h", then REC elapsed, TRIP and LEAN.
- Front camera by default (the rider talking), switchable to the rear for the road. Mirroring is left
  off so the burned-in text is never reversed.
- Audio comes from the microphone for commentary; without that permission the clip is silent and says
  so on screen.
- Files land in **Movies/RideDash** via `MediaStore`, named `RideDash_<timestamp>.mp4`.
- Recording only runs while the recording screen is in front, so nothing keeps the camera open behind
  the rider's back. **It makes the phone hotter** — expect the heat warning sooner in the sun.

### 4.6 Colour tokens
| Token | Day | Night |
|---|---|---|
| bg | #000000 | #000000 |
| fg (text) | #FFFFFF | #D6D6D6 |
| sub (labels) | #BDBDBD | #8F8F8F |
| accent (speed arc, overlay) | #FFD400 | #D9A400 |
| nav (turn arrow, BT) | #00E5FF | #00AFC2 |
| ok (GPS fix) | #3DDC84 | #2FA866 |
| track | #2A2A2A | #1A1A1A |
| line | #262626 | #171717 |
| tile | #121212 | #0B0B0B |
- **Night mode is automatic,** switched by the ambient light sensor with hysteresis.
  - Rough starting points: night below about 10 lux, day above about 40 lux, both held for 5 s.
  - It can also follow the time of sunset/sunrise.

### 4.7 Sunlight readability rules (must follow)
- No grey-on-grey, no gradients, no transparency on data, no thin fonts.
- Keep the main numbers huge. Show only essential items while riding.
- **Do not force brightness to 100%.** Keep the system auto-brightness on, because the phone's extra-bright sunlight boost usually works only with auto-brightness.

## 5. Data sources and logic
- **Speed:** `Location.speed` (m/s) × 3.6.
  - Show 0 below 2 km/h.
  - Light smoothing: EMA α≈0.5 or a 3-sample median.
  - Hide the value or show "--" when there's no fix or `speedAccuracy` is poor.
- **Trip distance:** sum the distances between fixes.
  - Ignore a jump when accuracy is above 20 m, or the implied speed is above 250 km/h.
- **Ride time:** time moving (speed above 3 km/h).
- **Max and avg speed:** from the moving samples.
- **Heading:**
  - While moving: `Location.bearing`.
  - When stopped or slow: the rotation-vector sensor (azimuth), corrected for landscape orientation.
  - Shown as a compass point + degrees.
- **Lean angle:** `TYPE_GAME_ROTATION_VECTOR` → roll, for the phone in landscape on the bars.
  - **Zero calibration:** a long-press on the lean tile, done while the bike is upright.
  - Low-pass about 5 Hz. Show L/R with sign, and track the maximum on each side.
  - Note: a handlebar-mounted phone also turns with the steering, so treat lean as approximate.
- **Altitude:** the barometer (`TYPE_PRESSURE` → `SensorManager.getAltitude`), offset against GPS altitude every few minutes.
  - Fallback: GPS altitude.
- **Weather:** **Open-Meteo**, chosen because it needs no API key and no account — nothing to leak in a
  sideloaded APK. One request for current conditions plus the next two hours of rain probability.
  - Refreshed at most every 20 minutes, or sooner once the bike has moved 5 km.
  - A failure keeps the last reading rather than blanking the tiles; with no network at all they show
    "--".
- **Clock and battery:** system values. Also watch the battery temperature.
  - If it's above about 45 °C, show a small heat warning. Heat is the main risk in the sun.

## 6. Navigation from Google Maps (no Maps SDK)
- **`NotificationListenerService`** reads the ongoing notification from `com.google.android.apps.maps`.
- **Fields to parse:**
  - `EXTRA_TITLE` (usually the distance or the instruction)
  - `EXTRA_TEXT` / `EXTRA_BIG_TEXT` (street / "Then …")
  - `EXTRA_SUB_TEXT` (ETA / distance / time)
  - The **large icon bitmap**, which is the maneuver arrow. Draw it tinted `nav` instead of mapping maneuver types.
- **First milestone task:** log every field to Logcat during a real navigation. The exact format differs between Maps versions and languages, so build the parser from the real samples.
  - Keep the parser tolerant: use regex for "m/km", times, and "Then".
- **Progress bar:** the distance at the first reading of each maneuver is 0% progress, and 0 m is 100%.
- **Navigation active** = that notification is present, which is what switches between 4.1 and 4.2.

## 7. Watch alerts
- Post our own **turn notification** on a dedicated channel.
  - Format: "↱ 350 m · MG Road". Use a text arrow, not an emoji image.
- Re-alert (vibrate) only on a **new maneuver** and again at about 200 m / 50 m. Otherwise use `setOnlyAlertOnce(true)`.
- The goBoult companion app mirrors it to the watch. Dada allows our app in the watch app's notification settings.

## 8. Dashboard mode: entry, kiosk and exit
- **Entry triggers:**
  1. **NFC tag on the mount:** an NDEF tag with an Android Application Record plus a custom URI (e.g. `ridedash://start`), and an intent filter on the Activity.
  2. **Charger connected:**
     - `ACTION_POWER_CONNECTED` can't be registered in the manifest on Android 8+, so a lightweight foreground service registers the receiver dynamically.
     - It starts the Activity, which is allowed from the background because the app holds `SYSTEM_ALERT_WINDOW`.
     - Optional setting: only trigger when charging from a "bike" charger. For example, require NFC in the last 60 s, or ask with a full-screen notification.
  3. **Manual:** a launcher icon and a Quick Settings tile.
- **While in dashboard mode:**
  - `FLAG_KEEP_SCREEN_ON`, immersive full screen, landscape locked.
  - `startLockTask()` (screen pinning) and Do Not Disturb (optional).
- **Exit:** hold for 2 s → `stopLockTask()`, then stop location, sensors and the overlay, and return to normal use.
  - Also exit automatically after unplugging if not moving for 2 min (optional).
- **Foreground service type (Android 14):**
  - `location` for GPS, and `specialUse` for the overlay/trigger if needed.
  - Declare the matching permissions.

## 9. Permissions and setup checklist (in-app onboarding screen)
- **Permissions:**
  - `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` (only if needed), `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`
  - `POST_NOTIFICATIONS`, `SYSTEM_ALERT_WINDOW` ("Display over other apps"), `NFC`
  - `INTERNET` (weather), `CAMERA` and `RECORD_AUDIO` (recording mode; both optional on the checklist,
    and asked for when recording mode is opened)
  - Notification access (the `NotificationListenerService` setting)
- **vivo/iQOO specific:**
  - Autostart ON for the app.
  - Battery → background power consumption → **Allow**.
  - Lock the app in Recents.
- The onboarding screen shows each item with a status tick and a button that opens the right settings page.

## 10. Project structure
```
ridedash/app/src/main/java/com/manish/ridedash/
  MainActivity.kt            // Compose host, kiosk handling
  ui/theme/                  // colours, type (Barlow), day/night tokens
  ui/dashboard/              // NavigatingPanel, CruisingPanel, SpeedGauge, StatusBar, BottomBar
  ui/onboarding/             // permission checklist
  ui/stats/                  // ride stats
  service/DashboardService.kt  // foreground service: location, sensors, triggers
  service/MapsNotificationListener.kt
  service/TriggerService.kt  // charger trigger
  service/DashboardTileService.kt
  overlay/SpeedOverlay.kt    // WindowManager + ComposeView, draggable
  data/RideRepository.kt     // StateFlow<RideState>
  data/sensors/              // LocationSource, LeanSource, BaroSource, LightSource, ...
  data/settings/             // DataStore
  data/weather/              // Open-Meteo fetch and parser
  record/                    // CameraX recording, the burned-in speed panel
  ui/record/                 // the recording screen
  nav/MapsParser.kt          // notification → NavState
  util/
```
- **`RideState`:** speed, gpsFix, satellites, tripKm, rideTime, maxSpeed, avgSpeed, heading, lean, leanMaxL, leanMaxR, altitude, battery, batteryTemp, night, nav (NavState?).

## 11. Milestones (one at a time, test on the phone after each)
1. **Project skeleton:** Compose, theme tokens and fonts, landscape lock, keep screen on, a static Navigating screen.
2. **Speed gauge + GPS speed live:** the status bar with GPS fix and battery.
3. **Cruising screen:** heading, lean (with calibration), altitude, max/avg. Day/night auto-switch.
4. **Maps notification listener:** log the samples, then the parser and the live Navigating screen.
5. **Map view:** launch Maps, add the draggable speed overlay, tap to return.
6. **Watch turn alerts:** the notification channel.
7. **Dashboard mode:** kiosk / screen pinning, Hold to exit, NFC trigger, charger trigger, onboarding checklist.
8. **Polish:** heat warning, ride stats screen, persist trip data (DataStore/Room).
9. **Weather:** Open-Meteo, the two tiles, the rain warning.
10. **Key-on sweep:** the gauge flourish on entering dashboard mode.
11. **Recording mode:** CameraX plus the burned-in speed panel.

Milestones 1–7 and 9–11 are implemented, and 8 apart from the full trip history. See the status table
in `ridedash/README.md` for what still needs checking on the bike.

## 12. Testing tips
- USB debugging: `adb devices`, then run from Android Studio. Wireless: `adb pair` / `adb connect`.
- **Bench-test GPS** with a mock-location app: Developer options → Select mock location app.
- **Navigation:** start any Google Maps route while walking or with mock location, and check `adb logcat -s RideDash/MapsRaw`.
- **Sunlight test:** outdoors at midday. Check that every value is readable at arm's length.

## 13. Out of scope for now
- **Continuous dashcam** (loop recording, incident capture, always-on) is still out. Recording mode
  (4.5) is the different thing: short clips the rider starts on purpose to talk about a route.
- TPMS (not wanted), bike RPM/gear/fuel.
- **Possible future:** an ESP32 + BLE bridge for bike signals (RPM, neutral, indicators, battery voltage).

## 14. Safety and legal notes
- The bike's own cluster stays the legal speedometer. GPS speed is for reference.
- No typing or small controls while riding. All touch targets are at least 48 dp, and critical actions need a long-press.
- **Mount:** use a vibration damper, which protects the camera's OIS. Add a sun visor or hood against heat.
- **Power:** the bike's USB-C port.
