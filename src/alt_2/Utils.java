package alt_2;

import battlecode.common.*;

public class Utils {

    public static RobotInfo nearestAllyTower(RobotController rc) throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo best = null; int bestDist = Integer.MAX_VALUE;
        for (RobotInfo r : allies) {
            if (!r.getType().isTowerType()) continue;
            int d = rc.getLocation().distanceSquaredTo(r.getLocation());
            if (d < bestDist) { bestDist = d; best = r; }
        }
        return best;
    }

    // FUNGSI BARU: Hanya mencari Paint Tower!
    public static RobotInfo nearestAllyPaintTower(RobotController rc) throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo best = null; int bestDist = Integer.MAX_VALUE;
        for (RobotInfo r : allies) {
            if (r.getType() == UnitType.LEVEL_ONE_PAINT_TOWER || r.getType() == UnitType.LEVEL_TWO_PAINT_TOWER || r.getType() == UnitType.LEVEL_THREE_PAINT_TOWER) {
                int d = rc.getLocation().distanceSquaredTo(r.getLocation());
                if (d < bestDist) { bestDist = d; best = r; }
            }
        }
        return best;
    }

    public static boolean paintEmergency(RobotController rc) throws GameActionException {
        return rc.getPaint() <= (rc.getType().paintCapacity * 0.15);
    }

    public static boolean paintLow(RobotController rc) throws GameActionException {
        int threshold = (int)(rc.getType().paintCapacity * GameConstants.INCREASED_COOLDOWN_THRESHOLD / 100.0);
        return rc.getPaint() < threshold;
    }

    public static boolean moveGreedy(RobotController rc, MapLocation target) throws GameActionException {
        if (!rc.isMovementReady()) return false;
        if (rc.getLocation().equals(target)) return false;
        return moveGreedy(rc, rc.getLocation().directionTo(target));
    }

    public static boolean moveGreedy(RobotController rc, Direction targetDir) throws GameActionException {
        if (!rc.isMovementReady() || targetDir == Direction.CENTER) return false;
        
        Direction[] tryDirs = {
            targetDir, 
            targetDir.rotateLeft(), targetDir.rotateRight(), 
            targetDir.rotateLeft().rotateLeft(), targetDir.rotateRight().rotateRight()
        };
        
        // PASS 1: Prioritas Tertinggi -> Jalan di atas Cat Sekutu (Paint Surfing)
        for (Direction d : tryDirs) {
            if (rc.canMove(d)) {
                PaintType pt = rc.senseMapInfo(rc.getLocation().add(d)).getPaint();
                if (pt == PaintType.ALLY_PRIMARY || pt == PaintType.ALLY_SECONDARY) {
                    rc.move(d); return true;
                }
            }
        }
        
        // PASS 2: Prioritas Kedua -> Jalan di atas Tile Kosong (Lebih aman dari cat musuh)
        for (Direction d : tryDirs) {
            if (rc.canMove(d)) {
                PaintType pt = rc.senseMapInfo(rc.getLocation().add(d)).getPaint();
                if (pt == PaintType.EMPTY) {
                    rc.move(d); return true;
                }
            }
        }
        
        // PASS 3: Terpaksa jalan di cat musuh (daripada nyangkut)
        for (Direction d : tryDirs) {
            if (rc.canMove(d)) { 
                rc.move(d); return true; 
            }
        }

        // PASS 4: ANTI-STUCK (Mundur/Wiggle jika jalan depan buntu total)
        Direction[] escapeDirs = {
            targetDir.rotateLeft().rotateLeft().rotateLeft(), 
            targetDir.rotateRight().rotateRight().rotateRight(), 
            targetDir.opposite()
        };
        for (Direction d : escapeDirs) {
             if (rc.canMove(d)) { rc.move(d); return true; }
        }
        return false;
    }

    public static MapLocation getEnemyEstimate(RobotController rc, int symmetryMode, MapLocation towerLoc) throws GameActionException {
        int W = rc.getMapWidth(), H = rc.getMapHeight();
        int x = towerLoc.x, y = towerLoc.y;
        if (symmetryMode == 0) return new MapLocation(W-1-x, H-1-y);
        if (symmetryMode == 1) return new MapLocation(W-1-x, y);
        return new MapLocation(x, H-1-y);
    }

    public static int hitungSimetriDariObservasi(RobotController rc, MapLocation enemyPos, MapLocation towerLoc) throws GameActionException {
        int W = rc.getMapWidth(), H = rc.getMapHeight();
        int dRot = enemyPos.distanceSquaredTo(new MapLocation(W-1-towerLoc.x, H-1-towerLoc.y));
        int dV   = enemyPos.distanceSquaredTo(new MapLocation(W-1-towerLoc.x, towerLoc.y));
        int dH   = enemyPos.distanceSquaredTo(new MapLocation(towerLoc.x, H-1-towerLoc.y));
        if (dRot <= dV && dRot <= dH) return 0;
        if (dV <= dH) return 1;
        return 2;
    }

    public static Direction rotateBy(Direction dir, int n) {
        n = (n % 8 + 8) % 8;
        for (int i = 0; i < n; i++) dir = dir.rotateLeft();
        return dir;
    }

    public static MapLocation tileToward(RobotController rc, MapLocation target, UnitType robotType) throws GameActionException {
        if (rc.getLocation().equals(target)) return null;
        Direction best = rc.getLocation().directionTo(target);
        Direction[] candidates = { best, best.rotateLeft(), best.rotateRight(), best.rotateLeft().rotateLeft(), best.rotateRight().rotateRight(), best.opposite().rotateLeft(), best.opposite().rotateRight(), best.opposite() };
        for (Direction d : candidates) {
            MapLocation loc = rc.adjacentLocation(d);
            if (rc.canBuildRobot(robotType, loc)) return loc;
        }
        return null;
    }

    public static UnitType inferTowerType(RobotController rc, MapLocation ruin) throws GameActionException {
        UnitType[] priority = { UnitType.LEVEL_ONE_PAINT_TOWER, UnitType.LEVEL_ONE_MONEY_TOWER, UnitType.LEVEL_ONE_DEFENSE_TOWER };
        for (UnitType t : priority) if (rc.canCompleteTowerPattern(t, ruin)) return t;
        return null;
    }

    public static UnitType chooseTowerType(RobotController rc) throws GameActionException {
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        int paintCount = 0, moneyCount = 0;
        for (RobotInfo r : allies) {
            if (!r.getType().isTowerType()) continue;
            UnitType t = r.getType();
            if (t==UnitType.LEVEL_ONE_PAINT_TOWER||t==UnitType.LEVEL_TWO_PAINT_TOWER||t==UnitType.LEVEL_THREE_PAINT_TOWER) paintCount++;
            else if (t==UnitType.LEVEL_ONE_MONEY_TOWER||t==UnitType.LEVEL_TWO_MONEY_TOWER||t==UnitType.LEVEL_THREE_MONEY_TOWER) moneyCount++;
        }
        if (paintCount < 2) return UnitType.LEVEL_ONE_PAINT_TOWER;
        if (moneyCount < 2) return UnitType.LEVEL_ONE_MONEY_TOWER;
        return UnitType.LEVEL_ONE_PAINT_TOWER;
    }

    public static RobotInfo weakestEnemyTower(RobotInfo[] enemies) {
        RobotInfo best = null;
        for (RobotInfo e : enemies) {
            if (!e.getType().isTowerType()) continue;
            if (best == null || e.getHealth() < best.getHealth()) best = e;
        }
        return best;
    }
}