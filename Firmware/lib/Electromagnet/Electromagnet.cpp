#include "Electromagnet.h"

Electromagnet::Electromagnet(int pin1, int pin2, double dutyCycle) : pin1(pin1), pin2(pin2), dutyCycle(dutyCycle) {}

void Electromagnet::initialize() {
    pinMode(pin1, OUTPUT);
    pinMode(pin2, OUTPUT);

    off();
}

void Electromagnet::on(bool whitePiece) {
    if(whitePiece) {
        analogWrite(pin1, (int)(255*dutyCycle));
        analogWrite(pin2, 0);
    } else {
        analogWrite(pin1, 0);
        analogWrite(pin2, (int)(dutyCycle*255));
    } 
}

void Electromagnet::off() {
    analogWrite(pin1, 0);
    analogWrite(pin2, 0);
}