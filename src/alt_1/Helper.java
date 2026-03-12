package alt_1;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.PaintType;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;

public class Helper {

    static RobotInfo getNeediestSplasher(RobotInfo[] allies) {
        RobotInfo neediest = null;
        int minPaint = Integer.MAX_VALUE;

        for (RobotInfo ally : allies) {
            if (ally.getType() == UnitType.SPLASHER) {
                if (ally.paintAmount < minPaint) {
                    minPaint = ally.paintAmount;
                    neediest = ally;
                }
            }
        }

        if (minPaint < 100) {
            return neediest;
        }
        return null;
    }

    static void moveToLoc(RobotController rc, MapLocation target) throws GameActionException {
        Direction dir = rc.getLocation().directionTo(target);

        if (dir == Direction.CENTER)
            return;

        Direction[] tryDirs = {
                dir,
                dir.rotateLeft(),
                dir.rotateRight()
        };

        for (Direction d : tryDirs) {
            if (rc.canMove(d)) {
                rc.move(d);
                return;
            }
        }
    }

    static void retreatForRefill(RobotController rc) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

        RobotInfo nearestTower = null;
        int minDist = 999999;

        for (RobotInfo ally : allies) {
            if (ally.getType().isTowerType()) {
                int dist = myLoc.distanceSquaredTo(ally.getLocation());
                if (dist < minDist) {
                    minDist = dist;
                    nearestTower = ally;
                }
            }
        }

        if (nearestTower != null) {
            moveToLoc(rc, nearestTower.getLocation());
        } else {
            RobotPlayer.explore(rc);
        }
    }

    static RobotInfo getNearestPaintTower(MapLocation myLoc, RobotInfo[] allies) {
        RobotInfo nearest = null;
        int minDist = 999999;

        for (RobotInfo ally : allies) {
            if (ally.getType().isTowerType() && ally.getPaintAmount() > 50) {
                int dist = myLoc.distanceSquaredTo(ally.getLocation());
                if (dist < minDist) {
                    minDist = dist;
                    nearest = ally;
                }
            }
        }
        return nearest;
    }

    static RobotInfo getClosestRobot(MapLocation myLoc, RobotInfo[] robots) {
        RobotInfo closest = null;
        int minDist = 999999;

        for (RobotInfo bot : robots) {
            int dist = myLoc.distanceSquaredTo(bot.getLocation());
            if (dist < minDist) {
                minDist = dist;
                closest = bot;
            }
        }
        return closest;
    }

    static RobotInfo getNeediestSplasher(MapLocation myLoc, RobotInfo[] allies) {
        RobotInfo neediest = null;
        int minDist = 999999;

        for (RobotInfo ally : allies) {
            if (ally.getType() == UnitType.SPLASHER) {

                if (ally.getPaintAmount() < 50) {
                    int dist = myLoc.distanceSquaredTo(ally.getLocation());
                    if (dist < minDist) {
                        minDist = dist;
                        neediest = ally;
                    }
                }
            }
        }
        return neediest;
    }

    static MapLocation findSpawnLocation(RobotController rc) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        Direction[] dirs = Direction.allDirections();

        for (Direction dir : dirs) {
            if (dir != Direction.CENTER) {
                MapLocation nextLoc = myLoc.add(dir);
                if (rc.onTheMap(nextLoc) && rc.senseRobotAtLocation(nextLoc) == null) {
                    if (rc.sensePassability(nextLoc)) {
                        return nextLoc;
                    }
                }
            }
        }
        return null;
    }

    static Direction chooseNewExplorationDirection(RobotController rc) throws GameActionException {
        MapLocation myLoc = rc.getLocation();

        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

        if (allies.length > 0) {
            int netDx = 0;
            int netDy = 0;

            for (RobotInfo ally : allies) {
                netDx += (myLoc.x - ally.location.x);
                netDy += (myLoc.y - ally.location.y);
            }

            if (netDx != 0 || netDy != 0) {
                Direction awayDir = myLoc.directionTo(new MapLocation(myLoc.x + netDx, myLoc.y + netDy));

                if (awayDir != Direction.CENTER && rc.canMove(awayDir)) {
                    return awayDir;
                }
            }
        }

        Direction[] dirs = Direction.allDirections();
        int startIndex = (int) (Math.random() * 8);

        for (int i = 0; i < 8; i++) {
            Direction dir = dirs[(startIndex + i) % 8];
            if (rc.canMove(dir)) {
                return dir;
            }
        }

        return Direction.NORTH;
    }

    static MapLocation getBestSplashTarget(RobotController rc) throws GameActionException {
        MapLocation myLoc = rc.getLocation();

        MapLocation[] possibleTargets = rc.getAllLocationsWithinRadiusSquared(myLoc, rc.getType().actionRadiusSquared);

        MapLocation bestTarget = null;
        int maxTilesToPaint = 0;

        for (MapLocation target : possibleTargets) {
            if (!rc.canAttack(target))
                continue;

            int validPaintCount = 0;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    MapLocation splashArea = target.translate(dx, dy);

                    if (rc.canSenseLocation(splashArea)) {
                        PaintType paintOnTile = rc.senseMapInfo(splashArea).getPaint();

                        if (paintOnTile != PaintType.ALLY_PRIMARY && paintOnTile != PaintType.ALLY_SECONDARY) {
                            validPaintCount++;
                        }
                    }
                }
            }

            if (validPaintCount > maxTilesToPaint && validPaintCount >= 4) {
                maxTilesToPaint = validPaintCount;
                bestTarget = target;
            }
        }

        return bestTarget;
    }
}
