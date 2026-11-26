#include <Arduino.h>
#include "Gantry.h"
#include "Electromagnet.h"
#include "MultiplexorArray.h"
#include <cstring>

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
bool boardInitialized = false;

char serialLog[65];
int serialLogIndex = 0;



void movePiece(int startSquare, int targetSquare, bool whitePiece) {
  magnet.off();
  gantry.setSquare(startSquare);
  magnet.on(whitePiece);
  gantry.setSquare(targetSquare);
  magnet.off();
}

void initialize() {
  magnet.initialize();
  gantry.initialize();
  sensors.initialize();
  boardInitialized = true;
}

void setup() {
  Serial.begin(9600);
}

void loop() {
  bool commandRecieved = false;

  while(Serial.available() > 0) {
    bool overflow = serialLogIndex >= 64;

    char serialChar = Serial.read();

    if(serialChar == '\n') {
      serialLog[serialLogIndex] = '\0';
      serialLogIndex = 0;
      if(overflow) {
        Serial.println("ERR");
        break;
      }
      commandRecieved = true;
      break;
    } else if (serialChar == '\r' || overflow) {
      continue;
    } else {
      serialLog[serialLogIndex] = serialChar;
      serialLogIndex++;
    }
  }

  if (commandRecieved) {
    char* command = strtok(serialLog, " ");
    if(command == NULL) {
      return;
    }

    if(strcmp(command, "INIT") == 0) {
      initialize();
      Serial.println("OK");

    } else if (strcmp(command, "PING") == 0) {
        char* args = strtok(nullptr, " ");

        Serial.print("OK ");
        Serial.println(args);

    } else if (strcmp(command, "SCAN") == 0) {
        if(!boardInitialized) {
          Serial.println("ERR");
          return;
        }
        boardState = sensors.getBoardState();
        char response[65];

        for(int i = 0; i < 64; i++) {

          if(boardState[i] == SquareState::WHITE) {
            response[i] = 'W';
          } else if (boardState[i] == SquareState::BLACK) {
            response[i] = 'B';
          } else {
            response[i] = 'E';
          }

        } 
        response[64] = '\0';

        Serial.println(response);
    } else if (strcmp(command, "MOVE") == 0) {
        if(!boardInitialized) {
          Serial.println("ERR");
          return;
        }
        char* color = strtok(nullptr, " ");

        if (color == nullptr) {
          Serial.println("ERR");
          return;
        }
        if ((strcmp(color, "W") != 0) && (strcmp(color, "B") != 0)) {
          Serial.println("ERR");
          return;
        }

        char* startSquarePtr = strtok(nullptr, " ");
        char* targetSquarePtr = strtok(nullptr, " ");

        if(startSquarePtr == nullptr || targetSquarePtr == nullptr) {
          Serial.println("ERR");
          return;
        }

        char* endptr;

        int startSquare = strtol(startSquarePtr, &endptr, 10);
        
        if(*endptr != '\0') {
          Serial.println("ERR");
          return;
        }

        int targetSquare = strtol(targetSquarePtr, &endptr, 10);

        if(*endptr != '\0') {
          Serial.println("ERR");
          return;
        }

        if(min(startSquare, targetSquare) < 0 || max(startSquare, targetSquare) > 63) {
          Serial.println("ERR");
          return;
        }

        movePiece(startSquare, targetSquare, strcmp(color, "W") == 0);

        Serial.println("OK");
    } else {
      Serial.println("ERR");
    }
  }
}