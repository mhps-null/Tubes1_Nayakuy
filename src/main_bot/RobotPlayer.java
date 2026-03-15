package main_bot;

import battlecode.common.*;

public class RobotPlayer {

    static final Direction[] DIRS = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static Direction currentExploreDir = null;
    static Direction currentSplasherDir = null;

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

    static void runTower(RobotController rc) throws GameActionException {
        int currentChips = rc.getChips();
        int currentPaint = rc.getPaint();
        int currentRound = rc.getRoundNum();
        MapLocation myLoc = rc.getLocation();

        if (rc.isActionReady()) {
            RobotInfo[] enemiesToAttack = rc.senseNearbyRobots(rc.getType().actionRadiusSquared,
                    rc.getTeam().opponent());

            if (enemiesToAttack.length > 0) {
                MapLocation bestTarget = null;
                int lowestHP = 999999;

                for (RobotInfo enemy : enemiesToAttack) {
                    if (enemy.getHealth() < lowestHP && rc.canAttack(enemy.getLocation())) {
                        lowestHP = enemy.getHealth();
                        bestTarget = enemy.getLocation();
                    }
                }

                if (bestTarget != null) {
                    rc.attack(bestTarget);
                }
            }
        }

        if (rc.isActionReady() && rc.canUpgradeTower(myLoc)) {
            if (currentChips > 5000) {
                rc.upgradeTower(myLoc);
                return;
            }
        }

        MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);
        boolean hasUnclaimedRuin = false;

        for (MapLocation ruin : nearbyRuins) {
            if (rc.canSenseLocation(ruin) && rc.senseRobotAtLocation(ruin) == null) {
                hasUnclaimedRuin = true;
                break;
            }
        }

        MapLocation spawnLoc = Helper.findSpawnLocation(rc);

        if (spawnLoc != null) {
            boolean isEarlyGameExplore = currentRound < 250;

            if (hasUnclaimedRuin || isEarlyGameExplore) {
                if (currentChips >= UnitType.SOLDIER.moneyCost && currentPaint >= UnitType.SOLDIER.paintCost) {
                    if (rc.canBuildRobot(UnitType.SOLDIER, spawnLoc)) {
                        rc.buildRobot(UnitType.SOLDIER, spawnLoc);
                        return;
                    }
                }
            }

            boolean isEconomyBooming = currentChips > 3000;
            boolean isLateGame = currentRound > 1250;

            if (isEconomyBooming || isLateGame) {
                if (currentChips >= UnitType.SPLASHER.moneyCost
                        && currentPaint >= UnitType.SPLASHER.paintCost) {
                    if (rc.canBuildRobot(UnitType.SPLASHER, spawnLoc)) {
                        rc.buildRobot(UnitType.SPLASHER, spawnLoc);
                        return;
                    }
                }
            }

            RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
            if (enemies.length > 0) {
                if (currentChips >= UnitType.MOPPER.moneyCost && currentPaint >= UnitType.MOPPER.paintCost) {
                    if (rc.canBuildRobot(UnitType.MOPPER, spawnLoc)) {
                        rc.buildRobot(UnitType.MOPPER, spawnLoc);
                        return;
                    }
                }
            }
        }

