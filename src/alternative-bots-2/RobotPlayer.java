package alt_3;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.PaintType;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;
import battlecode.common.Clock;

public class RobotPlayer {

    static final Direction[] DIRS = {
            Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
            Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
    };

    private static Direction explorationDir = null;

    public static void run(RobotController rc) throws GameActionException {
        while (true) {
            try {
                switch (rc.getType()) {
                    case SOLDIER:
                        runSoldier(rc);
                        break;
                    case MOPPER:
                        runMopper(rc);
                        break;
                    case SPLASHER:
                        runSplasher(rc);
                        break;
                    default:
                        runTower(rc);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Clock.yield();
        }
    }

    public static void runTower(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        if (enemies.length > 0) {
            if (enemies.length > 1 && rc.canAttack(null)) {
                rc.attack(null);
            }
            MapLocation bestSingleTarget = Helper.getGreedySingleTarget(enemies);
            if (bestSingleTarget != null && rc.canAttack(bestSingleTarget)) {
                rc.attack(bestSingleTarget);
            }
        }

        if (rc.isActionReady()) {
            UnitType typeToSpawn = UnitType.SOLDIER;

            if (enemies.length >= 3) {
                typeToSpawn = UnitType.SPLASHER;
            } else if (Helper.isMopperNeeded(rc)) {
                typeToSpawn = UnitType.MOPPER;
            }

            int SAFE_CHIP_RESERVE = UnitType.LEVEL_ONE_MONEY_TOWER.moneyCost;

            boolean bypassReserve = rc.getRoundNum() < 20;

            if ((rc.getChips() > SAFE_CHIP_RESERVE || bypassReserve) && rc.getPaint() >= typeToSpawn.paintCost) {
                MapLocation bestSpawnLoc = Helper.getGreedySpawnLocation(rc, typeToSpawn);
                if (bestSpawnLoc != null && rc.canBuildRobot(typeToSpawn, bestSpawnLoc)) {
                    rc.buildRobot(typeToSpawn, bestSpawnLoc);
                }
            }
        }
    }

    public static void runSoldier(RobotController rc) throws GameActionException {
        boolean actionDone = false;
        boolean isBuildingRuin = false;
        MapLocation activeRuinLoc = null;

        if (rc.isActionReady()) {
            MapLocation[] ruins = rc.senseNearbyRuins(-1);
            if (ruins.length > 0) {
                MapLocation targetRuin = null;

                for (MapLocation r : ruins) {
                    RobotInfo botAtRuin = rc.canSenseRobotAtLocation(r) ? rc.senseRobotAtLocation(r) : null;
                    if (botAtRuin == null || !botAtRuin.getType().isTowerType()) {
                        targetRuin = r;
                        break;
                    }
                }

                if (targetRuin != null) {
                    activeRuinLoc = targetRuin;

                    UnitType towerToBuild;

                    RobotInfo[] enemiesNearRuin = rc.senseNearbyRobots(activeRuinLoc, 25, rc.getTeam().opponent());
                    if (enemiesNearRuin.length > 0) {
                        towerToBuild = UnitType.LEVEL_ONE_DEFENSE_TOWER;
                    } else if (rc.getChips() < 2500) {
                        towerToBuild = UnitType.LEVEL_ONE_MONEY_TOWER;
                    } else {
                        towerToBuild = UnitType.LEVEL_ONE_PAINT_TOWER;
                    }

                    if (rc.canCompleteTowerPattern(towerToBuild, activeRuinLoc)) {
                        rc.completeTowerPattern(towerToBuild, activeRuinLoc);
                        actionDone = true;
                        isBuildingRuin = true;
                    } else if (rc.canMarkTowerPattern(towerToBuild, activeRuinLoc)) {
                        rc.markTowerPattern(towerToBuild, activeRuinLoc);
                        actionDone = true;
                        isBuildingRuin = true;
                    } else {
                        MapLocation blueprintTile = Helper.getBlueprintPaintTarget(rc, activeRuinLoc);

                        if (blueprintTile != null) {
                            isBuildingRuin = true;

                            if (rc.canAttack(blueprintTile)) {
                                PaintType requiredPaint = rc.senseMapInfo(blueprintTile).getMark();
                                boolean useSecondary = (requiredPaint == PaintType.ALLY_SECONDARY);
                                rc.attack(blueprintTile, useSecondary);
                                actionDone = true;
                            }
                        } else {
                            isBuildingRuin = false;
                        }
                    }
                }
            }

            if (!actionDone) {
                RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                MapLocation targetEnemyLoc = Helper.getGreedyEnemyTarget(rc, enemies);

                if (targetEnemyLoc != null && rc.canAttack(targetEnemyLoc)) {
                    rc.attack(targetEnemyLoc);
                    actionDone = true;
                }
            }

            if (!actionDone) {
                MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);
                MapLocation bestTileToPaint = Helper.getGreedyPaintTarget(rc, nearbyTiles);

                if (bestTileToPaint != null && rc.canAttack(bestTileToPaint)) {
                    rc.attack(bestTileToPaint);
                    actionDone = true;
                }
            }
        }

        if (rc.isMovementReady()) {
            if (isBuildingRuin && activeRuinLoc != null) {
                int distSq = rc.getLocation().distanceSquaredTo(activeRuinLoc);
                if (distSq > 2) {
                    Helper.moveTowards(rc, activeRuinLoc);
                } else {
                    Helper.moveRandomly(rc);
                }
                return;
            }

            if (explorationDir == null) {
                explorationDir = DIRS[rc.getID() % 8];
            }

            Direction bestDir = Direction.CENTER;
            int maxMoveScore = -99999;

            for (Direction dir : DIRS) {
                if (rc.canMove(dir)) {
                    MapLocation nextLoc = rc.adjacentLocation(dir);

                    int moveScore = Helper.evaluateFrontierExpansionScore(rc, nextLoc);

                    if (dir == explorationDir) {
                        moveScore += 5;
                    } else if (dir == explorationDir.rotateLeft() || dir == explorationDir.rotateRight()) {
                        moveScore += 2;
                    } else if (dir == explorationDir.opposite()) {
                        moveScore -= 8;
                    }

                    if (moveScore > maxMoveScore) {
                        maxMoveScore = moveScore;
                        bestDir = dir;
                    }
                }
            }

            if (bestDir != Direction.CENTER && rc.canMove(bestDir)) {
                rc.move(bestDir);

                explorationDir = bestDir;
            } else {
                explorationDir = DIRS[(int) (Math.random() * 8)];
            }
        }
    }

    public static void runSplasher(RobotController rc) throws GameActionException {
        MapLocation bestAttackTarget = null;

        if (rc.isActionReady()) {
            MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);
            RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

            bestAttackTarget = Helper.getGreedyAoETarget(rc, nearbyTiles, nearbyEnemies);

            if (bestAttackTarget != null && rc.canAttack(bestAttackTarget)) {
                rc.attack(bestAttackTarget);
            }
        }

        if (rc.isMovementReady()) {
            Direction bestDir = Direction.CENTER;
            int maxMoveScore = -99999;

            for (Direction dir : DIRS) {
                if (rc.canMove(dir)) {
                    MapLocation nextLoc = rc.adjacentLocation(dir);
                    int moveScore = Helper.evaluateSplasherMovementScore(rc, nextLoc, bestAttackTarget);

                    if (moveScore > maxMoveScore || (moveScore == maxMoveScore && Math.random() > 0.5)) {
                        maxMoveScore = moveScore;
                        bestDir = dir;
                    }
                }
            }

            if (bestDir != Direction.CENTER && rc.canMove(bestDir)) {
                rc.move(bestDir);
            }
        }
    }

