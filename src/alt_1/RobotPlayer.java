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

    static int[] soldierState = new int[10000];
    static int[] prevSoldierState = new int[10000];
    static MapLocation[] ruinTarget = new MapLocation[10000];
    static Direction[] soldierLastDir = new Direction[10000];

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

        int id = rc.getID();

        if (soldierLastDir[id] == null)
            soldierLastDir[id] = Direction.NORTH;

        if (rc.getPaint() < 80 && soldierState[id] != STATE_REFUELING) {
            prevSoldierState[id] = soldierState[id];
            soldierState[id] = STATE_REFUELING;
        }

        /* ================= REFUEL ================= */

        if (soldierState[id] == STATE_REFUELING) {

            RobotInfo tower = Helper.nearestAllyTower(rc);

            if (tower != null) {

                MapLocation t = tower.getLocation();

                if (rc.getLocation().distanceSquaredTo(t) <= 2) {

                    if (rc.isActionReady()) {
                        int need = rc.getType().paintCapacity - rc.getPaint();

                        if (rc.canTransferPaint(t, -need))
                            rc.transferPaint(t, -need);
                    }

                } else if (rc.isMovementReady()) {
                    Helper.moveToward(rc, t);
                }

            }

            if (rc.getPaint() > rc.getType().paintCapacity * 0.8)
                soldierState[id] = prevSoldierState[id];

            return;
        }

        /* ================= FIND RUIN ================= */

        if (soldierState[id] == STATE_EXPLORING && ruinTarget[id] == null) {

            MapLocation ruin = Helper.nearestEmptyRuin(rc);

            if (ruin != null && rc.getPaint() > 120) {
                ruinTarget[id] = ruin;
                soldierState[id] = STATE_BUILDING_TOWER;
            }
        }

        /* ================= BUILD TOWER ================= */

        if (soldierState[id] == STATE_BUILDING_TOWER) {

            if (ruinTarget[id] == null) {
                soldierState[id] = STATE_EXPLORING;
                return;
            }

            MapLocation ruin = ruinTarget[id];

            if (rc.canSenseLocation(ruin)) {

                RobotInfo r = rc.senseRobotAtLocation(ruin);

                if (r != null && r.getType().isTowerType()) {
                    ruinTarget[id] = null;
                    soldierState[id] = STATE_EXPLORING;
                    return;
                }
            }

            if (rc.getLocation().distanceSquaredTo(ruin) > 8) {

                if (rc.isMovementReady())
                    Helper.moveToward(rc, ruin);

                return;
            }

            UnitType type = Helper.neededTowerType(rc);

            if (rc.canMarkTowerPattern(type, ruin))
                rc.markTowerPattern(type, ruin);

            /* COMPLETE tower FIRST */

            if (rc.canCompleteTowerPattern(type, ruin)) {

                rc.completeTowerPattern(type, ruin);

                ruinTarget[id] = null;
                soldierState[id] = STATE_EXPLORING;

                return;
            }

            /* FIX pattern */

            MapInfo[] tiles = rc.senseNearbyMapInfos(ruin, 8);

            MapInfo best = null;

            for (MapInfo t : tiles) {

                if (t.getMark() == PaintType.EMPTY)
                    continue;

                if (t.getMark() == t.getPaint())
                    continue;

                if (rc.canAttack(t.getMapLocation())) {
                    best = t;
                    break;
                }
            }

            if (best != null && rc.isActionReady()) {

                boolean sec = best.getMark() == PaintType.ALLY_SECONDARY;

                rc.attack(best.getMapLocation(), sec);
            }

            return;
        }

        /* ================= NORMAL PAINT ================= */

        if (rc.isActionReady()) {

            MapInfo[] tiles = rc.senseNearbyMapInfos(rc.getLocation(), 9);

            MapInfo best = null;
            int score = 0;

            for (MapInfo t : tiles) {

                if (!rc.canAttack(t.getMapLocation()))
                    continue;

                int s = 0;

                if (t.getPaint().isEnemy())
                    s = 10;
                else if (t.getPaint() == PaintType.EMPTY)
                    s = 5;

                if (s > score) {
                    score = s;
                    best = t;
                }
            }

            if (best != null)
                rc.attack(best.getMapLocation());
        }

        /* ================= MOVE ================= */

        if (rc.isMovementReady()) {

            Direction best = null;
            int bestScore = -9999;

            for (Direction d : directions) {

                if (!rc.canMove(d))
                    continue;

                MapLocation next = rc.getLocation().add(d);

                int s = 0;

                MapInfo[] around = rc.senseNearbyMapInfos(next, 4);

                for (MapInfo m : around) {

                    if (m.getPaint().isEnemy())
                        s += 3;
                    else if (m.getPaint() == PaintType.EMPTY)
                        s += 2;
                    else
                        s -= 1;
                }

                if (d == soldierLastDir[id])
                    s += 3;

                if (s > bestScore) {
                    bestScore = s;
                    best = d;
                }
            }

            if (best != null) {

                soldierLastDir[id] = best;
                rc.move(best);
            }
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