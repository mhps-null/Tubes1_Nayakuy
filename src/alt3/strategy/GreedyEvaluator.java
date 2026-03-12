package alt3.strategy;

import battlecode.common.*;

public class GreedyEvaluator {

    public static int scoreTile(RobotController rc, MapInfo tile) throws GameActionException {

        int score = 0;

        MapLocation loc = tile.getMapLocation();
        PaintType paint = tile.getPaint();

        if (paint.isEnemy()) {
            score += 18;
        } 
        else if (paint == PaintType.EMPTY) {
            score += 10;
        }

        if (paint.isAlly()) {
            score -= 6;
        }

        if (tile.hasRuin()) {
            score += 70;
        }

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        for (RobotInfo enemy : enemies) {

            if (enemy.getType().isTowerType()) {

                int dist = loc.distanceSquaredTo(enemy.getLocation());

                if (dist <= 16) {
                    score -= 200;
                }
                else if (dist <= 25) {
                    score -= 80;
                }
            }
        }

        int distToSelf = rc.getLocation().distanceSquaredTo(loc);
        score -= distToSelf / 5;

        RobotInfo[] allies = rc.senseNearbyRobots(8, rc.getTeam());

        for (RobotInfo ally : allies) {

            if (ally.getLocation().equals(rc.getLocation())) continue;

            int d = loc.distanceSquaredTo(ally.getLocation());

            if (d <= 2) {
                score -= 4;
            }
        }

        return score;
    }
}