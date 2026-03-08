package alt3.strategy;

import battlecode.common.*;

public class GreedyEvaluator {

    public static int scoreTile(RobotController rc, MapInfo tile) throws GameActionException {

        int score = 0;

        MapLocation loc = tile.getMapLocation();

        if (tile.getPaint() == PaintType.EMPTY)
            score += 8;

        if (tile.getPaint().isEnemy())
            score += 6;

        if (tile.hasRuin())
            score += 10;

        if (tile.getPaint().isAlly())
            score -= 3;

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        for (RobotInfo enemy : enemies) {

            if (enemy.getType().isTowerType()) {

                int dist = loc.distanceSquaredTo(enemy.getLocation());

                if (dist <= 16) {
                    score -= 25;
                }
            }
        }

        int distToSelf = rc.getLocation().distanceSquaredTo(loc);
        score -= distToSelf / 3;

        return score;
    }
}