# Pan/tilt servo calibration

Finding the safe travel of each servo before any tracking code drives them.
Anything outside these limits stalls the servo against a mechanical stop: it
buzzes, draws stall current, heats up and strips its gears.

## Procedure

One servo at a time, signal on **D9**, servo ground tied to Arduino ground.
Power the servo from its own 5V supply — a stalled servo will brown out the
board if it is fed from the Arduino's 5V pin.

1. Flash `servo_limit_finder/servo_limit_finder.ino`.
2. Open Serial Monitor at **9600 baud**, line ending **"No line ending"**.
3. It starts centred at 90. Walk **down** in 10° steps: 80, 70, 60 …
4. Stop at the first touch or buzz, press **`d`** to cut the signal, then back
   off 5° and press **`a`**. That angle is the minimum.
5. Repeat **upwards**: 100, 110, 120 … for the maximum.
6. Move the other servo through its own full range at each extreme — the safe
   window for pan can depend on where tilt is sitting, and vice versa.
7. Repeat for the tilt servo.

### Commands

| Input | Effect |
| --- | --- |
| `0`–`180` | move there, 1° at a time |
| `+` / `-` | nudge 5° |
| `>` / `<` | nudge 1° |
| `c` | centre (90) |
| `d` | detach — cuts the signal, press this the moment it buzzes |
| `a` | re-attach at the last commanded angle |
| `?` | print current angle |

The sketch steps one degree at a time with a short delay, so a hard stop is
met slowly instead of hit at full speed. `d` is the emergency stop: a detached
servo holds no position and draws no stall current.

## Measured limits

Fill in once measured, then clamp every angle in the tracking firmware to
these values.

| Axis | Pin | Min | Max | Centre | Notes |
| --- | --- | --- | --- | --- | --- |
| Pan  | D9 | TBD | TBD | TBD | |
| Tilt | D9 | TBD | TBD | TBD | |

Keep a few degrees of margin inside the first angle that buzzed — a servo that
is just short of its stop under no load can still reach it once the camera
bracket is mounted and the rig is vibrating.
