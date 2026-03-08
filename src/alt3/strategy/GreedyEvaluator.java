package alt3.strategy;

import battlecode.common.*;

public class GreedyEvaluator {

    public static int scoreTile(RobotController rc, MapInfo tile) throws GameActionException {

        int score = 0;

        MapLocation loc = tile.getMapLocation();

        // paint expansion
        if (tile.getPaint() == PaintType.EMPTY)
            score += 6;

        if (tile.getPaint().isEnemy())
            score += 5;

        if (tile.hasRuin())
            score += 3;

        if (tile.getPaint().isAlly())
            score -= 1;

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        for (RobotInfo enemy : enemies) {

            if (enemy.getType().isTowerType()) {

                int dist = loc.distanceSquaredTo(enemy.getLocation());

                if (dist <= 16) { 
                    score -= 20;
                }

            }
        }

        return score;
    }
}