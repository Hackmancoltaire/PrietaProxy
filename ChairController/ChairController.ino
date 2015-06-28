#include <TimerOne.h>
#include "Light.h"

// Update TICKER_PWM_LEVELS to control how many levels you want
// usually 32 levels of brightess is good (32K colors)
#define TICKER_PWM_LEVELS 32
#define TICKER_STEP 256/TICKER_PWM_LEVELS

// Update TIMER_DELAY to control how fast this runs.
// As long as you don't see flicker ... a higher number (slower) is better to assure
// the loop has room to do stuff
#define TIMER_DELAY 280
#define SR_COUNT 2

// Used in iProcess to control the software PWM cycle
int ticker = 0;

// Shift register pins
int latchPin = 10;
int clockPin = 13;
int dataPin = 11;

//--- Used for faster latching
int latchPinPORTB = latchPin - 8;

//--- Holds a 0 to 255 PWM value used to set the value of each SR pin
Light lights[SR_COUNT * 8];

#include "PWM.h"

enum lightNames {
  lowPower,
  mediumPower,
  highPower,
  homing,
  mine,
  ecm,
  nuke,
  loading,
  weaponLock,
  redAlert,
  shields,
  propulsion,
  jumpDrive,
  comms
};

void setup() {
  Serial.begin(115200);

  pinMode(latchPin, OUTPUT);
  pinMode(clockPin, OUTPUT);
  pinMode(dataPin, OUTPUT);

  digitalWrite(latchPin, LOW);
  digitalWrite(dataPin, LOW);
  digitalWrite(clockPin, LOW);

  //--- Setup to run SPI
  setupSPI();

  //--- Activate the PWM timer
  Timer1.initialize(TIMER_DELAY); // Timer for updating pwm pins
  Timer1.attachInterrupt(iProcess);
}

//--- Very simple demo loop
void loop() {
  if (Serial.available() > 0) {
    char command = Serial.read();

    switch (command) {
      // Power
      case 'x': // NO POWER
        lights[lowPower].setBlinkRate(5);
        lights[mediumPower].setAnimation(off);
        lights[highPower].setAnimation(off);
        break;
      case 'L': // Low power max
        lights[lowPower].setAnimation(on);
        lights[mediumPower].setAnimation(off);
        lights[highPower].setAnimation(off);
        break;
      case 'l': // Low power fading
        lights[lowPower].setAnimation(fade);
        lights[mediumPower].setAnimation(off);
        lights[highPower].setAnimation(off);
        break;
      case 'M': // Medium power max
        lights[mediumPower].setAnimation(on);
        lights[highPower].setAnimation(off);
        lights[lowPower].setAnimation(on);
        break;
      case 'm': // Medium power fading
        lights[mediumPower].setAnimation(fade);
        lights[highPower].setAnimation(off);
        lights[lowPower].setAnimation(on);
        break;
      case 'H': // High poewr max
        lights[highPower].setAnimation(on);
        lights[mediumPower].setAnimation(on);
        lights[lowPower].setAnimation(on);
        break;
      case 'h': // High power fading
        lights[highPower].setAnimation(fade);
        lights[mediumPower].setAnimation(on);
        lights[lowPower].setAnimation(on);
        break;

      // Torpedoes
      case 'T': //Homing Loaded
        lights[homing].setAnimation(on); break;
      case 't': // Homing count
        if (Serial.available() > 0) {
          byte count = Serial.parseInt();
          lights[homing].setPulseCount(count);
        }
        break;
      case 'I': //Mine Loaded
        lights[mine].setAnimation(on); break;
      case 'i': // Mine count
        if (Serial.available() > 0) {
          byte count = Serial.parseInt();
          lights[mine].setPulseCount(count);
        }
        break;
      case 'E': //ECM Loaded
        lights[ecm].setAnimation(on); break;
      case 'e': // ECM count
        if (Serial.available() > 0) {
          byte count = Serial.parseInt();
          lights[ecm].setPulseCount(count);
        }
        break;
      case 'N': //Nuke Loaded
        lights[nuke].setAnimation(on); break;
      case 'n': // Nuke count
        if (Serial.available() > 0) {
          byte count = Serial.parseInt();
          lights[nuke].setPulseCount(count);
        }
        break;

      // Loading
      case 'O': lights[loading].setAnimation(on); break;
      case 'o': lights[loading].setAnimation(fade); break;

      // Targeting
      case 'A': lights[weaponLock].setAnimation(on); break;
      case 'a': lights[weaponLock].setAnimation(off); break;

      // Red Alert
      case 'R': lights[redAlert].setAnimation(fade); break;
      case 'r': lights[redAlert].setAnimation(off); break;

      // Shields
      case 'S': lights[shields].setAnimation(on); break;
      case 's': lights[shields].setAnimation(off); break;
      
      // Movement
      case 'U': lights[propulsion].setAnimation(on); break;
      case 'u': lights[propulsion].setAnimation(off); break;
      
      // Jump Drive
      case 'J': lights[jumpDrive].setAnimation(on); break;
      case 'j': lights[jumpDrive].setAnimation(off); break;

      // Ignore newlines
      case '\n': break;
      default: Serial.println("!" + String(command)); break;
    }
  }

  for (byte i = 0; i < 16; i++) {
    lights[i].animate();
    delay(5);
  }
}
