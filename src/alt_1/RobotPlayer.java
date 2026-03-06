package alt_1;

import battlecode.common.*;

public class RobotPlayer {
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

    static final int STATE_EXPLORING = 0;
    static final int STATE_BUILDING_TOWER = 1;
    static final int STATE_REFUELING = 2;

    static int soldierState = 0;
    static int prevSoldierState = 0;
    static MapLocation ruinTarget = null;
    static Direction splasherLastDir = Direction.NORTH;

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
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        // Greedy: Single attack: serang musuh dengan HP terendah
        if (rc.isActionReady()) {
            RobotInfo targetSingle = null;
            int lowestHP = Integer.MAX_VALUE;

            for (RobotInfo enemy : enemies) {
                if (rc.canAttack(enemy.getLocation())) {
                    if (enemy.getHealth() < lowestHP) {
                        lowestHP = enemy.getHealth();
                        targetSingle = enemy;
                    }
                }
            }

            if (targetSingle != null) {
                rc.attack(targetSingle.getLocation());
            }
        }

        // Greedy: AoE attack: serang musuh terbanyak dalam radius
        if (rc.isActionReady()) {
            MapLocation bestAoE = null;
            int bestCount = 0;

            for (RobotInfo enemy : enemies) {
                int count = 0;
                for (RobotInfo other : enemies) {
                    if (Helper.distSq(enemy.getLocation(), other.getLocation()) <= 4) {
                        count++;
                    }
                }
                if (count > bestCount && rc.canAttack(enemy.getLocation())) {
                    bestCount = count;
                    bestAoE = enemy.getLocation();
                }
            }

            if (bestAoE != null) {
                rc.attack(bestAoE);
            }
        }

        RobotInfo[] allAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        int totalRobots = 0;
        int totalMoppers = 0;
        int totalSplashers = 0;

        for (RobotInfo r : allAllies) {
            if (r.getType().isTowerType())
                continue;

            totalRobots++;
            if (r.getType() == UnitType.MOPPER)
                totalMoppers++;
            if (r.getType() == UnitType.SPLASHER)
                totalSplashers++;
        }

        double ratioMopper = totalRobots > 0 ? (double) totalMoppers / totalRobots : 0;
        double ratioSplasher = totalRobots > 0 ? (double) totalSplashers / totalRobots : 0;

        if (rc.isActionReady()) {

            // Greedy: respawn robot yang paling dibutuhkan
            UnitType toSpawn = null;

            if (ratioMopper < 0.25) {
                toSpawn = UnitType.MOPPER;
            } else if (ratioSplasher < 0.15 && rc.getChips() > 600) {
                toSpawn = UnitType.SPLASHER;
            } else {
                toSpawn = UnitType.SOLDIER;
            }

            MapLocation bestSpawn = null;
            int bestSpawnScore = Integer.MIN_VALUE;

            for (Direction dir : directions) {
                MapLocation candidate = rc.getLocation().add(dir);
                if (!rc.canBuildRobot(toSpawn, candidate))
                    continue;

                int spawnScore = 0;
                try {
                    MapInfo tileInfo = rc.senseMapInfo(candidate);
                    if (tileInfo.getPaint().isAlly())
                        spawnScore += 5;
                    if (tileInfo.getPaint() == PaintType.EMPTY)
                        spawnScore += 2;
                    if (tileInfo.getPaint().isEnemy())
                        spawnScore -= 3;
                } catch (GameActionException e) {
                }

                if (spawnScore > bestSpawnScore) {
                    bestSpawnScore = spawnScore;
                    bestSpawn = candidate;
                }
            }

            if (bestSpawn != null) {
                rc.buildRobot(toSpawn, bestSpawn);
            }
        }

