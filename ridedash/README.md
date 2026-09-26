# RideDash

An Android handlebar dashboard for a Triumph Speed 400, built to run on an old iQOO 9 SE bolted to
the bars. It is an **add-on**, not a cluster replacement: the bike's own cluster stays the legal
speedometer, and everything here comes from the phone's own sensors plus whatever Google Maps puts in
its navigation notification.

The full design brief lives in [`../CLAUDE.md`](../CLAUDE.md) at the repository root. This file covers
building it, running it and what is finished.

## Two screens, one dashboard

| | |
|---|---|
| **Navigating** (a route is running) | Speed gauge on the left, maneuver arrow, distance to the turn, street, progress bar, "Then …" and the ETA / LEFT / TIME row on the right. |
| **Cruising** (no route) | Same gauge, and a 3×2 grid: heading, lean, weather, altitude, max/avg speed, rain and wind. |

The switch between them is automatic: it follows whether the Google Maps ongoing notification is
there.

Two more things the dashboard does:

- **Key-on sweep.** Entering dashboard mode runs the gauge 0 → 180 → 0 before live speed takes over,
  the way the bike's own cluster does when the ignition comes on. A tap skips it. It is also a
  self-test: you see the whole arc and every label before setting off.
- **Recording mode** (the **REC** button). The camera records at sensor quality and the speed panel is
  composited into the frames as they go past, so one file has you on one side and the numbers on the
  other. Clips land in **Movies/RideDash**. Front camera by default, switchable to the road; the
  microphone carries your commentary. It heats the phone, so expect the 45 °C warning sooner in the
  sun.

Weather comes from **Open-Meteo** — no API key and no account, which matters for an app that is
sideloaded rather than published. It refreshes every 20 minutes or after 5 km, and a failed fetch keeps
the last reading instead of blanking the tiles.

## Build and install

Requirements: Android Studio (Ladybug or newer), JDK 17, a phone with USB debugging on. There is no
emulator in the plan — the sensors and the sunlight are the point.

```bash
cd ridedash
./gradlew installDebug          # build and push to the connected phone
./gradlew test                  # the pure-logic unit tests (parser, trip maths, formatting)
./gradlew lint                  # Android lint
```

Wireless instead of a cable:

```bash
adb pair <phone-ip>:<pair-port>     # from Developer options → Wireless debugging
adb connect <phone-ip>:<port>
```

`local.properties` is not committed; Android Studio writes it with your SDK path on first open.

## First run on the phone

The launcher icon opens the setup checklist, which is also the home screen. Each row opens the exact
settings page that grants it. Three of them — autostart, background power and locking the app in
Recents — cannot be read or set by an app on Funtouch OS, so they are marked *by hand* and stay on the
list as a reminder. [`docs/SETUP.md`](docs/SETUP.md) has the whole walk-through, including the NFC tag
on the mount and how to bench-test with a mock location.

Once the essentials are green, **START DASHBOARD** takes the screen: landscape, full screen, screen
kept on, pinned. **Hold to exit** for two seconds gives the phone back.

## Where things are

```
app/src/main/java/com/manish/ridedash/
  MainActivity.kt              Compose host, dashboard mode, pinning, NFC and charger entry
  ui/theme/                    colour tokens (day/night), Barlow type scale
  ui/dashboard/                gauge, status bar, bottom bar, the two panels, drawn icons
  ui/onboarding/               the setup checklist
  ui/stats/                    ride stats (v1: this ride and the last one)
  service/DashboardService.kt  foreground service: location, sensors, overlay, watch alerts
  service/MapsNotificationListener.kt   reads the Maps notification
  service/TriggerService.kt    charger trigger (ACTION_POWER_CONNECTED, registered at runtime)
  service/DashboardTileService.kt       Quick Settings tile
  overlay/SpeedOverlay.kt      the draggable speed box drawn over Google Maps
  data/RideRepository.kt       StateFlow<RideState>, the one source of truth
  data/sensors/                location, lean, heading, barometer, light, battery, Bluetooth
  data/settings/               DataStore: lean zero, overlay position, triggers, last ride
  data/weather/                Open-Meteo fetch, parser, WMO code labels
  record/                      CameraX recording and the burned-in speed panel
  ui/record/                   the recording screen
  nav/                         MapsParser, progress tracking, watch turn alerts
  util/                        formatting, setup checks, notification channels
```

