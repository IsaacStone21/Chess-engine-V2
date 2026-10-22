#include "Gantry.h"
#include <cmath>



    Gantry::Gantry(int leftStep, int leftDir, int rightStep, int rightDir, int enablePin, int xLimitPin, int yLimitPin)
        : leftStepper(leftStep, leftDir, enablePin),
        rightStepper(rightStep, rightDir, enablePin), xLimitPin(xLimitPin), yLimitPin(yLimitPin) {}



    void Gantry::initialize() {
        rightStepper.begin();
        leftStepper.begin();
        enable();
        pinMode(yLimitPin, INPUT);
        pinMode(xLimitPin, INPUT);

        zero();
    }

    void Gantry::enable() {
        leftStepper.enableMotor();
        rightStepper.enableMotor();
    }

    void Gantry::disable() {
        leftStepper.disableMotor();
        rightStepper.disableMotor();
    }

    void Gantry::setPosition(double xtarget, double yTarget) {
        update();

        double xDist = xtarget - xPosMM;
        double yDist = yTarget - yPosMM;

        double angle = std::atan2(yDist, xDist) * 180 / 3.1415;

        if(angle < 45 && angle > -135) {
            leftStepper.setDirection(false);
        } else {
            leftStepper.setDirection(true);
        }

        if(angle > -45 && angle < 135) {
            rightStepper.setDirection(false);
        } else {
            rightStepper.setDirection(true);
        }

        long leftSteps = std::round(std::abs(((yDist - xDist) *stepsPerRev * microSteps) / mmPerRot));
        long rightSteps = std::round(std::abs(((-yDist - xDist) * stepsPerRev * microSteps) / mmPerRot));


        long steps;
        double ratio;
        
        Stepper* majorMotor;
        Stepper* minorMotor;

        if(leftSteps > rightSteps) {
            steps = leftSteps;
            majorMotor = &leftStepper;
            minorMotor = &rightStepper;
            ratio = (double)rightSteps / leftSteps;

        } else {
            steps = rightSteps;
            majorMotor = &rightStepper;
            minorMotor = &leftStepper;
            ratio = (double)leftSteps / rightSteps;
        }

        double scale = std::sqrt(2 / (1 + ratio * ratio));
        double majorAccel = accel * scale;
        double majorMaxSpeed = maxSpeed * scale;

        //BresenHam's Line algorithym

        long minorSteps = (majorMotor == &leftStepper) ? rightSteps : leftSteps;
        long D = 2 * minorSteps - steps;
        long startMicros = micros();
        double scheduledMicros = 0;

        for(long i = 0; i < steps; i++) {
            // Trapezoidal Profile
            double desiredVelocity1 = std::sqrt(2 * majorAccel * (i + 0.5) * mmPerStep);
            double desiredVelocity2 = std::sqrt(2 * majorAccel * (((steps - (i + 0.5)) * mmPerStep)));

            // mm/s
            double trueDesiredVelocity = std::min(desiredVelocity1, std::min(desiredVelocity2, majorMaxSpeed));


            scheduledMicros += mmPerStep / (trueDesiredVelocity * std::pow(10, -6));
            while ((double)(uint32_t)(micros() - startMicros) < scheduledMicros) {}

            majorMotor->step();

            if(D >=0) {
                D -= 2*steps;
                minorMotor->step();
            }
            D += 2*minorSteps;
        }
    }

    void Gantry::zero() {
        leftStepper.setDirection(false);
        rightStepper.setDirection(true);

        bool hitLimitSwitch = digitalRead(yLimitPin) == HIGH;
        int stepCount = 0;

        //not sure if 100000 is too high or too low
        while (!hitLimitSwitch && stepCount < 100000) {
            leftStepper.step();
            rightStepper.step();
            delayMicroseconds(1000);


            hitLimitSwitch = digitalRead(yLimitPin) == HIGH;
            stepCount++;
        }

        hitLimitSwitch = digitalRead(xLimitPin) == HIGH;
        stepCount = 0;
        leftStepper.setDirection(true);

        while(!hitLimitSwitch && stepCount < 100000) {
            leftStepper.step();
            rightStepper.step();

            delayMicroseconds(1000);

            hitLimitSwitch = digitalRead(xLimitPin) == HIGH;
            stepCount++;
        }
        leftStepper.reset();
        rightStepper.reset();
    }

    void Gantry::update() {
        beltA = leftStepper.getSteps() * mmPerStep;

        beltB = rightStepper.getSteps() * mmPerStep;

        xPosMM = -0.5 * (beltA + beltB);
        yPosMM = 0.5 * (beltA - beltB);
    }