    public static void runMopper(RobotController rc) throws GameActionException {
        boolean hasActed = false;

        if (rc.isActionReady()) {
            Direction bestSwingDir = Helper.getGreedyMopSwingDir(rc);
            if (bestSwingDir != null && rc.canMopSwing(bestSwingDir)) {
                rc.mopSwing(bestSwingDir);
                hasActed = true;
            }

            if (!hasActed) {
                MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);
                MapLocation bestCleanLoc = Helper.getGreedyCleanTarget(rc, nearbyTiles);

                if (bestCleanLoc != null && rc.canAttack(bestCleanLoc)) {
                    rc.attack(bestCleanLoc);
                    hasActed = true;
                }
            }
        }

        if (!hasActed && rc.isActionReady() && rc.getPaint() >= 50) {
            RobotInfo[] nearbyAllies = rc.senseNearbyRobots(2, rc.getTeam());
            for (RobotInfo ally : nearbyAllies) {
                if (ally.getType() == UnitType.SOLDIER || ally.getType() == UnitType.SPLASHER) {

                    int paintNeeded = ally.getType().paintCapacity - ally.getPaintAmount();

                    if (paintNeeded >= 50 && rc.canTransferPaint(ally.getLocation(), 50)) {
                        rc.transferPaint(ally.getLocation(), 50);
                        hasActed = true;
                        break;
                    }
                }
            }
        }

        if (rc.isMovementReady()) {
            Direction bestDir = Direction.CENTER;
            int maxMoveScore = -99999;

            for (Direction dir : DIRS) {
                if (rc.canMove(dir)) {
                    MapLocation nextLoc = rc.adjacentLocation(dir);
                    int moveScore = Helper.evaluateMopperMovementScore(rc, nextLoc);

                    if (moveScore > maxMoveScore || (moveScore == maxMoveScore && Math.random() > 0.5)) {
                        maxMoveScore = moveScore;
                        bestDir = dir;
                    }
                }
            }

            if (bestDir != Direction.CENTER && rc.canMove(bestDir)) {
                rc.move(bestDir);
            }
        }
    }
}