package alt_1;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.PaintType;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;

public class Helper {

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static int distSq(MapLocation a, MapLocation b) {
        int dx = a.x - b.x;
        int dy = a.y - b.y;
        return dx * dx + dy * dy;
    }

    static boolean moveToward(RobotController rc, MapLocation target) throws GameActionException {
        if (!rc.isMovementReady())
            return false;

        Direction dir = rc.getLocation().directionTo(target);

        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }
        Direction left = dir.rotateLeft();
        if (rc.canMove(left)) {
            rc.move(left);
            return true;
        }
        Direction right = dir.rotateRight();
        if (rc.canMove(right)) {
            rc.move(right);
            return true;
        }
        return false;
    }

    static boolean moveAway(RobotController rc, MapLocation target) throws GameActionException {
        if (!rc.isMovementReady())
            return false;

        Direction dir = rc.getLocation().directionTo(target).opposite();

        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }
        Direction left = dir.rotateLeft();
        if (rc.canMove(left)) {
            rc.move(left);
            return true;
        }
        Direction right = dir.rotateRight();
        if (rc.canMove(right)) {
            rc.move(right);
            return true;
        }
        return false;
    }

    // Greedy: bergerak berdasarkan skor
    static Direction bestMoveDirection(RobotController rc) throws GameActionException {
        Direction bestDir = null;
        int bestScore = Integer.MIN_VALUE;
        MapLocation here = rc.getLocation();

        for (Direction dir : directions) {
            if (!rc.canMove(dir))
                continue;

            MapLocation next = here.add(dir);
            int score = scoreMoveLocation(rc, next);

            if (score > bestScore) {
                bestScore = score;
                bestDir = dir;
            }
        }
        return bestDir;
    }

    static int scoreMoveLocation(RobotController rc, MapLocation loc) throws GameActionException {
        int score = 0;

        MapInfo[] nearby = rc.senseNearbyMapInfos(loc, 4);
        for (MapInfo tile : nearby) {
            PaintType paint = tile.getPaint();
            if (paint == PaintType.EMPTY)
                score += 2;
            else if (paint.isEnemy())
                score += 3;
        }

        RobotInfo[] allies = rc.senseNearbyRobots(loc, 2, rc.getTeam());
        score -= allies.length * 5;

        try {
            MapInfo locInfo = rc.senseMapInfo(loc);
            if (locInfo.getPaint().isEnemy() && rc.getPaint() < 80) {
                score -= 8;
            }
        } catch (GameActionException e) {
        }

        return score;
    }

    static RobotInfo nearestAllyTower(RobotController rc) throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo best = null;
        int bestDist = Integer.MAX_VALUE;

        for (RobotInfo r : allies) {
            if (r.getType().isTowerType()) {
                int d = distSq(rc.getLocation(), r.getLocation());
                if (d < bestDist) {
                    bestDist = d;
                    best = r;
                }
            }
        }
        return best;
    }

    static MapLocation nearestEmptyRuin(RobotController rc) throws GameActionException {
        MapInfo[] tiles = rc.senseNearbyMapInfos();
        MapLocation best = null;
        int bestDist = Integer.MAX_VALUE;

        for (MapInfo tile : tiles) {
            if (!tile.hasRuin())
                continue;

            MapLocation loc = tile.getMapLocation();

            RobotInfo existing = rc.senseRobotAtLocation(loc);
            if (existing != null)
                continue;

            int d = distSq(rc.getLocation(), loc);
            if (d < bestDist) {
                bestDist = d;
                best = loc;
            }
        }
        return best;
    }

    static int countAdjacentAllies(RobotController rc) throws GameActionException {
        return rc.senseNearbyRobots(2, rc.getTeam()).length;
    }

    static MapLocation nearestEnemyPaint(RobotController rc) throws GameActionException {
        MapInfo[] tiles = rc.senseNearbyMapInfos();
        MapLocation best = null;
        int bestDist = Integer.MAX_VALUE;

        for (MapInfo tile : tiles) {
            if (!tile.getPaint().isEnemy())
                continue;

            int d = distSq(rc.getLocation(), tile.getMapLocation());
            if (d < bestDist) {
                bestDist = d;
                best = tile.getMapLocation();
            }
        }
        return best;
    }

    static RobotInfo nearestEnemy(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo best = null;
        int bestDist = Integer.MAX_VALUE;

        for (RobotInfo r : enemies) {
            int d = distSq(rc.getLocation(), r.getLocation());
            if (d < bestDist) {
                bestDist = d;
                best = r;
            }
        }
        return best;
    }

    static boolean isOnEnemyTerritory(RobotController rc) throws GameActionException {
        return rc.senseMapInfo(rc.getLocation()).getPaint().isEnemy();
    }

    static RobotInfo mostNeedyAlly(RobotController rc, int radiusSq) throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(radiusSq, rc.getTeam());
        RobotInfo best = null;
        int lowestPaint = Integer.MAX_VALUE;

        for (RobotInfo r : allies) {
            if (r.getType().isTowerType())
                continue;
            if (r.getType() == UnitType.MOPPER)
                continue;

            if (r.getPaintAmount() < lowestPaint) {
                lowestPaint = r.getPaintAmount();
                best = r;
            }
        }
        return best;
    }

    static UnitType neededTowerType(RobotController rc) throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        int paintTowers = 0;
        int moneyTowers = 0;

        for (RobotInfo r : allies) {
            if (r.getType() == UnitType.LEVEL_ONE_PAINT_TOWER ||
                    r.getType() == UnitType.LEVEL_TWO_PAINT_TOWER ||
                    r.getType() == UnitType.LEVEL_THREE_PAINT_TOWER) {
                paintTowers++;
            }
            if (r.getType() == UnitType.LEVEL_ONE_MONEY_TOWER ||
                    r.getType() == UnitType.LEVEL_TWO_MONEY_TOWER ||
                    r.getType() == UnitType.LEVEL_THREE_MONEY_TOWER) {
                moneyTowers++;
            }
        }

        // Greedy: seimbangkan paint dan money tower
        if (paintTowers <= moneyTowers)
            return UnitType.LEVEL_ONE_PAINT_TOWER;
        if (moneyTowers < 3)
            return UnitType.LEVEL_ONE_MONEY_TOWER;
        return UnitType.LEVEL_ONE_DEFENSE_TOWER;
    }
}
