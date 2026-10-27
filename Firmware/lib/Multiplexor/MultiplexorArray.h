#pragma once
#include "Arduino.h"
#include <array>


enum SquareState{
    WHITE,
    BLACK,
    EMPTY
};

class MultiplexorArray{

    public:
    MultiplexorArray(const std::array<int, 6> sigPins, const std::array<int, 4> selectPins);
    void initialize();

    std::array<SquareState, 64> getBoardState();


    private:
    uint32_t multiplexorIdMask = ~0x0000000F;
    std::array<int, 6> sigPins;
    std::array<int, 4> selectPins;

    int lowThreshold = 800;
    int highThreshold = 2000;

    SquareState getSquare(int square);
};