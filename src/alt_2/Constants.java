package alt_2;

import battlecode.common.Direction;

public class Constants {
    // Array arah standar
    public static final Direction[] DIRECTIONS = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    // Konstanta Pesan (32-bit Encoding)
    public static final int MSG_ENEMY     = 0;
    public static final int MSG_LOW_PAINT = 1;
    public static final int MSG_SYMMETRY  = 2;
}