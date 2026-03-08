package alt3.robots;

import battlecode.common.*;
import alt3.nav.*;

public class SplasherBot {

    public static void run(RobotController rc) throws GameActionException {

        MapInfo[] tiles = rc.senseNearbyMapInfos();

        int enemyCount = 0;
        MapLocation best = null;

        for (MapInfo tile : tiles) {

            if (tile.getPaint().isEnemy()) {
                enemyCount++;
                best = tile.getMapLocation();
            }
        }

        if (enemyCount >= 3 && best != null && rc.canAttack(best)) {
            rc.attack(best);
            return;
        }

        Navigation.randomMove(rc);
    }
}