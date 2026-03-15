package alt3;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.PaintType;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;

public class Helper {
    static final Direction[] DIRS = {
            Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
            Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
    };

    public static Direction getGreedyMopSwingDir(RobotController rc) throws GameActionException {
        Direction bestDir = null;
        int maxScore = 0;

        for (Direction dir : DIRS) {
            if (rc.canMopSwing(dir)) {
                int score = 0;
                MapLocation targetLoc = rc.adjacentLocation(dir);

                if (rc.canSenseLocation(targetLoc)) {
                    RobotInfo bot = rc.senseRobotAtLocation(targetLoc);
                    if (bot != null && bot.getTeam() == rc.getTeam().opponent()) {
                        score += 10;
                    }
                }

                if (score > maxScore && score > 0) {
                    maxScore = score;
                    bestDir = dir;
                }
            }
        }
        return bestDir;
    }

    public static MapLocation getGreedyCleanTarget(RobotController rc, MapInfo[] nearbyTiles)
            throws GameActionException {
        MapLocation bestLoc = null;
        int maxScore = -9999;

        for (MapInfo tile : nearbyTiles) {
            MapLocation loc = tile.getMapLocation();

            if (rc.canAttack(loc)) {
                int score = 0;

                if (tile.getPaint().isEnemy()) {
                    score = 10;

                    if (score > maxScore || (score == maxScore && Math.random() > 0.5)) {
                        maxScore = score;
                        bestLoc = loc;
                    }
                }
            }
        }
        return bestLoc;
    }

    public static MapLocation getGreedySpawnLocation(RobotController rc, UnitType type) throws GameActionException {
        MapLocation bestLoc = null;
        int maxScore = -9999;
        MapLocation myLoc = rc.getLocation();
        MapLocation[] possibleLocs = rc.getAllLocationsWithinRadiusSquared(myLoc, 4);

        for (MapLocation loc : possibleLocs) {
            if (rc.canBuildRobot(type, loc)) {
                int score = (Math.abs(loc.x - myLoc.x) + Math.abs(loc.y - myLoc.y));
                if (score > maxScore) {
                    maxScore = score;
                    bestLoc = loc;
                }
            }
        }
        return bestLoc;
    }

    public static MapLocation getGreedySingleTarget(RobotInfo[] enemies) {
        MapLocation bestTarget = null;
        int lowestHP = 999999;
        for (RobotInfo enemy : enemies) {
            if (enemy.getHealth() < lowestHP) {
                lowestHP = enemy.getHealth();
                bestTarget = enemy.getLocation();
            }
        }
        return bestTarget;
    }

    public static boolean isMopperNeeded(RobotController rc) throws GameActionException {
        MapInfo[] maps = rc.senseNearbyMapInfos(-1);
        int enemyPaintCount = 0;
        for (MapInfo map : maps) {
            if (map.getPaint().isEnemy()) {
                enemyPaintCount++;
            }
        }
        return enemyPaintCount > 5;
    }

