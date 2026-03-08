package alt3.robots;

import battlecode.common.*;
import alt3.nav.*;

public class MopperBot {

    public static void run(RobotController rc) throws GameActionException {

        MapInfo[] tiles = rc.senseNearbyMapInfos();

        for (MapInfo tile : tiles) {

            if (tile.getPaint().isEnemy()) {

                MapLocation loc = tile.getMapLocation();

                if (rc.canAttack(loc)) {
                    rc.attack(loc);
                    return;
                }
            }
        }

        Navigation.randomMove(rc);
    }
}