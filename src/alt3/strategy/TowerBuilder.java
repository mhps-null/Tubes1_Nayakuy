package alt3.strategy;

import battlecode.common.*;
import alt3.nav.Navigation;

public class TowerBuilder {

    public static boolean tryBuildTower(RobotController rc) throws GameActionException {

        MapInfo[] tiles = rc.senseNearbyMapInfos();
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

        MapLocation bestRuin = null;
        int bestScore = -999;

        for (MapInfo tile : tiles) {

            if (!tile.hasRuin()) continue;

            MapLocation ruin = tile.getMapLocation();

            int score = 0;

            int dist = rc.getLocation().distanceSquaredTo(ruin);
            score += 80 - dist;

            int allyNear = 0;

            for (RobotInfo ally : allies) {

                int d = ally.getLocation().distanceSquaredTo(ruin);

                if (d <= 4) {
                    allyNear++;
                }
            }

            if (allyNear >= 4) continue;

            score -= allyNear * 10;

            if (score > bestScore) {
                bestScore = score;
                bestRuin = ruin;
            }
        }

        if (bestRuin == null) return false;

        if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, bestRuin)) {
            rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, bestRuin);
            return true;
        }

        if (rc.canMarkTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, bestRuin)) {
            rc.markTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, bestRuin);
            return true;
        }

        Navigation.moveToward(rc, bestRuin);
        return true;
    }
}