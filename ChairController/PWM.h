//--- The really fast SPI version of shiftOut
byte spi_transfer(byte data) {
  SPDR = data;                    // Start the transmission
  loop_until_bit_is_set(SPSR, SPIF);
  return SPDR;                    // return the received byte, we don't need that
}

//--- Direct port access latching
void latchOn() {
  bitSet(PORTB, latchPinPORTB);
}
void latchOff() {
  bitClear(PORTB, latchPinPORTB);
}

//--- This process is run by the timer and does the PWM control
void iProcess() {
  //--- Create a temporary array of bytes to hold shift register values in
  byte srVals[SR_COUNT];
  //--- increment our ticker
  ticker++;
  //--- if our ticker level cycles, restart
  if ( ticker > TICKER_PWM_LEVELS )
    ticker = 0;
  //--- get ticker as a 0 to 255 value, so we can always use the same data regardless of actual PWM levels
  int myPos = ticker * TICKER_STEP;

  //--- Loop through all bits in the shift register (8 pin for the 595's)
  for (int i = 0 ; i < 8; i++ ) {
    int myLev = 0;
    //--- Loop through all shift registers and set the bit on or off
    for (int iSR = 0 ; iSR < SR_COUNT ; iSR++) {
      //--- Start with the bit off
      myLev = 0;
      //--- If the value in the sr pin related to this SR/Byte is over the current pwm value
      //     then turn the bit on
      if (lights[i + (iSR * 8)].currentBrightness > myPos)
        myLev = 1;
      //--- Write the bit into the SR byte array
      bitWrite(srVals[iSR], i, myLev );
    }

  }

  //--- Run through all the temporary shift register values and send them (last one first)
  // latching in the process
  latchOff();

  for (int iSR = SR_COUNT - 1 ; iSR >= 0 ; iSR--) {
    spi_transfer(srVals[iSR]);
  }

  latchOn();
}

//--- Used to setup SPI based on current pin setup
//    this is called in the setup routine;
void setupSPI() {
  byte clr;
  SPCR |= ( (1 << SPE) | (1 << MSTR) ); // enable SPI as master
  SPCR &= ~( (1 << SPR1) | (1 << SPR0) ); // clear prescaler bits
  clr = SPSR; // clear SPI status reg
  clr = SPDR; // clear SPI data reg
  SPSR |= (1 << SPI2X); // set prescaler bits
  delay(10);
}
