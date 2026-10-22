#include <Arduino.h>
#include "Gantry.h"
#include "Electromagnet.h"

#define leftStepPin 2
#define leftDirPin 3
#define rightStepPin 4
#define rightDirPin 5
#define motorEnablePin 6
#define xLimitPin 8
#define yLimitPin 9
#define magnetPin1 7
#define magnetPin2 22
#define magnetDutyCycle 0.65

Gantry gantry = Gantry(leftStepPin, leftDirPin, rightStepPin, rightDirPin, motorEnablePin, xLimitPin, yLimitPin);
Electromagnet magnet = Electromagnet(magnetPin1, magnetPin2, magnetDutyCycle);

void setup() {
  magnet.initialize();
  gantry.initialize();
}

void loop() {
  // put your main code here, to run repeatedly:
}
