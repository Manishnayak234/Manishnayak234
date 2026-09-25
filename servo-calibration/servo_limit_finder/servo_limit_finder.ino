/*
 * servo_limit_finder - find the safe mechanical travel of one servo.
 *
 * Wiring: signal on D9, servo ground tied to Arduino ground.
 * Power the servo from its own 5V supply, not the Arduino 5V pin; a stalled
 * servo pulls enough current to brown out the board mid-measurement.
 *
 * Serial Monitor: 9600 baud, line ending "No line ending".
 *
 * Commands
 *   0..180   move there, one degree at a time
 *   +  -     nudge 5 degrees
 *   >  <     nudge 1 degree
 *   c        centre (90)
 *   d        detach - cuts the signal, use the moment it buzzes
 *   a        re-attach at the last commanded angle
 *   ?        print current angle
 *
 * Procedure: from 90, walk down in 10 degree steps until the horn touches
 * something or the servo buzzes. Press 'd' immediately, back off 5 degrees,
 * 'a', and note that angle as the minimum. Repeat upwards for the maximum.
 */

#include <Servo.h>

const int SERVO_PIN = 9;
const int START_ANGLE = 90;
const int STEP_DELAY_MS = 15;   // per degree; raise it to move more gently
const int IDLE_MS = 80;         // end of a typed number, with no line ending

Servo servo;
int angle = START_ANGLE;
bool attached = false;

char digits[4];
byte digitCount = 0;
unsigned long lastDigitMs = 0;

void attachServo() {
  servo.attach(SERVO_PIN);
  servo.write(angle);
  attached = true;
}

void detachServo() {
  servo.detach();
  attached = false;
  Serial.println(F("detached"));
}

// Walk to the target so a hard stop is met slowly rather than hit at speed.
void moveTo(int target) {
  if (target < 0 || target > 180) {
    Serial.println(F("out of range"));
    return;
  }
  if (!attached) attachServo();

  int step = (target > angle) ? 1 : -1;
  while (angle != target) {
    angle += step;
    servo.write(angle);
    delay(STEP_DELAY_MS);
  }
  Serial.println(angle);
}

void handleCommand(char c) {
  switch (c) {
    case '+': moveTo(angle + 5); break;
    case '-': moveTo(angle - 5); break;
    case '>': moveTo(angle + 1); break;
    case '<': moveTo(angle - 1); break;
    case 'c': moveTo(START_ANGLE); break;
    case 'd': detachServo(); break;
    case 'a': attachServo(); Serial.println(angle); break;
    case '?': Serial.println(angle); break;
    default: break;
  }
}

void setup() {
  Serial.begin(9600);
  attachServo();
  Serial.println(F("ready - type an angle, or + - > < c d a ?"));
  Serial.println(angle);
}

void loop() {
  while (Serial.available()) {
    char c = Serial.read();
    if (c >= '0' && c <= '9') {
      if (digitCount < 3) digits[digitCount++] = c;
      lastDigitMs = millis();
    } else if (c != '\r' && c != '\n' && c != ' ') {
      handleCommand(c);
    }
  }

  if (digitCount > 0 && millis() - lastDigitMs > IDLE_MS) {
    digits[digitCount] = '\0';
    digitCount = 0;
    moveTo(atoi(digits));
  }
}
