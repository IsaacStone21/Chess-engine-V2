#include "platform.h"

#include <chrono>

uint32_t nowMicros() {
    auto elapsed = std::chrono::steady_clock::now().time_since_epoch();
    return (uint32_t) std::chrono::duration_cast<std::chrono::microseconds>(elapsed).count();
}
