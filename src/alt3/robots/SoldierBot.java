package alt3.robots;

import battlecode.common.*;
import alt3.strategy.*;
import alt3.nav.*;

public class SoldierBot {

    public static void run(RobotController rc) throws GameActionException {

        if (attackEnemyRobot(rc)) {
            return;
        }

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

    static boolean attackEnemyRobot(RobotController rc) throws GameActionException {

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        RobotInfo best = null;
        int bestDist = Integer.MAX_VALUE;

        for (RobotInfo enemy : enemies) {

            if (!enemy.getType().isTowerType()) {

                int dist = rc.getLocation().distanceSquaredTo(enemy.getLocation());

                if (dist < bestDist) {
                    bestDist = dist;
                    best = enemy;
                }
            }
        }

        if (best != null && rc.canAttack(best.getLocation())) {
            rc.attack(best.getLocation());
            return true;
        }

        return false;
    }

    static void paintUnderSelf(RobotController rc) throws GameActionException {

        MapInfo tile = rc.senseMapInfo(rc.getLocation());

        if (tile.getPaint() == PaintType.EMPTY && rc.canAttack(rc.getLocation())) {
            rc.attack(rc.getLocation());
        }
    }

    static boolean tryPaintBestTile(RobotController rc) throws GameActionException {

        MapInfo[] tiles = rc.senseNearbyMapInfos();

        MapLocation best = null;
        int bestScore = -999;

        for (MapInfo tile : tiles) {

            if (tile.getPaint().isAlly()) continue;

            int score = GreedyEvaluator.scoreTile(rc, tile);

            if (score > bestScore) {
                bestScore = score;
                best = tile.getMapLocation();
            }
        }

        if (best != null) {

            MapInfo info = rc.senseMapInfo(best);

            if (bestScore > 0 && rc.canAttack(best) && !info.getPaint().isAlly()) {
                rc.attack(best);
                return true;
            }
        }

        return false;
    }
}