#include "MultiplexorArray.h"

MultiplexorArray::MultiplexorArray(const std::array<int, 6> sigPins, const std::array<int, 4> selectPins) 
                    : sigPins(sigPins), selectPins(selectPins) {}


std::array<SquareState, 64> MultiplexorArray::getBoardState() {
    std::array<SquareState, 64> board;

    for(int i = 0; i < 16; i++) {
        for(int j = 0; j < 4; j++) {
            board[16*j + i] = getSquare(16*j + i);
        }   
    }

    return board;
}


SquareState MultiplexorArray::getSquare(int square) {
    int multiplexorID = (square & multiplexorIdMask) >> 4;

    int sigPort = square & 0xf;

 
    for(int i = 0; i < selectPins.size(); i++) {
        digitalWrite(selectPins[i], (sigPort >> i) & 1);
    }

    delayMicroseconds(5);

    int sum = 0;
    int numReadings = 8;

    for (int i = 0; i < numReadings; i++) {
        sum += analogRead(sigPins[multiplexorID]);
    }

    int avg =  sum / numReadings;

    if (avg < lowThreshold) {
        return SquareState::WHITE;
    } else if (avg > highThreshold) {
        return SquareState::BLACK;
    } else {
        return SquareState::EMPTY;
    }
}

void MultiplexorArray::initialize() {
    for(int i = 0; i < sigPins.size(); i++) {
        pinMode(sigPins[i], INPUT);
    }

    for(int i = 0; i < selectPins.size(); i++) {
        pinMode(selectPins[i], OUTPUT);
    }
}