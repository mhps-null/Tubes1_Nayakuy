package alt3.robots;

import battlecode.common.*;
import alt3.strategy.*;
import alt3.nav.*;

public class SoldierBot {

public static void run(RobotController rc) throws GameActionException {

    RobotInfo targetEnemy = getBestEnemy(rc);

    if (targetEnemy != null && rc.canAttack(targetEnemy.getLocation())) {
        rc.attack(targetEnemy.getLocation());
        return;
    }

    if (targetEnemy != null) {

        int dist = rc.getLocation().distanceSquaredTo(targetEnemy.getLocation());

        if (rc.canAttack(targetEnemy.getLocation())) {
            rc.attack(targetEnemy.getLocation());
            return;
        }

        if (dist <= 36) {
            Navigation.moveToward(rc, targetEnemy.getLocation());
            return;
        }
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

static RobotInfo getBestEnemy(RobotController rc) throws GameActionException {

    RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

    RobotInfo best = null;
    int bestScore = -999;

    for (RobotInfo enemy : enemies) {

        if (enemy.getType().isTowerType()) continue;

        boolean nearTower = false;

        for (RobotInfo e : enemies) {

            if (e.getType().isTowerType()) {

                int d = enemy.getLocation().distanceSquaredTo(e.getLocation());

                if (d <= 16) {
                    nearTower = true;
                    break;
                }
            }
        }

        if (nearTower) continue;

        int score = 0;

        score += 40;

        score += (50 - enemy.getHealth());

        int dist = rc.getLocation().distanceSquaredTo(enemy.getLocation());
        score -= dist;

        if (score > bestScore) {
            bestScore = score;
            best = enemy;
        }
    }

    return best;
}

static void paintUnderSelf(RobotController rc) throws GameActionException {

    MapInfo tile = rc.senseMapInfo(rc.getLocation());

    if (tile.getPaint() == PaintType.EMPTY && rc.getPaint() > 10 && rc.canAttack(rc.getLocation())) {
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