`RideState` is the whole dashboard in one immutable snapshot. The service writes it; the UI and the
overlay only read it.

## Milestone status

| # | Milestone | State |
|---|---|---|
| 1 | Project skeleton, theme, fonts, static Navigating screen | done |
| 2 | Speed gauge on live GPS, status bar with fix and battery | done |
| 3 | Cruising screen: heading, lean with calibration, altitude, max/avg, auto night | done |
| 4 | Maps notification listener, parser, live Navigating screen | done — **parser still needs real samples** |
| 5 | Map view: launch Maps, draggable speed overlay, tap to return | done |
| 6 | Watch turn alerts on their own channel | done |
| 7 | Dashboard mode: pinning, Hold to exit, NFC and charger triggers, onboarding | done |
| 8 | Polish: heat warning, ride stats, persisted trip data | heat warning and stats done; full trip history is v2 |
| 9 | Weather: Open-Meteo, the two tiles, the rain warning | done |
| 10 | Key-on sweep | done |
| 11 | Recording mode: CameraX with the speed burned in | done — **unproven on a phone** |

## Getting an APK without building it yourself

Every push to this branch builds a debug APK in GitHub Actions
([workflow](../.github/workflows/android.yml)). Open the latest run under the repository's **Actions**
tab, scroll to **Artifacts**, and download **`ridedash-debug-apk`** — GitHub wraps it in a zip, so
unzip it and install `app-debug.apk`, or `adb install` it from the laptop. The same run also uploads
**`ridedash-reports`** with the lint and unit-test HTML reports.

It is signed with a throwaway debug key, so uninstall it before installing a build from your own
Android Studio; two different signatures cannot sit on top of each other.

## What has been checked, and what has not

The build is green in CI: `assembleDebug` packages the APK, the unit tests pass (53 tests over the
Maps parser, maneuver progress, watch-alert glyphs, trip maths, speed smoothing, heading wrap-around,
the weather parser and the number formatting), and Android lint reports no errors.

What that does **not** cover is a phone. Nothing here has run on a device, so the sensors, the Maps
notification parsing, the overlay over Google Maps, screen pinning and the vivo-specific entry
triggers are all unproven in the only place that counts. Work down the list below on the bike,
milestone by milestone.

## Things to check on the real bike

- **The Maps parser is built from expected shapes, not from your phone's Maps.** Start a route and
  watch `adb logcat -s RideDash/MapsRaw`: every field of every Maps notification is logged verbatim.
  Compare against `MapsParserTest` and adjust `MapsParser` where the wording differs. The switch that
  keeps that logging on is `ALWAYS_LOG_RAW` in `MapsNotificationListener`.
- **Lean sign.** If lean reads mirrored in the real mount, flip `LeanSource.LEAN_SIGN` to `-1f`. Zero
  it with a long press on the LEAN tile while the bike is upright — and remember a bar-mounted phone
  turns with the steering, so lean is approximate by construction.
- **Screen pinning and Maps.** Pinning is what stops a glove or a pocket leaving the dashboard, and it
  also blocks switching apps, so the Map button lifts pinning, opens Maps, and pinning goes back on
  when you return. The first time, Android asks you to confirm pinning.
- **Brightness.** The app never forces brightness; the phone's sunlight boost only works with auto
  brightness on, so leave it on.
- **Heat.** Battery temperature is shown in the status bar once it passes 45 °C. In direct sun, a hood
  over the phone matters more than anything in software — and recording makes it worse, so treat long
  clips as a hot-weather compromise.
- **Recording framing.** The burned-in panel takes the left 40% of the frame. If you want the true
  half-and-half split, set `PANEL_FRACTION` to `0.5` in `record/SpeedPanelPainter.kt`; if you want more
  camera, take it down to `0.3`.
- **Weather with no signal.** The tiles keep the last reading rather than blanking, so a number on the
  weather tile may be twenty minutes and a few kilometres old. That is deliberate — stale is more use
  than empty.

## Not built yet

- Trip history beyond the last ride (Room or DataStore, v2), which is what the Ride stats screen is
  waiting for.
- Bike-side signals (RPM, gear, neutral, indicators). That needs the ESP32 + BLE bridge from the
  brief's "possible future" list, and nothing here assumes it.

## Licences

Barlow and Barlow Condensed are bundled under the SIL Open Font Licence; the licence text is in
[`licenses/Barlow-OFL.txt`](licenses/Barlow-OFL.txt).
