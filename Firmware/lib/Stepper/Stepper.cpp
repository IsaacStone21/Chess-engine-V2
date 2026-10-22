#include "Stepper.h"

//in microseconds
#define minStepTime 2
#define dirSwitchDownTime 1


Stepper::Stepper(int stepPin, int dirPin, int enablePin) {
    this->stepPin = stepPin;
    this->dirPin = dirPin;
    this->enablePin = enablePin;
}

void Stepper::begin() {
    pinMode(stepPin, OUTPUT);
    pinMode(dirPin, OUTPUT);
    pinMode(enablePin, OUTPUT);

    digitalWrite(dirPin, HIGH);
}

void Stepper::step() {
    digitalWrite(stepPin, HIGH);
    delayMicroseconds(minStepTime);
    digitalWrite(stepPin, LOW);
    delayMicroseconds(minStepTime);
    if(posDir) {
        steps++;
    } else {
        steps--;
    }
}

void Stepper::setDirection(bool positive) {
    digitalWrite(dirPin, positive ? HIGH : LOW);
    delayMicroseconds(dirSwitchDownTime);
    posDir = positive;
}

// DRV8825 ENABLE pin is active low: LOW = driver on, HIGH = driver off
void Stepper::disableMotor() {
    digitalWrite(enablePin, HIGH);
}

void Stepper::enableMotor() {
    digitalWrite(enablePin, LOW);
}

long Stepper::getSteps() {
    return steps;
}

void Stepper::reset() {
    steps = 0;
    enableMotor();
    setDirection(true);
}