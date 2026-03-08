package alt3.strategy;

import battlecode.common.*;

public class TowerBuilder {

    public static boolean tryBuildTower(RobotController rc) throws GameActionException {

        MapInfo[] tiles = rc.senseNearbyMapInfos();

        for (MapInfo tile : tiles) {

            if (tile.hasRuin()) {

                MapLocation ruin = tile.getMapLocation();

                if (rc.canCompleteTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruin)) {

                    rc.completeTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruin);
                    return true;
                }

                if (rc.canMarkTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruin)) {

                    rc.markTowerPattern(UnitType.LEVEL_ONE_PAINT_TOWER, ruin);
                    return true;
                }
            }
        }

        return false;
    }
}