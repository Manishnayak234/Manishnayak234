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
| **Cruising** (no route) | Same gauge, and a 2×2 grid of heading, lean, altitude and max/avg speed. |

The switch between them is automatic: it follows whether the Google Maps ongoing notification is
there.

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

## What has been checked, and what has not

The whole pure-logic layer was compiled with Kotlin 2.0.21 and its unit tests run: 40 tests over the
Maps parser, maneuver progress, watch-alert glyphs, trip maths, speed smoothing, heading wrap-around
and all the number formatting. They pass.

The **Android build has not been run**: the container this was written in cannot reach the Android SDK
or Google's Maven repository, so nothing here has been through AGP, Compose's compiler or lint, and it
has never been on a phone. Treat the first `./gradlew installDebug` in Android Studio as the real first
compile — expect to fix the odd import or API detail, and do it milestone by milestone as the brief
says.

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
  over the phone matters more than anything in software.

## Not built yet

- Trip history beyond the last ride (Room or DataStore, v2), which is what the Ride stats screen is
  waiting for.
- Bike-side signals (RPM, gear, neutral, indicators). That needs the ESP32 + BLE bridge from the
  brief's "possible future" list, and nothing here assumes it.

## Licences

Barlow and Barlow Condensed are bundled under the SIL Open Font Licence; the licence text is in
[`licenses/Barlow-OFL.txt`](licenses/Barlow-OFL.txt).