    public static MapLocation getBlueprintPaintTarget(RobotController rc, MapLocation targetRuin)
            throws GameActionException {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                MapLocation checkLoc = targetRuin.translate(dx, dy);
                if (rc.canSenseLocation(checkLoc)) {
                    MapInfo info = rc.senseMapInfo(checkLoc);
                    PaintType requiredMark = info.getMark();
                    PaintType currentPaint = info.getPaint();

                    if (requiredMark != PaintType.EMPTY && currentPaint != requiredMark) {
                        return checkLoc;
                    }
                }
            }
        }
        return null;
    }

    public static MapLocation getGreedyPaintTarget(RobotController rc, MapInfo[] nearbyTiles)
            throws GameActionException {
        MapLocation bestLoc = null;
        int maxScore = -9999;

        for (MapInfo tile : nearbyTiles) {
            MapLocation loc = tile.getMapLocation();
            if (rc.canAttack(loc)) {
                int score = 0;
                if (!tile.getPaint().isAlly()) {
                    score = tile.getPaint().isEnemy() ? 5 : 6;

                    if (score > maxScore || (score == maxScore && Math.random() > 0.5)) {
                        maxScore = score;
                        bestLoc = loc;
                    }
                }
            }
        }
        return bestLoc;
    }

    public static MapLocation getGreedyEnemyTarget(RobotController rc, RobotInfo[] enemies) {
        MapLocation bestLoc = null;
        for (RobotInfo enemy : enemies) {
            if (rc.canAttack(enemy.getLocation())) {
                if (enemy.getType().isTowerType()) {
                    return enemy.getLocation();
                }
                bestLoc = enemy.getLocation();
            }
        }
        return bestLoc;
    }

    public static int evaluateFrontierExpansionScore(RobotController rc, MapLocation loc) throws GameActionException {
        int score = 0;
        MapInfo tileInfo = rc.senseMapInfo(loc);

        if (tileInfo.hasRuin())
            score += 2;
        if (!tileInfo.getPaint().isAlly())
            score += 3;
        else
            score -= 2;

        RobotInfo enemyTower = senseEnemyTowerNearby(rc, loc);
        if (enemyTower != null)
            score -= 4;

        return score;
    }

    public static RobotInfo senseEnemyTowerNearby(RobotController rc, MapLocation loc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(loc, 9, rc.getTeam().opponent());
        for (RobotInfo enemy : enemies) {
            if (enemy.getType().isTowerType())
                return enemy;
        }
        return null;
    }

    public static MapLocation getGreedyAoETarget(RobotController rc, MapInfo[] nearbyTiles, RobotInfo[] enemies)
            throws GameActionException {
        MapLocation bestLoc = null;
        int maxAoEScore = -9999;

        for (MapInfo tile : nearbyTiles) {
            MapLocation centerLoc = tile.getMapLocation();
            if (rc.canAttack(centerLoc)) {
                int currentScore = 0;

                for (RobotInfo enemy : enemies) {
                    if (enemy.getLocation().distanceSquaredTo(centerLoc) <= 4) {
                        currentScore += 20;
                    }
                }

                if (tile.getPaint().isEnemy())
                    currentScore += 10;
                else if (!tile.getPaint().isAlly())
                    currentScore += 3;

                if (currentScore > maxAoEScore && currentScore > 0) {
                    maxAoEScore = currentScore;
                    bestLoc = centerLoc;
                }
            }
        }
        return bestLoc;
    }

    public static int evaluateSplasherMovementScore(RobotController rc, MapLocation loc, MapLocation currentTarget)
            throws GameActionException {
        int score = 0;
        if (currentTarget != null) {
            int distToTarget = loc.distanceSquaredTo(currentTarget);
            score -= distToTarget;
            return score;
        }

        MapInfo tileInfo = rc.senseMapInfo(loc);
        if (!tileInfo.getPaint().isAlly())
            score += 5;
        else
            score -= 2;

        RobotInfo[] enemiesNear = rc.senseNearbyRobots(loc, 9, rc.getTeam().opponent());
        for (RobotInfo enemy : enemiesNear) {
            if (enemy.getType().isTowerType())
                score -= 10;
        }
        return score;
    }

    public static int evaluateMopperMovementScore(RobotController rc, MapLocation loc) throws GameActionException {
        int score = 0;
        MapInfo info = rc.senseMapInfo(loc);

        if (info.getPaint().isEnemy())
            score += 8;
        else if (!info.getPaint().isAlly())
            score += 2;

        RobotInfo[] enemiesNear = rc.senseNearbyRobots(loc, 9, rc.getTeam().opponent());
        if (enemiesNear.length > 0)
            score += 5;

        return score;
    }

    public static void moveTowards(RobotController rc, MapLocation target) throws GameActionException {
        Direction dir = rc.getLocation().directionTo(target);

        Direction[] tryDirs = {
                dir,
                dir.rotateLeft(),
                dir.rotateRight(),
                dir.rotateLeft().rotateLeft(),
                dir.rotateRight().rotateRight()
        };

        for (Direction d : tryDirs) {
            if (rc.canMove(d)) {
                rc.move(d);
                return;
            }
        }

        moveRandomly(rc);
    }

    public static void moveRandomly(RobotController rc) throws GameActionException {
        int startIndex = (int) (Math.random() * DIRS.length);
        for (int i = 0; i < DIRS.length; i++) {
            Direction dir = DIRS[(startIndex + i) % DIRS.length];
            if (rc.canMove(dir)) {
                rc.move(dir);
                break;
            }
        }
    }
}