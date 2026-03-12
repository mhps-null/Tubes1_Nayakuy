package alt3.robots;

import battlecode.common.*;
import alt3.nav.*;
import alt3.strategy.*;

public class SplasherBot {

    public static void run(RobotController rc) throws GameActionException {

        MapInfo[] tiles = rc.senseNearbyMapInfos();
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        MapLocation best = null;
        int bestScore = -999;

        for (MapInfo tile : tiles) {

            MapLocation loc = tile.getMapLocation();
            int score = 0;

            for (MapInfo other : tiles) {
                if (other.getPaint().isEnemy()) {
                    if (loc.distanceSquaredTo(other.getMapLocation()) <= 2) {
                        score += 2;
                    }
                }
            }

            for (RobotInfo enemy : enemies) {

                if (enemy.getType().isTowerType()) continue;

                int dist = loc.distanceSquaredTo(enemy.getLocation());

                if (dist <= 2) {
                    score += 20;
                }
                else if (dist <= 4) {
                    score += 10;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                best = loc;
            }
        }

        if (best != null && bestScore >= 4 && rc.canAttack(best)) {
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