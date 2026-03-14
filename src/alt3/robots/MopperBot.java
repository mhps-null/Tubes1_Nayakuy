package alt3.robots;

import battlecode.common.*;
import alt3.nav.*;
import alt3.strategy.*;

public class MopperBot {

    public static void run(RobotController rc) throws GameActionException {

        MapInfo[] tiles = rc.senseNearbyMapInfos();

        MapLocation best = null;
        int bestScore = Integer.MIN_VALUE;

        for (MapInfo tile : tiles) {

            if (tile.getPaint().isEnemy()) {

                MapLocation loc = tile.getMapLocation();
                int dist = rc.getLocation().distanceSquaredTo(loc);

                int score = -dist;

                for (MapInfo other : tiles) {
                    if (other.getPaint().isEnemy()) {
                        if (loc.distanceSquaredTo(other.getMapLocation()) <= 2) {
                            score += 5;
                        }
                    }
                }

                if (score > bestScore) {
                    bestScore = score;
                    best = loc;
                }
            }
        }

        if (best != null) {

            if (rc.canAttack(best)) {
                rc.attack(best);
                return;
            }

            Navigation.moveToward(rc, best);
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