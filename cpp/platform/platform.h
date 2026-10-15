#pragma once

#include <stdint.h>

//the little the engine needs from the operating system; platform_host.cpp implements it

//microseconds since an arbitrary start. It wraps after about 71 minutes, so only ever compare differences
//(now - start), which unsigned subtraction keeps correct across the wrap
uint32_t nowMicros();
