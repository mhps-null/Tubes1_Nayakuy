package alt3.nav;

import battlecode.common.*;

import java.util.Random;

public class Navigation {

    static Random rng = new Random();

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST
    };

    public static void moveToward(RobotController rc, MapLocation target) throws GameActionException {

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

        Direction bestDir = null;
        int bestScore = -999;

        for (Direction d : directions) {

            if (!rc.canMove(d)) continue;

            MapLocation next = rc.getLocation().add(d);

            if (!rc.onTheMap(next)) continue;

            MapInfo info = rc.senseMapInfo(next);

            int score = 0;

            if (info.getPaint().isEnemy()) {
                score -= 6;
            }

            int dist = next.distanceSquaredTo(target);
            score -= dist * 2;

            for (RobotInfo enemy : enemies) {

                if (enemy.getType().isTowerType()) {

                    int dTower = next.distanceSquaredTo(enemy.getLocation());

                    if (dTower <= 16) {
                        score -= 200;
                    }
                    else if (dTower <= 25) {
                        score -= 80;
                    }
                }
            }

            for (RobotInfo ally : allies) {

                if (ally.getLocation().equals(rc.getLocation())) continue;

                int dAlly = next.distanceSquaredTo(ally.getLocation());

                if (dAlly <= 2) {
                    score -= 20;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestDir = d;
            }
        }

        if (bestDir != null) {
            rc.move(bestDir);
            return;
        }

        randomMove(rc);
    }

    public static void randomMove(RobotController rc) throws GameActionException {

        int start = rng.nextInt(directions.length);

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

        for (int i = 0; i < directions.length; i++) {

            Direction d = directions[(start + i) % directions.length];

            if (!rc.canMove(d)) continue;

            MapLocation next = rc.getLocation().add(d);

            if (!rc.onTheMap(next)) continue;

            MapInfo info = rc.senseMapInfo(next);

            if (info.getPaint().isEnemy()) continue;

            boolean nearEnemyTower = false;

            for (RobotInfo enemy : enemies) {

                if (enemy.getType().isTowerType()) {

                    int dist = next.distanceSquaredTo(enemy.getLocation());

                    if (dist <= 25) {
                        nearEnemyTower = true;
                        break;
                    }
                }
            }

            if (nearEnemyTower) continue;

            boolean nearAlly = false;

            for (RobotInfo ally : allies) {

                if (ally.getLocation().equals(rc.getLocation())) continue;

                int dist = next.distanceSquaredTo(ally.getLocation());

                if (dist <= 2) {
                    nearAlly = true;
                    break;
                }
            }

            if (nearAlly) continue;

            rc.move(d);
            return;
        }
    }
}