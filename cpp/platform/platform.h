#pragma once

#include <stdint.h>

//the little the engine needs from whatever it runs on; platform_host.cpp and platform_teensy.cpp implement it

//microseconds since an arbitrary start. It wraps after about 71 minutes, so only ever compare differences
//(now - start), which unsigned subtraction keeps correct across the wrap
uint32_t nowMicros();

//called by the search every few thousand nodes, at the same point it checks the clock. Board firmware can poll
//buttons and sensors here and call Engine::stopSearch() to make the search return early
void serviceHook();
