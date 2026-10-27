#include <Arduino.h>
#include "Gantry.h"
#include "Electromagnet.h"
#include "MultiplexorArray.h"

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

std::array<int, 6> sigPins = {14, 15, 16, 17, 18, 19};
std::array<int, 4> selectPins = {28, 29, 30, 31};

Gantry gantry = Gantry(leftStepPin, leftDirPin, rightStepPin, rightDirPin, motorEnablePin, xLimitPin, yLimitPin);
Electromagnet magnet = Electromagnet(magnetPin1, magnetPin2, magnetDutyCycle);
MultiplexorArray sensors = MultiplexorArray(sigPins, selectPins);
std::array<SquareState, 64> boardState;


void movePiece(int startSquare, int targetSquare) {
  magnet.off();
  boardState = sensors.getBoardState();
  gantry.setSquare(startSquare);
  if(boardState[startSquare] == SquareState::WHITE) {
    magnet.on(true);
  } else {
    magnet.on(false);
  }




  gantry.setSquare(targetSquare);
  magnet.off();
}



void setup() {
  magnet.initialize();
  gantry.initialize();
  sensors.initialize();
}

void loop() {
  // put your main code here, to run repeatedly:
}
