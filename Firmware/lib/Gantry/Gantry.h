#pragma once
#include "Stepper.h"

class Gantry {

    public:

    Gantry(int leftStep, int leftDir, int rightStep, int rightDir, int enablePin, int xLimitPin, int yLimitPin);
    void setPosition(double x, double y);
    void setSquare(int index);
    void initialize();
    void enable();
    void disable();




    private:
    int xLimitPin;
    int yLimitPin;
    //2mm belt pitch, 16 tooth pulley on motors
    static constexpr double mmPerRot = 2 * 16;
    static constexpr double microSteps = 2;  
    static constexpr double stepsPerRev = 200;
    static constexpr double mmPerStep = mmPerRot / stepsPerRev / microSteps;

    // mm/s and mm/s^2 respectively
    static constexpr double maxSpeed = 150;
    static constexpr double accel = 400;

    static const int squareSize = 55;

    //TODO: Actually find these :(
    static const int xZeroOffset = 25;
    static const int yZeroOffset = 25;

    Stepper leftStepper;
    Stepper rightStepper;

    double beltA = 0;
    double beltB = 0;


    double xPosMM = 0;
    double yPosMM = 0;

    void update();
    void zero();
};