package alt3.robots;

import battlecode.common.*;
import alt3.nav.*;
import alt3.strategy.*;

public class SplasherBot {

    public static void run(RobotController rc) throws GameActionException {

        MapInfo[] tiles = rc.senseNearbyMapInfos();

        MapLocation best = null;
        int bestScore = 0;

        for (MapInfo tile : tiles) {

            MapLocation loc = tile.getMapLocation();

            int enemyAround = 0;

            for (MapInfo other : tiles) {
                if (other.getPaint().isEnemy()) {

                    if (loc.distanceSquaredTo(other.getMapLocation()) <= 2) {
                        enemyAround++;
                    }
                }
            }

            if (enemyAround > bestScore) {
                bestScore = enemyAround;
                best = loc;
            }
        }

        if (best != null && bestScore >= 3 && rc.canAttack(best)) {
            rc.attack(best);
            return;
        }

        MapLocation frontier = FrontierDetector.findNearestFrontier(rc);

        if (frontier != null) {
            Navigation.moveToward(rc, frontier);
            return;
        }

        Navigation.randomMove(rc);
    }
}