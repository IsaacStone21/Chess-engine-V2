#pragma once

#include "Arduino.h"

class Electromagnet {
    public:
    // 2 pins for an H-bridge allowing dual polarity
    Electromagnet(int pin1, int pin2, double dutyCycle);
    void initialize();
    void on(bool whitePiece);
    void off();


    private:
    double dutyCycle;
    int pin1;
    int pin2;

};