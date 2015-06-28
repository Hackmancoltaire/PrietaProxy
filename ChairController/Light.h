#define maxBrightness 150
#define pulseLength 5

enum animation {
  on,
  off,
  blink,
  fade,
  pulse
};


class Light {
  private:
    byte pulsesLeft;
    byte blinkRate = 5;

  public:
    byte currentBrightness;
    byte currentAnimation;
    byte animationTimer;
    byte pulseCount;


    Light() {
      currentAnimation = off;
      animationTimer = 0;
      pulseCount = 0;
      pulsesLeft = 0;
    }

    void animate() {
      switch (currentAnimation) {
        case on:
          currentBrightness = maxBrightness; break;
        case off:
          currentBrightness = 0; break;
        case blink:
          if (animationTimer > blinkRate) {
            toggleSwitch();
            animationTimer = 0;
          } else {
            animationTimer++;
          }
          break;
        case fade:
          if (isOn()) {
            animationTimer = 1;
          } else if (isOff()) {
            animationTimer = 0;
          }

          if (animationTimer == 0) {
            currentBrightness += 8;
          }
          else {
            currentBrightness = currentBrightness - 8;
          }
          break;
        case pulse:
          if (pulsesLeft > 0) {
            // There are still pulses to do
            if (animationTimer == 0) {
              // A timer just finished

              // If we're on
              if (isOn()) {
                // Turn light off and reduce pulses.
                toggleSwitch();
                pulsesLeft--;

                // Set delay for 10 units.
                if (pulsesLeft > 0) {
                  animationTimer = pulseLength;
                } else {
                  animationTimer = pulseLength * 3;
                }
              } else {
                // We've been off for 10 units
                // Turn light back on and reset timer
                toggleSwitch();
                animationTimer = pulseLength;
              }
            } else {
              animationTimer--;
            }
          } else {
            // There are no more pulses left
            if (animationTimer == 0) {
              pulsesLeft = pulseCount;
              toggleSwitch();
              animationTimer = pulseLength;
            } else {
              animationTimer--;
            }
          }

          break;

        default: break;
      }
    }

    boolean isOn() {
      return (currentBrightness >= maxBrightness);
    }

    boolean isOff() {
      return (currentBrightness <= 0);
    }

    void toggleSwitch() {
      if (isOn()) {
        currentBrightness = 0;
      } else {
        currentBrightness = maxBrightness;
      }
    }

    void setBrightness(byte newBrightness) {
      currentBrightness = newBrightness;
    }

    void setAnimation(animation routine) {
      if (currentAnimation != routine) {
        currentAnimation = routine;
        animationTimer = 0;
      }
    }

    void setBlinkRate(byte rate) {
      setAnimation(blink);
      blinkRate = rate;
    }

    void setPulseCount(byte pulses) {
      if (currentAnimation != pulse) {
        setAnimation(pulse);
        pulseCount = pulses;

        if (isOn()) {
          pulsesLeft = pulses + 1;
        }
        else {
          pulsesLeft = pulses;
        }
      }
    }
};
