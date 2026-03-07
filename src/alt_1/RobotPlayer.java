package alt_1;

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

    static final int S_RUSH_RUIN = 0;
    static final int S_REFUEL = 1;
    static final int S_WANDER = 2;

    static int soldierState = S_RUSH_RUIN;
    static int soldierPrevState = S_RUSH_RUIN;
    static MapLocation targetRuin = null;
    static int orbitIdx = 0;
    static Direction lastMoveDir = Direction.NORTH;

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
        // Greedy: habiskan musuh yang hampir mati
        if (rc.isActionReady()) {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            RobotInfo target = null;
            int lowestHP = Integer.MAX_VALUE;
            for (RobotInfo e : enemies) {
                if (rc.canAttack(e.getLocation()) && e.getHealth() < lowestHP) {
                    lowestHP = e.getHealth();
                    target = e;
                }
            }
            if (target != null)
                rc.attack(target.getLocation());
        }

        if (rc.isActionReady()) {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            MapLocation bestAoE = null;
            int bestCount = 0;
            for (RobotInfo e : enemies) {
                if (!rc.canAttack(e.getLocation()))
                    continue;
                int count = 0;
                for (RobotInfo o : enemies)
                    if (Helper.distSq(e.getLocation(), o.getLocation()) <= 4)
                        count++;
                if (count > bestCount) {
                    bestCount = count;
                    bestAoE = e.getLocation();
                }
            }
            if (bestAoE != null)
                rc.attack(bestAoE);
        }

        if (rc.isActionReady()) {
            int round = rc.getRoundNum();
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());

            int totalUnits = 0, moppers = 0;
            for (RobotInfo r : allies) {
                if (r.getType().isTowerType())
                    continue;
                totalUnits++;
                if (r.getType() == UnitType.MOPPER)
                    moppers++;
            }

            UnitType toSpawn;
            if (round <= 300) {
                toSpawn = (moppers * 3 < totalUnits) ? UnitType.MOPPER : UnitType.SOLDIER;
            } else {
                double mRatio = (double) moppers / Math.max(totalUnits, 1);
                toSpawn = (mRatio < 0.25) ? UnitType.MOPPER : UnitType.SOLDIER;
            }

            MapLocation bestSpawn = null;
            int bestScore = Integer.MIN_VALUE;
            for (Direction d : DIRS) {
                MapLocation c = rc.getLocation().add(d);
                if (!rc.canBuildRobot(toSpawn, c))
                    continue;
                int score = 0;
                try {
                    PaintType p = rc.senseMapInfo(c).getPaint();
                    if (p.isAlly())
                        score += 10;
                    else if (p == PaintType.EMPTY)
                        score += 5;
                    else if (p.isEnemy())
                        score -= 5;
                } catch (GameActionException ignored) {
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestSpawn = c;
                }
            }
            if (bestSpawn != null)
                rc.buildRobot(toSpawn, bestSpawn);
        }
    }

    static void runSoldier(RobotController rc) throws GameActionException {

        if (rc.getPaint() < 80 && soldierState != S_REFUEL) {
            soldierPrevState = soldierState;
            soldierState = S_REFUEL;
        }

        if (soldierState != S_REFUEL) {
            RobotInfo[] nearEnemies = rc.senseNearbyRobots(4, rc.getTeam().opponent());
            if (nearEnemies.length > 0 && rc.isMovementReady()) {
                Direction escape = findEscapeDir(rc, nearEnemies);
                if (escape != null) {
                    rc.move(escape);
                    return;
                }
            }
        }

        if (soldierState == S_REFUEL) {
            doRefuel(rc);
            return;
        }

        if (soldierState == S_RUSH_RUIN) {
            targetRuin = pickBestRuin(rc, targetRuin);

            if (targetRuin != null) {
                doBuildTower(rc);
            } else {
                soldierState = S_WANDER;
            }
            return;
        }

        if (soldierState == S_WANDER) {
            MapLocation ruin = findBestVisibleRuin(rc);
            if (ruin != null) {
                targetRuin = ruin;
                soldierState = S_RUSH_RUIN;
                orbitIdx = 0;
                return;
            }
            doWander(rc);
        }
    }

    // Greedy: ruin dengan skor tertinggi dipilih
    static MapLocation pickBestRuin(RobotController rc, MapLocation current)
            throws GameActionException {
        if (current != null) {
            if (rc.canSenseLocation(current)) {
                RobotInfo r = rc.senseRobotAtLocation(current);
                if (r != null && r.getType().isTowerType()) {
                    soldierState = S_RUSH_RUIN;
                    return null;
                }
            }
            return current;
        }
        return findBestVisibleRuin(rc);
    }

    static MapLocation findBestVisibleRuin(RobotController rc)
            throws GameActionException {
        MapLocation[] ruins = rc.senseNearbyRuins(-1);
        MapLocation best = null;
        int bestScore = Integer.MIN_VALUE;

        for (MapLocation ruin : ruins) {
            if (rc.canSenseLocation(ruin)) {
                RobotInfo r = rc.senseRobotAtLocation(ruin);
                if (r != null && r.getType().isTowerType())
                    continue;
            }
            boolean claimed = false;
            for (RobotInfo ally : rc.senseNearbyRobots(ruin, 3, rc.getTeam())) {
                if (!ally.getType().isTowerType()) {
                    claimed = true;
                    break;
                }
            }
            if (claimed)
                continue;

            int score = 1000 - rc.getLocation().distanceSquaredTo(ruin);
            if (score > bestScore) {
                bestScore = score;
                best = ruin;
            }
        }
        return best;
    }

    static void doBuildTower(RobotController rc) throws GameActionException {
        MapLocation ruin = targetRuin;
        if (ruin == null)
            return;

        UnitType towerType = chooseTowerType(rc);

        if (rc.canMarkTowerPattern(towerType, ruin))
            rc.markTowerPattern(towerType, ruin);

        if (rc.canCompleteTowerPattern(towerType, ruin)) {
            rc.completeTowerPattern(towerType, ruin);
            targetRuin = null;
            soldierState = S_RUSH_RUIN;
            orbitIdx = 0;
            return;
        }

        int distToRuin = rc.getLocation().distanceSquaredTo(ruin);
        if (distToRuin > 2) {
            doOpportunisticPaint(rc);
            if (rc.isMovementReady())
                Helper.moveToward(rc, ruin);
            return;
        }

        MapInfo[] tiles = rc.senseNearbyMapInfos(ruin, 8);

        if (rc.isActionReady()) {
            for (MapInfo t : tiles) {
                if (t.getMark() == PaintType.EMPTY)
                    continue;
                if (t.getMark() == t.getPaint())
                    continue;
                if (!rc.canAttack(t.getMapLocation()))
                    continue;
                boolean secondary = (t.getMark() == PaintType.ALLY_SECONDARY);
                rc.attack(t.getMapLocation(), secondary);
                break;
            }
        }

        if (rc.isMovementReady()) {
            for (int i = 0; i < 8; i++) {
                if (ruin.add(DIRS[i]).equals(rc.getLocation())) {
                    orbitIdx = i;
                    break;
                }
            }
            for (int step = 1; step <= 8; step++) {
                int nextIdx = (orbitIdx + step) % 8;
                MapLocation nextPos = ruin.add(DIRS[nextIdx]);
                Direction dir = rc.getLocation().directionTo(nextPos);
                if (rc.canMove(dir)) {
                    rc.move(dir);
                    orbitIdx = nextIdx;
                    break;
                }
            }
        }
    }

    static UnitType chooseTowerType(RobotController rc) throws GameActionException {
        int paintTowers = 0, moneyTowers = 0;
        for (RobotInfo ally : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (ally.getType() == UnitType.LEVEL_ONE_PAINT_TOWER
                    || ally.getType() == UnitType.LEVEL_TWO_PAINT_TOWER
                    || ally.getType() == UnitType.LEVEL_THREE_PAINT_TOWER)
                paintTowers++;
            if (ally.getType() == UnitType.LEVEL_ONE_MONEY_TOWER
                    || ally.getType() == UnitType.LEVEL_TWO_MONEY_TOWER
                    || ally.getType() == UnitType.LEVEL_THREE_MONEY_TOWER)
                moneyTowers++;
        }
        if (paintTowers > moneyTowers + 1)
            return UnitType.LEVEL_ONE_MONEY_TOWER;
        return UnitType.LEVEL_ONE_PAINT_TOWER;
    }

    static void doRefuel(RobotController rc) throws GameActionException {
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
        if (rc.getPaint() >= rc.getType().paintCapacity * 0.8) {
            soldierState = soldierPrevState;
        }
    }

    // Greedy: pilih arah dengan tile empty terbanyak dalam radius
    static void doWander(RobotController rc) throws GameActionException {
        doOpportunisticPaint(rc);

        if (!rc.isMovementReady())
            return;

        Direction best = null;
        int bestScore = Integer.MIN_VALUE;

        for (Direction d : DIRS) {
            if (!rc.canMove(d))
                continue;
            MapLocation next = rc.getLocation().add(d);
            int score = 0;

            for (MapInfo m : rc.senseNearbyMapInfos(next, 4)) {
                if (m.getPaint() == PaintType.EMPTY)
                    score += 2;
                else if (m.getPaint().isEnemy())
                    score += 3;
                else if (m.getPaint().isAlly())
                    score -= 1;
            }
            score -= rc.senseNearbyRobots(next, 9, rc.getTeam()).length * 4;
            if (d == lastMoveDir)
                score += 3;

            if (score > bestScore) {
                bestScore = score;
                best = d;
            }
        }

        if (best != null) {
            lastMoveDir = best;
            rc.move(best);
        }
    }

    static void doOpportunisticPaint(RobotController rc) throws GameActionException {
        if (!rc.isActionReady())
            return;
        MapInfo best = null;
        int bestScore = 0;
        for (MapInfo t : rc.senseNearbyMapInfos(rc.getLocation(), 9)) {
            if (!rc.canAttack(t.getMapLocation()))
                continue;
            int s = 0;
            if (t.getPaint().isEnemy())
                s = 10;
            else if (t.getPaint() == PaintType.EMPTY)
                s = 5;
            if (s > bestScore) {
                bestScore = s;
                best = t;
            }
        }
        if (best != null)
            rc.attack(best.getMapLocation());
    }

    // Greedy: pilih arah yang memaksimalkan total jarak dari musuh
    static Direction findEscapeDir(RobotController rc, RobotInfo[] enemies)
            throws GameActionException {
        Direction best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Direction d : DIRS) {
            if (!rc.canMove(d))
                continue;
            MapLocation next = rc.getLocation().add(d);
            int score = 0;
            for (RobotInfo e : enemies)
                score += Helper.distSq(next, e.getLocation());
            if (score > bestScore) {
                bestScore = score;
                best = d;
            }
        }
        return best;
    }

    static void runMopper(RobotController rc) throws GameActionException {

        if (rc.getPaint() < 40) {
            RobotInfo tower = Helper.nearestAllyTower(rc);
            if (tower != null) {
                MapLocation tLoc = tower.getLocation();
                if (rc.getLocation().distanceSquaredTo(tLoc) <= 2) {
                    if (rc.isActionReady()) {
                        int need = rc.getType().paintCapacity - rc.getPaint();
                        if (rc.canTransferPaint(tLoc, -need))
                            rc.transferPaint(tLoc, -need);
                    }
                } else if (rc.isMovementReady()) {
                    Helper.moveToward(rc, tLoc);
                }
            }
            return;
        }

        // Greedy: ally dengan paint paling sedikit mendapat transfer
        if (rc.isActionReady()) {
            RobotInfo needy = Helper.mostNeedyAlly(rc, 2);
            if (needy != null && needy.getPaintAmount() < 80) {
                int give = Math.min(
                        rc.getPaint() - 40,
                        needy.getType().paintCapacity - needy.getPaintAmount());
                if (give > 0 && rc.canTransferPaint(needy.getLocation(), give))
                    rc.transferPaint(needy.getLocation(), give);
            }
        }

        if (rc.isActionReady()) {
            RobotInfo bestEnemy = null;
            int lowestPaint = Integer.MAX_VALUE;
            for (RobotInfo e : rc.senseNearbyRobots(2, rc.getTeam().opponent())) {
                if (rc.canAttack(e.getLocation()) && e.getPaintAmount() < lowestPaint) {
                    lowestPaint = e.getPaintAmount();
                    bestEnemy = e;
                }
            }
            if (bestEnemy != null)
                rc.attack(bestEnemy.getLocation());
        }

        if (rc.isActionReady()) {
            Direction bestSwing = null;
            int bestCount = 1;
            for (Direction d : new Direction[] {
                    Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST }) {
                MapLocation s1 = rc.getLocation().add(d);
                MapLocation s2 = s1.add(d);
                int cnt = rc.senseNearbyRobots(s1, 1, rc.getTeam().opponent()).length
                        + rc.senseNearbyRobots(s2, 1, rc.getTeam().opponent()).length;
                if (cnt > bestCount) {
                    bestCount = cnt;
                    bestSwing = d;
                }
            }
            if (bestSwing != null && rc.canMopSwing(bestSwing))
                rc.mopSwing(bestSwing);
        }

        if (rc.isActionReady()) {
            MapLocation bestTile = null;
            int bestScore = 0;
            for (MapInfo tile : rc.senseNearbyMapInfos()) {
                if (!tile.getPaint().isEnemy())
                    continue;
                if (!rc.canAttack(tile.getMapLocation()))
                    continue;
                int adj = 0;
                for (MapInfo t : rc.senseNearbyMapInfos(tile.getMapLocation(), 2))
                    if (t.getPaint().isEnemy())
                        adj++;
                if (adj > bestScore) {
                    bestScore = adj;
                    bestTile = tile.getMapLocation();
                }
            }
            if (bestTile != null)
                rc.attack(bestTile);
        }

        if (rc.isMovementReady()) {
            RobotInfo needy = Helper.mostNeedyAlly(rc, 20);
            if (needy != null && needy.getPaintAmount() < 80
                    && Helper.distSq(rc.getLocation(), needy.getLocation()) > 2) {
                Helper.moveToward(rc, needy.getLocation());
                return;
            }
            MapLocation ep = Helper.nearestEnemyPaint(rc);
            if (ep != null) {
                Helper.moveToward(rc, ep);
                return;
            }
            RobotInfo nearSoldier = null;
            int bestDist = Integer.MAX_VALUE;
            for (RobotInfo r : rc.senseNearbyRobots(-1, rc.getTeam())) {
                if (r.getType() != UnitType.SOLDIER)
                    continue;
                int d = Helper.distSq(rc.getLocation(), r.getLocation());
                if (d < bestDist) {
                    bestDist = d;
                    nearSoldier = r;
                }
            }
            if (nearSoldier != null) {
                if (bestDist < 4)
                    Helper.moveAway(rc, nearSoldier.getLocation());
                else
                    Helper.moveToward(rc, nearSoldier.getLocation());
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
                MapLocation tLoc = tower.getLocation();
                if (rc.getLocation().distanceSquaredTo(tLoc) <= 2) {
                    if (rc.isActionReady()) {
                        int need = rc.getType().paintCapacity - rc.getPaint();
                        if (rc.canTransferPaint(tLoc, -need))
                            rc.transferPaint(tLoc, -need);
                    }
                } else if (rc.isMovementReady()) {
                    Helper.moveToward(rc, tLoc);
                }
            } else if (rc.isMovementReady()) {
                Direction best = null;
                int bestAlly = -1;
                for (Direction d : DIRS) {
                    if (!rc.canMove(d))
                        continue;
                    int cnt = 0;
                    for (MapInfo t : rc.senseNearbyMapInfos(rc.getLocation().add(d), 4))
                        if (t.getPaint().isAlly())
                            cnt++;
                    if (cnt > bestAlly) {
                        bestAlly = cnt;
                        best = d;
                    }
                }
                if (best != null)
                    rc.move(best);
            }
            return;
        }

        // Greedy: enemy_tile * 3 + empty_tile * 2 dalam radius 2 dari pusat splash
        if (rc.isActionReady()) {
            MapLocation bestCenter = null;
            int bestScore = 8;
            for (MapInfo cand : rc.senseNearbyMapInfos(rc.getLocation(), 4)) {
                MapLocation center = cand.getMapLocation();
                if (!rc.canAttack(center))
                    continue;
                int score = 0;
                for (MapInfo t : rc.senseNearbyMapInfos(center, 2)) {
                    if (t.getPaint().isEnemy())
                        score += 3;
                    else if (t.getPaint() == PaintType.EMPTY)
                        score += 2;
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestCenter = center;
                }
            }
            if (bestCenter != null) {
                rc.attack(bestCenter);
                return;
            }
        }

        if (rc.isMovementReady() && Helper.countAdjacentAllies(rc) > 2) {
            Direction bestEsc = null;
            int leastAllies = Integer.MAX_VALUE;
            for (Direction d : DIRS) {
                if (!rc.canMove(d))
                    continue;
                int cnt = rc.senseNearbyRobots(
                        rc.getLocation().add(d), 2, rc.getTeam()).length;
                if (cnt < leastAllies) {
                    leastAllies = cnt;
                    bestEsc = d;
                }
            }
            if (bestEsc != null) {
                rc.move(bestEsc);
                return;
            }
        }

        if (rc.isMovementReady()) {
            Direction best = null;
            int bestScore = Integer.MIN_VALUE;
            for (Direction d : DIRS) {
                if (!rc.canMove(d))
                    continue;
                MapLocation next = rc.getLocation().add(d);
                int score = 0;
                for (MapInfo t : rc.senseNearbyMapInfos(next, 4)) {
                    if (t.getPaint().isEnemy())
                        score += 3;
                    else if (t.getPaint() == PaintType.EMPTY)
                        score += 2;
                }
                score -= rc.senseNearbyRobots(next, 2, rc.getTeam()).length * 5;
                if (d == splasherLastDir
                        || d == splasherLastDir.rotateLeft()
                        || d == splasherLastDir.rotateRight())
                    score += 3;
                if (d == splasherLastDir.opposite())
                    score -= 5;
                if (score > bestScore) {
                    bestScore = score;
                    best = d;
                }
            }
            if (best != null) {
                splasherLastDir = best;
                rc.move(best);
            }
        }
    }
}