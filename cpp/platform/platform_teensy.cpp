#include "platform.h"

#include <Arduino.h>

uint32_t nowMicros() {
    return micros();
}

//weak, so the board firmware (or the serial shell) replaces it just by defining its own serviceHook()
__attribute__((weak)) void serviceHook() {
}
