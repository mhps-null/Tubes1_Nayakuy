package alt3.strategy;

import battlecode.common.*;

public class GreedyEvaluator {

    public static int scoreTile(RobotController rc, MapInfo tile) throws GameActionException {

        int score = 0;

        MapLocation loc = tile.getMapLocation();

        if (tile.getPaint() == PaintType.EMPTY)
            score += 7;

        if (tile.getPaint().isEnemy())
            score += 10;

        if (tile.hasRuin())
            score += 50;

        if (tile.getPaint().isAlly())
            score -= 3;

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        for (RobotInfo enemy : enemies) {

            if (enemy.getType().isTowerType()) {

                int dist = loc.distanceSquaredTo(enemy.getLocation());

                if (dist <= 16) {
                    score -= 80;   
                }
                else if (dist <= 25) {
                    score -= 40;   
                }
            }
        }

        int distToSelf = rc.getLocation().distanceSquaredTo(loc);
        score -= distToSelf / 10;

        return score;
    }
}