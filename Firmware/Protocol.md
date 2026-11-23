# Communication Protocol between Pi and Teensy

The Pi will keep track of the official game state and engine.  
The Teensy will send an array of SquareStates to the Pi, which will determine if a move was made and what it is.  

| Command | Args                     | Reply           | Notes                         |
|---------|--------------------------|-----------------|-------------------------------|
| `PING`  | `<token>`                | `OK <token>`    | Echoes the token back         |
| `INIT`  | none                     | `OK` / `ERR`    | Homes the gantry              |
| `SCAN`  | none                     | `<64 chars>`    | One char per square, see below |
| `MOVE`  | `<W\|B> <from> <to>`     | `OK` / `ERR`    | Polarity, start and target index (0–63) |

**SCAN reply format:** 64 characters, no spaces, square 0 first.
`W` = white piece, `B` = black piece, `E` = empty.