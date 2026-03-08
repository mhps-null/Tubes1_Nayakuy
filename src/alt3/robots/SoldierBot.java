package alt3.robots;

import battlecode.common.*;
import alt3.strategy.*;
import alt3.nav.*;

public class SoldierBot {

    public static void run(RobotController rc) throws GameActionException {

        paintUnderSelf(rc);

        if (TowerBuilder.tryBuildTower(rc)) {
            return;
        }

        if (tryPaintBestTile(rc)) {
            return;
        }

        MapLocation frontier = FrontierDetector.findNearestFrontier(rc);

        if (frontier != null) {
            Navigation.moveToward(rc, frontier);
            return;
        }

        Navigation.randomMove(rc);
    }


    static void paintUnderSelf(RobotController rc) throws GameActionException {

        MapInfo tile = rc.senseMapInfo(rc.getLocation());

        if (!tile.getPaint().isAlly() && rc.canAttack(rc.getLocation())) {
            rc.attack(rc.getLocation());
        }
    }


    static boolean tryPaintBestTile(RobotController rc) throws GameActionException {

        MapInfo[] tiles = rc.senseNearbyMapInfos();

        MapLocation best = null;
        int bestScore = -999;

        for (MapInfo tile : tiles) {

            int score = GreedyEvaluator.scoreTile(rc, tile);

            if (score > bestScore) {
                bestScore = score;
                best = tile.getMapLocation();
            }
        }

        if (bestScore > 0 && best != null && rc.canAttack(best)) {
            rc.attack(best);
            return true;
        }

        return false;
    }
}