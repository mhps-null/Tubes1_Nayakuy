package alt3.strategy;

import battlecode.common.*;

public class FrontierDetector {

    public static MapLocation findNearestFrontier(RobotController rc) throws GameActionException {

        MapInfo[] tiles = rc.senseNearbyMapInfos();

        MapLocation best = null;
        int bestDist = Integer.MAX_VALUE;

        for (MapInfo tile : tiles) {

            MapLocation loc = tile.getMapLocation();

            if (tile.hasRuin()) {
                return loc;
            }

            if (tile.getPaint() == PaintType.EMPTY || tile.getPaint().isEnemy()) {

                int dist = rc.getLocation().distanceSquaredTo(loc);

                if (dist < bestDist) {
                    bestDist = dist;
                    best = loc;
                }
            }
        }

        return best;
    }
}