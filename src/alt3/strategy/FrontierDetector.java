package alt3.strategy;

import battlecode.common.*;

public class FrontierDetector {

    public static MapLocation findNearestFrontier(RobotController rc) throws GameActionException {

        MapInfo[] tiles = rc.senseNearbyMapInfos();

        MapLocation best = null;
        int bestScore = -999;

        for (MapInfo tile : tiles) {

            MapLocation loc = tile.getMapLocation();

            if (tile.hasRuin()) {
                return loc;
            }

            if (tile.getPaint() == PaintType.EMPTY || tile.getPaint().isEnemy()) {

                int dist = rc.getLocation().distanceSquaredTo(loc);

                int score = 50 - dist;

                if (tile.getPaint().isEnemy()) {
                    score += 10;
                }

                if (score > bestScore) {
                    bestScore = score;
                    best = loc;
                }
            }
        }

        return best;
    }
}