        MapLocation ruinLoc = Helper.nearestEmptyRuin(rc);
        if (ruinLoc != null) {
            Direction dirToRuin = rc.getLocation().directionTo(ruinLoc);
            MapLocation markerLoc = ruinLoc.subtract(dirToRuin);

            try {
                if (rc.canMark(markerLoc)) {
                    rc.mark(markerLoc, false);
                }
            } catch (GameActionException e) {
            }
        }
    }

    static void runSoldier(RobotController rc) throws GameActionException {

        if (rc.getPaint() < 80) {
            prevSoldierState = soldierState;
            soldierState = STATE_REFUELING;
        }

        if (soldierState == STATE_REFUELING) {

            RobotInfo tower = Helper.nearestAllyTower(rc);

            if (tower != null) {
                MapLocation towerLoc = tower.getLocation();

                if (Helper.distSq(rc.getLocation(), towerLoc) <= 2) {
                    if (rc.isActionReady()) {
                        int paintNeeded = rc.getType().paintCapacity - rc.getPaint();
                        if (rc.canTransferPaint(towerLoc, -paintNeeded)) {
                            rc.transferPaint(towerLoc, -paintNeeded);
                        }
                    }
                } else {
                    Helper.moveToward(rc, towerLoc);
                }
            } else {
                Direction best = null;
                int bestAllyTiles = -1;

                for (Direction dir : directions) {
                    if (!rc.canMove(dir))
                        continue;
                    MapLocation next = rc.getLocation().add(dir);
                    MapInfo[] tilesAround = rc.senseNearbyMapInfos(next, 4);
                    int allyCount = 0;
                    for (MapInfo t : tilesAround) {
                        if (t.getPaint().isAlly())
                            allyCount++;
                    }
                    if (allyCount > bestAllyTiles) {
                        bestAllyTiles = allyCount;
                        best = dir;
                    }
                }

                if (best != null && rc.canMove(best))
                    rc.move(best);
            }

            if (rc.getPaint() >= rc.getType().paintCapacity * 0.8) {
                soldierState = prevSoldierState;
            }
            return;
        }

        if (soldierState == STATE_EXPLORING && rc.getPaint() > 120) {
            MapLocation ruin = Helper.nearestEmptyRuin(rc);
            if (ruin != null) {
                ruinTarget = ruin;
                soldierState = STATE_BUILDING_TOWER;
            }
        }

        if (soldierState == STATE_BUILDING_TOWER) {

            if (ruinTarget == null) {
                soldierState = STATE_EXPLORING;
                return;
            }

            try {
                RobotInfo existing = rc.senseRobotAtLocation(ruinTarget);
                if (existing != null && existing.getType().isTowerType()) {
                    ruinTarget = null;
                    soldierState = STATE_EXPLORING;
                    return;
                }
            } catch (GameActionException e) {
            }

            if (Helper.distSq(rc.getLocation(), ruinTarget) > 8) {
                Helper.moveToward(rc, ruinTarget);
                return;
            }

            UnitType towerType = Helper.neededTowerType(rc);

            if (rc.canMarkTowerPattern(towerType, ruinTarget)) {
                rc.markTowerPattern(towerType, ruinTarget);
            }

            // Greedy: pilih petak yang paling salah
            MapInfo bestTile = null;
            int bestScore = -1;

            MapInfo[] patternTiles = rc.senseNearbyMapInfos(ruinTarget, 8);
            for (MapInfo tile : patternTiles) {
                if (tile.getMark() == PaintType.EMPTY)
                    continue;
                if (tile.getMark() == tile.getPaint())
                    continue;

                if (rc.canAttack(tile.getMapLocation())) {
                    int allyNear = rc.senseNearbyRobots(
                            tile.getMapLocation(), 2, rc.getTeam()).length;
                    int tileScore = 10 - allyNear;

                    if (tileScore > bestScore) {
                        bestScore = tileScore;
                        bestTile = tile;
                    }
                }
            }

            if (bestTile != null && rc.isActionReady()) {
                boolean useSecondary = (bestTile.getMark() == PaintType.ALLY_SECONDARY);
                rc.attack(bestTile.getMapLocation(), useSecondary);
            } else if (bestTile == null) {
                Helper.moveToward(rc, ruinTarget);
            }

            if (rc.canCompleteTowerPattern(towerType, ruinTarget)) {
                rc.completeTowerPattern(towerType, ruinTarget);
                ruinTarget = null;
                soldierState = STATE_EXPLORING;
            }

            return;
        }

        // Greedy: Pilih tile dengan skor tertinggi
        if (rc.isActionReady()) {
            MapInfo bestAttack = null;
            int bestAttackScore = 0;

            MapInfo[] attackable = rc.senseNearbyMapInfos(
                    rc.getLocation(), 9);

            for (MapInfo tile : attackable) {
                if (!rc.canAttack(tile.getMapLocation()))
                    continue;

                PaintType paint = tile.getPaint();
                int tileScore = 0;

                if (paint.isEnemy())
                    tileScore = 15;
                else if (paint == PaintType.EMPTY)
                    tileScore = 8;

                if (tileScore > bestAttackScore) {
                    bestAttackScore = tileScore;
                    bestAttack = tile;
                }
            }

            if (bestAttack != null) {
                boolean useSecondary = (bestAttack.getMark() == PaintType.ALLY_SECONDARY);
                rc.attack(bestAttack.getMapLocation(), useSecondary);
            }
        }

        if (rc.isMovementReady()) {
            Direction best = Helper.bestMoveDirection(rc);
            if (best != null)
                rc.move(best);
        }
    }

    static void runMopper(RobotController rc) throws GameActionException {
        if (rc.getPaint() < 60) {
            if (Helper.isOnEnemyTerritory(rc)) {
                Direction best = null;
                int bestAlly = -1;

                for (Direction dir : directions) {
                    if (!rc.canMove(dir))
                        continue;
                    MapLocation next = rc.getLocation().add(dir);
                    MapInfo[] around = rc.senseNearbyMapInfos(next, 4);
                    int allyCount = 0;
                    for (MapInfo t : around) {
                        if (t.getPaint().isAlly())
                            allyCount++;
                    }
                    if (allyCount > bestAlly) {
                        bestAlly = allyCount;
                        best = dir;
                    }
                }

                if (best != null && rc.canMove(best))
                    rc.move(best);
                return;
            }

            RobotInfo tower = Helper.nearestAllyTower(rc);
            if (tower != null) {
                MapLocation towerLoc = tower.getLocation();
                if (Helper.distSq(rc.getLocation(), towerLoc) <= 2) {
                    if (rc.isActionReady()) {
                        int needed = rc.getType().paintCapacity - rc.getPaint();
                        if (rc.canTransferPaint(towerLoc, -needed)) {
                            rc.transferPaint(towerLoc, -needed);
                        }
                    }
                } else {
                    Helper.moveToward(rc, towerLoc);
                }
            }
            return;
        }

        if (rc.isActionReady()) {
            RobotInfo needy = Helper.mostNeedyAlly(rc, 2);

            if (needy != null && needy.getPaintAmount() < 80) {
                int canGive = rc.getPaint() - 60;
                if (canGive > 0) {
                    int toTransfer = Math.min(canGive,
                            needy.getType().paintCapacity - needy.getPaintAmount());
                    if (rc.canTransferPaint(needy.getLocation(), toTransfer)) {
                        rc.transferPaint(needy.getLocation(), toTransfer);
                    }
                }
            }
        }

        if (rc.isActionReady()) {
            RobotInfo bestEnemyRobot = null;
            int lowestEnemyPaint = Integer.MAX_VALUE;

            RobotInfo[] enemies = rc.senseNearbyRobots(2, rc.getTeam().opponent());
            for (RobotInfo enemy : enemies) {
                if (rc.canAttack(enemy.getLocation())) {
                    // Greedy: targetkan musuh dengan cat paling sedikit
                    if (enemy.getPaintAmount() < lowestEnemyPaint) {
                        lowestEnemyPaint = enemy.getPaintAmount();
                        bestEnemyRobot = enemy;
                    }
                }
            }

            if (bestEnemyRobot != null) {
                rc.attack(bestEnemyRobot.getLocation());
            }
        }

        if (rc.isActionReady()) {
            Direction bestSwingDir = null;
            int bestSwingCount = 1;

            Direction[] cardinals = {
                    Direction.NORTH, Direction.SOUTH,
                    Direction.EAST, Direction.WEST
            };

            for (Direction dir : cardinals) {
                MapLocation step1 = rc.getLocation().add(dir);
                MapLocation step2 = step1.add(dir);

                int count = 0;

                RobotInfo[] nearStep1 = rc.senseNearbyRobots(step1, 1, rc.getTeam().opponent());
                RobotInfo[] nearStep2 = rc.senseNearbyRobots(step2, 1, rc.getTeam().opponent());
                count = nearStep1.length + nearStep2.length;

                if (count > bestSwingCount) {
                    bestSwingCount = count;
                    bestSwingDir = dir;
                }
            }

            if (bestSwingDir != null && rc.canMopSwing(bestSwingDir)) {
                rc.mopSwing(bestSwingDir);
            }
        }

        if (rc.isActionReady()) {
            MapLocation bestTile = null;
            int bestTileScore = 0;

            MapInfo[] tiles = rc.senseNearbyMapInfos();
            for (MapInfo tile : tiles) {
                if (!tile.getPaint().isEnemy())
                    continue;
                if (!rc.canAttack(tile.getMapLocation()))
                    continue;

                int adjacentEnemyTiles = 0;
                MapInfo[] around = rc.senseNearbyMapInfos(tile.getMapLocation(), 2);
                for (MapInfo t : around) {
                    if (t.getPaint().isEnemy())
                        adjacentEnemyTiles++;
                }

                int tileScore = 15 + adjacentEnemyTiles;
                if (tileScore > bestTileScore) {
                    bestTileScore = tileScore;
                    bestTile = tile.getMapLocation();
                }
            }

            if (bestTile != null) {
                rc.attack(bestTile);
            }
        }

        if (rc.isMovementReady()) {

            RobotInfo needy = Helper.mostNeedyAlly(rc, 20);
            if (needy != null && needy.getPaintAmount() < 80) {
                int distToNeedy = Helper.distSq(rc.getLocation(), needy.getLocation());
                if (distToNeedy > 2) {
                    Helper.moveToward(rc, needy.getLocation());
                }
                return;
            }

            MapLocation enemyPaint = Helper.nearestEnemyPaint(rc);
            if (enemyPaint != null) {
                Helper.moveToward(rc, enemyPaint);
                return;
            }

            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
            RobotInfo nearestSoldier = null;
            int bestDist = Integer.MAX_VALUE;

            for (RobotInfo r : allies) {
                if (r.getType() == UnitType.SOLDIER) {
                    int d = Helper.distSq(rc.getLocation(), r.getLocation());
                    if (d < bestDist) {
                        bestDist = d;
                        nearestSoldier = r;
                    }
                }
            }

            if (nearestSoldier != null) {
                if (bestDist < 4) {
                    Helper.moveAway(rc, nearestSoldier.getLocation());
                } else {
                    Helper.moveToward(rc, nearestSoldier.getLocation());
                }
                return;
            }

            Direction best = Helper.bestMoveDirection(rc);
            if (best != null && rc.canMove(best))
                rc.move(best);
        }
    }

    static void runSplasher(RobotController rc) throws GameActionException {
        if (rc.getPaint() < 100) {
            RobotInfo tower = Helper.nearestAllyTower(rc);

            if (tower != null) {
                MapLocation towerLoc = tower.getLocation();
                if (Helper.distSq(rc.getLocation(), towerLoc) <= 2) {
                    if (rc.isActionReady()) {
                        int needed = rc.getType().paintCapacity - rc.getPaint();
                        if (rc.canTransferPaint(towerLoc, -needed)) {
                            rc.transferPaint(towerLoc, -needed);
                        }
                    }
                } else {
                    Helper.moveToward(rc, towerLoc);
                }
            } else {
                Direction best = null;
                int bestAlly = -1;

                for (Direction dir : directions) {
                    if (!rc.canMove(dir))
                        continue;
                    MapLocation next = rc.getLocation().add(dir);
                    MapInfo[] around = rc.senseNearbyMapInfos(next, 4);
                    int allyCount = 0;
                    for (MapInfo t : around) {
                        if (t.getPaint().isAlly())
                            allyCount++;
                    }
                    if (allyCount > bestAlly) {
                        bestAlly = allyCount;
                        best = dir;
                    }
                }

                if (best != null && rc.canMove(best))
                    rc.move(best);
            }
            return;
        }

        // Greed: cari titik serangan terbaik berd. skor
        if (rc.isActionReady()) {
            MapLocation bestCenter = null;
            int bestCenterScore = 8;

            MapInfo[] candidates = rc.senseNearbyMapInfos(rc.getLocation(), 4);

            for (MapInfo candidate : candidates) {
                MapLocation center = candidate.getMapLocation();

                if (!rc.canAttack(center))
                    continue;

                int score = 0;

                MapInfo[] affected = rc.senseNearbyMapInfos(center, 2);

                for (MapInfo affected_tile : affected) {
                    PaintType paint = affected_tile.getPaint();
                    if (paint.isEnemy())
                        score += 3;
                    else if (paint == PaintType.EMPTY)
                        score += 2;
                }

                try {
                    RobotInfo robotAtCenter = rc.senseRobotAtLocation(center);
                    if (robotAtCenter != null
                            && robotAtCenter.getTeam() == rc.getTeam().opponent()
                            && robotAtCenter.getType().isTowerType()) {
                        score += 5;
                    }
                } catch (GameActionException e) {
                }

                if (score > bestCenterScore) {
                    bestCenterScore = score;
                    bestCenter = center;
                }
            }

            if (bestCenter != null) {
                rc.attack(bestCenter);
                return;
            }
        }

        if (rc.isMovementReady()) {
            int nearbyAllies = Helper.countAdjacentAllies(rc);

            if (nearbyAllies > 2) {
                Direction bestEscape = null;
                int leastAllies = Integer.MAX_VALUE;

                for (Direction dir : directions) {
                    if (!rc.canMove(dir))
                        continue;
                    MapLocation next = rc.getLocation().add(dir);
                    int allies = rc.senseNearbyRobots(next, 2, rc.getTeam()).length;
                    if (allies < leastAllies) {
                        leastAllies = allies;
                        bestEscape = dir;
                    }
                }

                if (bestEscape != null) {
                    rc.move(bestEscape);
                    return;
                }
            }

        }

        // Greedy: gerak ke arah yang titik serangannya paling menguntungkan
        if (rc.isMovementReady()) {

            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo nearestEnemyTower = null;
            int bestTowerDist = Integer.MAX_VALUE;

            for (RobotInfo enemy : enemies) {
                if (enemy.getType().isTowerType()) {
                    int d = Helper.distSq(rc.getLocation(), enemy.getLocation());
                    if (d < bestTowerDist) {
                        bestTowerDist = d;
                        nearestEnemyTower = enemy;
                    }
                }
            }

            if (nearestEnemyTower != null) {
                Helper.moveToward(rc, nearestEnemyTower.getLocation());
                return;
            }

            Direction best = null;
            int bestAreaScore = Integer.MIN_VALUE;

            for (Direction dir : directions) {
                if (!rc.canMove(dir))
                    continue;

                MapLocation next = rc.getLocation().add(dir);
                int areaScore = 0;

                MapInfo[] around = rc.senseNearbyMapInfos(next, 4);
                for (MapInfo t : around) {
                    PaintType paint = t.getPaint();
                    if (paint.isEnemy())
                        areaScore += 3;
                    else if (paint == PaintType.EMPTY)
                        areaScore += 2;
                }

                int allies = rc.senseNearbyRobots(next, 2, rc.getTeam()).length;
                areaScore -= allies * 5;

                if (dir == splasherLastDir || dir == splasherLastDir.rotateLeft()
                        || dir == splasherLastDir.rotateRight()) {
                    areaScore += 3;
                }

                if (dir == splasherLastDir.opposite()) {
                    areaScore -= 5;
                }

                if (areaScore > bestAreaScore) {
                    bestAreaScore = areaScore;
                    best = dir;
                }
            }

            if (best != null) {
                splasherLastDir = best;
                rc.move(best);
            }
        }
    }
}