#pragma once
#include "Arduino.h"

class Stepper{
    public:
        Stepper(int stepPin, int dirPin, int enablePin);
        void disableMotor();
        void enableMotor();
        void setDirection(bool positive);
        void step();
        void begin();
        long getSteps();
        void reset();


    private:
        int stepPin;
        int dirPin;
        int enablePin;
        long steps = 0;
        bool posDir = true;
};
