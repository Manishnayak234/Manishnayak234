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

Requirements: Android Studio (Ladybug or newer), JDK 17 or 21, SDK platform 35 and build-tools 35,
a phone with USB debugging on. There is no emulator in the plan — the sensors and the sunlight are the
point.

**Gradle 8.11.1 will not run on a JDK newer than 23**, and a current Android Studio bundles JBR 25, so
a build started from Studio fails with a bare `25.0.3` error until you point it at an older JDK:
Settings → Build, Execution, Deployment → Build Tools → Gradle → **Gradle JDK → 21**. From the command
line, set `JAVA_HOME` to a JDK 21:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

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
    BrightnessSlider.kt        the brightness strip, on every dashboard layout
    CompactDashboard.kt        the half-width layout used in split screen
    RainTile.kt                rain for the next five hours, on the cruising screen
    StartupBadge.kt            the SPEED 400 badge, the riding scene and the bike drawing
  ui/onboarding/               the setup checklist
  ui/stats/                    ride stats (v1: this ride and the last one)
  service/DashboardService.kt  foreground service: location, sensors, overlay, watch alerts
  service/MapsNotificationListener.kt   reads the Maps notification
  service/TriggerService.kt    charger trigger (ACTION_POWER_CONNECTED, registered at runtime)
  service/DashboardTileService.kt       Quick Settings tile
  overlay/SpeedOverlay.kt      the draggable speed box drawn over Google Maps
  data/RideRepository.kt       StateFlow<RideState>, the one source of truth
  data/sensors/                location, lean, heading, barometer, light, battery, Bluetooth
  data/weather/                rain forecast: Open-Meteo fetch and its parser
  data/OverspeedGate.kt        the 80 km/h warning, with hysteresis
  data/SweetSpotGate.kt        the 65-70 km/h band
  data/LeanCheerGate.kt        the lean cheers, and the latch that makes them readable
  data/RideMotion.kt           one intensity behind the road, the trees and the dust
  data/DebugSpeed.kt           bench speed simulator, debug builds only
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
| 4 | Maps notification listener, parser, live Navigating screen | done — parser built from real samples off the phone; **no turn sample captured yet** |
| 5 | Map view: launch Maps, draggable speed overlay, tap to return | done, seen working on the phone |
| 5b | Split screen: compact dashboard beside Google Maps | done |
| 6 | Watch turn alerts on their own channel | done |
| 7 | Dashboard mode: pinning, Hold to exit, NFC and charger triggers, onboarding | done |
| 9 | Rain forecast for the next five hours | done, fetching live on the phone |
| 11 | "Slow down buddy" over 80 km/h | done, seen on the bench with the speed simulator |
| 12 | Riding scene: bike, road, dust, trees; badge morph; rider | done |
| 13 | Sweet spot at 65-70, lean cheers past 15 degrees | done |
| 10 | Power-on gauge sweep, as a cluster does | done |
| 8 | Polish: heat warning, ride stats, persisted trip data | heat warning and stats done; full trip history is v2 |

## What has been checked, and what has not

The **Android build now runs clean**: `assembleDebug`, `test` and `lint` all pass on JDK 21 against
SDK 35, so everything here has been through AGP 8.7.3, the Compose compiler and lint. The debug APK
builds at about 27 MB. The unit tests are 43 over the Maps parser, maneuver progress, watch-alert
glyphs, trip maths, speed smoothing, heading wrap-around and all the number formatting, with no
failures.

Lint is at **zero errors**. Getting there fixed three real problems the first compile surfaced:

- `TurnAlerts` and `TriggerService` posted notifications without checking `POST_NOTIFICATIONS`, so on
  Android 13+ a missing permission would have silently swallowed every watch alert. Both now check and
  log instead.
- `androidx.fragment` resolved transitively to 1.1.0, which predates the Activity Result APIs that
  `MainActivity` uses. Pinned forward to 1.8.5.
- The clock's `produceState` is now a plain `remember` + `LaunchedEffect`, which says the same thing
  without tripping Compose's lint check.

What remains is 38 lint warnings, all benign: mostly newer library versions being available, three
unused strings, and the fixed landscape orientation that this app exists to have.

It has **never been on a phone**. Nothing below the pure-logic layer — GPS, the sensors, the Maps
notification, the overlay, pinning — has been exercised against real hardware.

## Things to check on the real bike

