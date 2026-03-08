package alt3.util;

import battlecode.common.*;

public class Util {

    public static int distance(MapLocation a, MapLocation b) {
        return a.distanceSquaredTo(b);
    }

}