        if (rc.isActionReady()) {

            if (rc.canUpgradeTower(myLoc)) {
                rc.upgradeTower(myLoc);
            }
        }
    }

    static void runSoldier(RobotController rc) throws GameActionException {
        if (!rc.isActionReady() && !rc.isMovementReady()) {
            return;
        }

        MapLocation myLoc = rc.getLocation();
        int currentRound = rc.getRoundNum();
        MapLocation[] nearbyRuins = rc.senseNearbyRuins(-1);

        int myChips = rc.getChips();
        int myPaint = rc.getPaint();

        UnitType towerToBuild;

        if (myChips > 2500) {
            towerToBuild = UnitType.LEVEL_ONE_PAINT_TOWER;
        } else if (myPaint > 2000 && myChips < 1000) {
            towerToBuild = UnitType.LEVEL_ONE_MONEY_TOWER;
        } else {
            towerToBuild = (currentRound <= 125) ? UnitType.LEVEL_ONE_MONEY_TOWER : UnitType.LEVEL_ONE_PAINT_TOWER;
        }

        if (rc.getRoundNum() < 100) {

            MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(100);
            int allyPaintCount = 0;

            for (MapInfo tile : nearbyTiles) {
                PaintType mark = tile.getMark();
                if (mark == PaintType.ALLY_PRIMARY || mark == PaintType.ALLY_SECONDARY) {
                    allyPaintCount++;
                }
            }

            if (allyPaintCount < 40) {

                MapLocation targetSRPLoc = null;

                MapLocation[] candidateSpots = rc.getAllLocationsWithinRadiusSquared(myLoc, 64);

                for (MapLocation candidate : candidateSpots) {
                    if (rc.canMarkResourcePattern(candidate)) {

                        boolean isAreaCompletelyClean = true;

                        for (int dx = -2; dx <= 2; dx++) {
                            for (int dy = -2; dy <= 2; dy++) {
                                MapLocation checkLoc = candidate.translate(dx, dy);

                                if (rc.canSenseLocation(checkLoc)) {
                                    PaintType existingMark = rc.senseMapInfo(checkLoc).getMark();

                                    if (existingMark != PaintType.EMPTY) {
                                        isAreaCompletelyClean = false;
                                        break;
                                    }
                                }
                            }
                            if (!isAreaCompletelyClean)
                                break;
                        }

                        if (isAreaCompletelyClean) {
                            targetSRPLoc = candidate;
                            break;
                        }
                    }
                }

                if (targetSRPLoc != null) {
                    if (myLoc.equals(targetSRPLoc)) {
                        if (rc.isActionReady()) {
                            rc.markResourcePattern(targetSRPLoc);
                            return;
                        }
                    } else if (rc.isMovementReady()) {
                        Helper.moveToLoc(rc, targetSRPLoc);
                        return;
                    }
                }
            }
        }

        if (nearbyRuins.length > 0 && rc.isActionReady()) {
            MapLocation targetRuin = nearbyRuins[0];
            if (rc.canCompleteTowerPattern(towerToBuild, targetRuin)) {
                rc.completeTowerPattern(towerToBuild, targetRuin);
                return;
            }
        }
        if (rc.isActionReady() && rc.canCompleteResourcePattern(myLoc)) {
            rc.completeResourcePattern(myLoc);
            return;
        }

        if (rc.isActionReady() || rc.isMovementReady()) {
            MapInfo[] nearbyTiles = rc.senseNearbyMapInfos(-1);
            boolean hasPainted = false;
            MapLocation targetUnpaintedTile = null;

            for (MapInfo tile : nearbyTiles) {
                PaintType mark = tile.getMark();
                PaintType paint = tile.getPaint();
                MapLocation tileLoc = tile.getMapLocation();

                if ((mark == PaintType.ALLY_PRIMARY && paint != PaintType.ALLY_PRIMARY) ||
                        (mark == PaintType.ALLY_SECONDARY && paint != PaintType.ALLY_SECONDARY)) {

                    boolean needsSecondary = (mark == PaintType.ALLY_SECONDARY);

                    boolean inRange = myLoc.distanceSquaredTo(tileLoc) <= rc.getType().actionRadiusSquared;

                    if (inRange) {
                        if (rc.canAttack(tileLoc)) {

                            if (!needsSecondary) {
                                rc.attack(tileLoc);
                            } else {
                                rc.attack(tileLoc, true);
                            }

                            hasPainted = true;
                            break;
                        }

                    } else {
                        if (targetUnpaintedTile == null) {
                            targetUnpaintedTile = tileLoc;
                        }
                    }
                }
            }

            if (hasPainted)
                return;

            if (rc.isMovementReady()) {
                if (targetUnpaintedTile != null) {
                    Helper.moveToLoc(rc, targetUnpaintedTile);
                    return;
                } else if (nearbyRuins.length > 0) {
                    MapLocation targetRuin = null;

                    for (MapLocation ruin : nearbyRuins) {
                        RobotInfo occupant = rc.senseRobotAtLocation(ruin);

                        if (occupant == null) {
                            targetRuin = ruin;
                            break;
                        }
                    }

                    if (targetRuin != null) {
                        if (rc.isActionReady() && rc.canMarkTowerPattern(towerToBuild, targetRuin)) {
                            rc.markTowerPattern(towerToBuild, targetRuin);
                            return;
                        }
                        Helper.moveToLoc(rc, targetRuin);
                        return;
                    }
                }
            }
        }

        if (rc.isMovementReady()) {
            explore(rc);
        }
    }

    static void runSplasher(RobotController rc) throws GameActionException {
        boolean canMove = rc.isMovementReady();

        boolean isOutOfPaint = rc.getPaint() < 50;

        if (isOutOfPaint) {
            if (canMove) {
                Helper.retreatForRefill(rc);
            }
            return;
        }

        boolean canAttack = rc.isActionReady();

        if (!canAttack && !canMove)
            return;

        if (canAttack) {
            MapLocation bestTarget = Helper.getBestSplashTarget(rc);
            if (bestTarget != null && rc.canAttack(bestTarget)) {
                rc.attack(bestTarget);
                canAttack = false;
            }
        }

        if (rc.isMovementReady()) {
            moveTowardsUnpaintedArea(rc);
        }
    }

    static void runMopper(RobotController rc) throws GameActionException {
        boolean canAct = rc.isActionReady();
        boolean canMove = rc.isMovementReady();

        if (!canAct && !canMove)
            return;

        MapLocation myLoc = rc.getLocation();
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

        if (rc.getPaint() < 30) {
            if (canMove) {
                RobotInfo nearestTower = Helper.getNearestPaintTower(myLoc, allies);
                if (nearestTower != null) {
                    Helper.moveToLoc(rc, nearestTower.getLocation());
                } else {
                    explore(rc);
                }
            }
            return;
        }

        if (enemies.length > 0) {
            RobotInfo closestEnemy = Helper.getClosestRobot(myLoc, enemies);
            if (closestEnemy != null) {
                if (canAct && rc.canAttack(closestEnemy.getLocation())) {
                    rc.attack(closestEnemy.getLocation());
                    canAct = false;
                }
                if (canMove && rc.isMovementReady()) {
                    Helper.moveToLoc(rc, closestEnemy.getLocation());
                    canMove = false;
                }
            }
        }

        if (canAct || canMove) {
            RobotInfo needySplasher = Helper.getNeediestSplasher(myLoc, allies);

            if (needySplasher != null) {
                int distSquared = myLoc.distanceSquaredTo(needySplasher.getLocation());

                if (canAct && distSquared <= rc.getType().actionRadiusSquared) {
                    int amountNeeded = needySplasher.getType().paintCapacity - needySplasher.getPaintAmount();

                    int amountToGive = Math.min(rc.getPaint() - 30, amountNeeded);

                    if (amountToGive > 0 && rc.canTransferPaint(needySplasher.getLocation(), amountToGive)) {
                        rc.transferPaint(needySplasher.getLocation(), amountToGive);
                        canAct = false;
                    }
                }

                if (canMove && rc.isMovementReady()) {
                    Helper.moveToLoc(rc, needySplasher.getLocation());
                    canMove = false;
                }
            }
        }

        if (canMove && rc.isMovementReady()) {
            explore(rc);
        }
    }

    static void explore(RobotController rc) throws GameActionException {
        if (currentExploreDir == null || !rc.canMove(currentExploreDir)) {
            currentExploreDir = Helper.chooseNewExplorationDirection(rc);
        }

        if (rc.canMove(currentExploreDir)) {
            rc.move(currentExploreDir);
        } else if (rc.canMove(currentExploreDir.rotateLeft())) {
            rc.move(currentExploreDir.rotateLeft());
            currentExploreDir = currentExploreDir.rotateLeft();
        } else if (rc.canMove(currentExploreDir.rotateRight())) {
            rc.move(currentExploreDir.rotateRight());
            currentExploreDir = currentExploreDir.rotateRight();
        } else {
            currentExploreDir = null;
        }
    }

    static void moveTowardsUnpaintedArea(RobotController rc) throws GameActionException {
        MapLocation myLoc = rc.getLocation();

        Direction bestDir = null;
        int maxScore = -999999;

        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

        Direction[] dirs = Direction.allDirections();
        for (Direction dir : dirs) {
            if (dir == Direction.CENTER || !rc.canMove(dir))
                continue;

            int dirScore = 0;
            MapLocation targetLoc = myLoc.add(dir);

            for (RobotInfo ally : allies) {
                if (ally.getType() == UnitType.SPLASHER) {
                    int distSquared = targetLoc.distanceSquaredTo(ally.getLocation());
                    if (distSquared < 16) {
                        dirScore -= 500;
                    }
                }
            }

            MapLocation scout1 = targetLoc;
            MapLocation scout2 = targetLoc.translate(dir.dx * 2, dir.dy * 2);

            MapLocation[] scouts = { scout1, scout2 };
            for (MapLocation scoutLoc : scouts) {
                if (rc.canSenseLocation(scoutLoc)) {
                    PaintType paint = rc.senseMapInfo(scoutLoc).getPaint();

                    if (paint == PaintType.ENEMY_PRIMARY || paint == PaintType.ENEMY_SECONDARY) {
                        dirScore += 100;
                    } else if (paint == PaintType.EMPTY) {
                        dirScore += 60;
                    } else {
                        dirScore -= 200;
                    }
                }
            }

            if (dir == currentSplasherDir) {
                dirScore += 30;
            }

            if (dirScore > maxScore) {
                maxScore = dirScore;
                bestDir = dir;
            }
        }

        if (bestDir != null) {
            rc.move(bestDir);
            currentSplasherDir = bestDir;
        } else {
            if (currentSplasherDir != null) {
                currentSplasherDir = currentSplasherDir.rotateRight();
            } else {
                currentSplasherDir = Direction.NORTH;
            }
        }
    }
}