- **The Maps parser has seen this phone's Maps, but only at the start of a route.** The captured
  shape is a bare or empty `title`, the command in `text` ("Head southwest"), and
  `subText` = "6 min · 3.1 km · 3:58 am ETA". Those samples are in `MapsParserTest`. What has *not*
  been seen is an actual turn — "Turn left onto MG Road" is covered by tests written from the same
  shape, not from a log. Ride a route and watch `adb logcat -s RideDash/MapsRaw`, then add whatever
  comes out to `MapsParserTest` before touching `MapsParser`. The switch that keeps that logging on is
  `ALWAYS_LOG_RAW` in `MapsNotificationListener`.
- **Google Maps notifications must be allowed**, or there is no Navigating screen at all — the
  listener has nothing to read and the dashboard stays on the Cruising tiles for ever. On the test
  phone they were blocked by default (`importance=NONE`), which looks exactly like a broken parser.
- **Screen pinning blocks everything else.** It is what stops a glove leaving the dashboard, but it
  also blocks split screen, vivo's Small Window, and any other app being launched — a pinned app
  refuses those with `START_RETURN_LOCK_TASK_MODE_VIOLATION`. Pinning is skipped automatically in
  split screen for that reason.
- **Heading is read off the back of the phone, not its top.** Confirmed correct in the mount. It only
  applies below 5 km/h — above that `LocationSource` hands over to the GPS bearing — and a phone
  lying flat deliberately falls back to the top-edge reading, so heading behaves differently on a
  bench than on the bars. If it ever reads consistently 90° or 180° out, the axis sign in
  `OrientationMath.headingDeg` is the thing to flip; if it wanders, calibrate the magnetometer and
  suspect the mount or the bike's electrics before the code.
- **Lean sign.** Confirmed correct in the mount. If lean ever reads mirrored, flip
  `LeanSource.LEAN_SIGN` to `-1f`. Zero
  it with a long press on the LEAN tile while the bike is upright — and remember a bar-mounted phone
  turns with the steering, so lean is approximate by construction.
- **Screen pinning and Maps.** Pinning is what stops a glove or a pocket leaving the dashboard, and it
  also blocks switching apps, so the Map button lifts pinning, opens Maps, and pinning goes back on
  when you return. The first time, Android asks you to confirm pinning.
- **Brightness.** The app never forces brightness; the phone's sunlight boost only works with auto
  brightness on, so leave it on.
- **Heat.** Battery temperature is shown in the status bar once it passes 45 °C. In direct sun, a hood
  over the phone matters more than anything in software.

## Rain

The only forecast worth a glance from a saddle is whether it is about to rain, so that is all this
shows: the first hour that crosses 40%, in accent, with five hourly bars behind it and the current
temperature. `DRY` when the window stays clear, and the whole tile greys out once the forecast is
over 90 minutes old rather than quietly presenting stale numbers as current.

It comes from **Open-Meteo**, which needs no API key, no cloud project and no billing account — the
whole feature is a URL and a parser, so there is no HTTP or JSON dependency in the app. `INTERNET` is
the only permission it added. A request goes out when the last one ages past 30 minutes or the bike
has moved about 15 km, and once immediately on the first GPS fix; mobile data on a ride is not free.

Android stubs `org.json` in JVM unit tests, so the real implementation is on the **test** classpath
only (`testImplementation(libs.org.json)`). It is not in the APK.

The tile is on the cruising screen only — the navigating panel has no room for it at 361 dp — so the
**status bar carries the next two hours** on every screen: a droplet and a percentage, grey until it
reaches 40% and accent above. It is shown even at 4%, deliberately: hiding a low number saves a
little clutter and costs the ability to tell "no rain coming" apart from "the forecast is broken".
That confusion cost real time once already. The indicator disappears entirely once the forecast goes
stale rather than presenting old numbers as current.

The forecast call runs in its own coroutine, never on the service tick. Awaiting it inline stalled
the one-second tick for as long as the request hung, which is worst exactly where a rider has no
signal — and a dropped GPS fix would not have greyed out until it returned.

## Over 80

The speed number, the gauge arc and the line under the gauge all turn red together, and that line
becomes SLOW DOWN BUDDY. The warning takes the trip line's place rather than adding a banner: same
spot, no layout shift, and nothing new covering the turn arrow. It reaches the split-screen layout
and the floating box over Maps too, since that box is the only speed visible with the map up.

