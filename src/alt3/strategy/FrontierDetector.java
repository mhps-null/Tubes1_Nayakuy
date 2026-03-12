package alt3.strategy;

import battlecode.common.*;

public class FrontierDetector {

    public static MapLocation findNearestFrontier(RobotController rc) throws GameActionException {

        MapInfo[] tiles = rc.senseNearbyMapInfos();
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        MapLocation best = null;
        int bestScore = -999;

        for (MapInfo tile : tiles) {

            MapLocation loc = tile.getMapLocation();
            PaintType paint = tile.getPaint();

            int score = 0;

            if (tile.hasRuin()) {

                int allyNear = 0;

                for (RobotInfo a : allies) {
                    if (a.getLocation().distanceSquaredTo(loc) <= 4) {
                        allyNear++;
                    }
                }

                if (allyNear >= 3) continue;

                score += 80;
            }

            if (paint == PaintType.EMPTY || paint.isEnemy()) {

                int dist = rc.getLocation().distanceSquaredTo(loc);

                score += 50 - dist;

                if (paint.isEnemy()) {
                    score += 20;
                }

                int allyNear = 0;

                for (RobotInfo a : allies) {
                    if (a.getLocation().equals(rc.getLocation())) continue;
                    if (a.getLocation().distanceSquaredTo(loc) <= 2) {
                        allyNear++;
                    }
                }

                score -= allyNear * 12;

                for (RobotInfo enemy : enemies) {

                    if (enemy.getType().isTowerType()) {

                        int d = loc.distanceSquaredTo(enemy.getLocation());

                        if (d <= 16) {
                            score -= 120;
                        } 
                        else if (d <= 25) {
                            score -= 60;
                        }
                    }
                }
            }

            if (score > bestScore) {
                bestScore = score;
                best = loc;
            }
        }

        return best;
    }
}