//the precomputed rook attack table lives in its own file so only this one compiles the 100k-entry header. The flash
//backend looks attacks up in it; every build links it so the PC test can check it against the computed table
#include "magic.h"

#include "generated/rook_attacks.h"