`OverspeedGate` holds it on from 80 km/h down to 76 rather than switching on one number. A bare
comparison would strobe the screen every time the throttle breathed at the limit, and a warning that
blinks is one you stop reading. A speed the app does not trust never trips it, and losing the fix
clears a warning already up.

It is suppressed during the power-on sweep, which runs to 180 by design.

## The cluster it pretends to be

At key-on the gauge sweeps 0 to 180 and back while a SPEED 400 badge holds the right panel. When the
sweep ends the badge does not cut away: its bike shrinks and flies into the riding band while the
dashboard fades up underneath, so the big drawing and the small one read as one object being set
down. The morph's target is computed from the band's own published geometry, not eyeballed, so
resizing the band cannot leave the bike landing beside it.

Screen pinning waits for all of that. Android puts two dialogs up when lock task starts and they
cover the gauge completely, so the sweep runs first on a clear screen and pinning follows. Waiting
for window focus was the obvious answer and does not work here: a vivo SystemUI window holds the
focus on this phone, so `onWindowFocusChanged` never fires at all.

The band itself is a side-on riding scene: the bike never moves, the road slides under it, dust
trails off the exhaust and a treeline drifts past at a fifth of the road's rate. That parallax is
the whole illusion of depth. Everything is driven from one intensity in `RideMotion`, so the road,
the trees and the smoke can never disagree about how hard the bike is working — and parked means
parked: solid road line, no dust.

The rider sits up, because a Speed 400 is a roadster and not a supersport. Accent yellow is spent on
the visor, one shoulder and the dust, and nowhere else; the speed arc has to keep owning that colour.

## Things the dashboard says

One line under the gauge carries three states, in the trip readout's place so nothing shifts:

| | |
|---|---|
| normal | `12.4 km · 0:38` |
| 65-70 km/h | `SWEET SPOT`, in green |
| over 80 km/h | `SLOW DOWN BUDDY!`, in red, with the arc and number to match |

Past 15 degrees of lean the LEAN tile's own label is replaced for three seconds — `WAH BETE WAH,
MAUJ KARDI` to the left, `BADE HEAVY DRIVER HO` to the right. It latches deliberately: nobody reads
text mid-corner at fifteen degrees, so the cheer arrives on the way out, which is the only moment it
could be read.

All three are gates with hysteresis rather than plain comparisons. The sweet spot band is five km/h
wide and the overspeed threshold sits where a throttle breathes; without the slack they would strobe,
and a message that blinks is one you stop reading.

## Testing at speed without riding

Debug builds take a simulated speed over adb, so the things that only happen at speed can be seen on
a bench: the overspeed warning, the sweet spot, the road and dust, the switch from compass to GPS
bearing.

```bash
adb shell am broadcast -p com.manish.ridedash -a com.manish.ridedash.DEBUG_SPEED --ef kmh 95
adb shell am broadcast -p com.manish.ridedash -a com.manish.ridedash.DEBUG_SPEED --ez off true
```

It publishes on the service tick rather than on GPS updates, so it works indoors with no fix.
`DebugSpeed.set` is fenced behind `BuildConfig.DEBUG`: a release build has no path to a speed that
did not come from the GPS, whatever is broadcast at it.

## Screen sizes

The mockup was drawn for 914 x 412 dp. The phone this runs on is 440 dpi and reports **801 x 361 dp**
once the system bars are out, which is 113 dp narrower and 51 dp shorter. Three things were clipped off
the screen before that was noticed: the trip line under the gauge, the whole ETA row on the Navigating
panel, and, in split screen, everything. The gauge now sizes itself from the height it is given, the
Navigating panel's type was retuned, and anything under 560 dp wide gets `CompactDashboard` instead.
If the layout is ever changed, check it at 361 dp of height, not at the mockup's 412.

## Not built yet

- Trip history beyond the last ride (Room or DataStore, v2), which is what the Ride stats screen is
  waiting for.
- Bike-side signals (RPM, gear, neutral, indicators). That needs the ESP32 + BLE bridge from the
  brief's "possible future" list, and nothing here assumes it.

## Licences

Barlow and Barlow Condensed are bundled under the SIL Open Font Licence; the licence text is in
[`licenses/Barlow-OFL.txt`](licenses/Barlow-OFL.txt).
