package alt_2;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.PaintType;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;

public class Mopper {

    public static void run(RobotController rc) throws GameActionException {
        rc.setIndicatorString("Mopper Vanguard (Pantang Pulang!)");

        RobotInfo[] allAllies = rc.senseNearbyRobots(-1, rc.getTeam());

        for (RobotInfo a : rc.senseNearbyRobots(2, rc.getTeam())) {
            if (a.getType().isTowerType()) continue;
            if (a.getPaintAmount() < 10 && rc.isActionReady() && rc.getPaint() >= 50) {
                if (rc.canTransferPaint(a.getLocation(), 40)) { 
                    rc.transferPaint(a.getLocation(), 40); 
                    return; 
                }
            }
        }

        RobotInfo[] combatEnemies = rc.senseNearbyRobots(2, rc.getTeam().opponent());
        if (combatEnemies.length > 0 && rc.isActionReady()) {
            Direction bestSwing = null; int bestCount = 0;
            for (Direction d : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                if (!rc.canMopSwing(d)) continue;
                int count = 0; MapLocation s1 = rc.getLocation().add(d); MapLocation s2 = s1.add(d);
                for (RobotInfo e : combatEnemies) {
                    if (e.getLocation().distanceSquaredTo(s1) <= 2 || e.getLocation().distanceSquaredTo(s2) <= 2) count++;
                }
                if (count > bestCount) { bestCount = count; bestSwing = d; }
            }
            if (bestSwing != null && bestCount >= 2) { 
                rc.mopSwing(bestSwing); 
                return; 
            }
            
            RobotInfo target = combatEnemies[0];
            for (RobotInfo e : combatEnemies) {
                if (e.getPaintAmount() < target.getPaintAmount()) target = e;
            }
            if (rc.canAttack(target.getLocation())) { 
                rc.attack(target.getLocation()); 
                return; 
            }
        }

        MapInfo[] nearbyTiles = rc.senseNearbyMapInfos();
        MapLocation enemyPaintTile = null; int bestDist = Integer.MAX_VALUE;
        for (MapInfo tile : nearbyTiles) {
            if (!tile.getPaint().isEnemy()) continue;
            int d = rc.getLocation().distanceSquaredTo(tile.getMapLocation());
            if (d < bestDist) { bestDist = d; enemyPaintTile = tile.getMapLocation(); }
        }
        if (enemyPaintTile != null) {
            if (rc.isMovementReady() && bestDist > 2) Utils.moveGreedy(rc, enemyPaintTile);
            if (rc.isActionReady() && rc.canAttack(enemyPaintTile)) { 
                rc.attack(enemyPaintTile); 
                return; 
            }
        }

        MapLocation[] ruins = rc.senseNearbyRuins(-1);
        for (MapLocation ruin : ruins) {
            if (rc.senseRobotAtLocation(ruin) != null) continue;
            if (rc.senseMapInfo(ruin).getMark() != PaintType.EMPTY) continue;
            if (rc.getLocation().distanceSquaredTo(ruin) > 2) {
                if (rc.isMovementReady()) Utils.moveGreedy(rc, ruin);
                return;
            }
            if (rc.canMark(ruin)) rc.mark(ruin, false);
            UnitType tt = Utils.chooseTowerType(rc);
            if (rc.canMarkTowerPattern(tt, ruin)) rc.markTowerPattern(tt, ruin);
            UnitType done = Utils.inferTowerType(rc, ruin);
            if (done != null) rc.completeTowerPattern(done, ruin);
            return;
        }

        RobotInfo nearestSoldier = null; int minDist = Integer.MAX_VALUE;
        for (RobotInfo r : allAllies) {
            if (r.getType() != UnitType.SOLDIER) continue;
            int d = rc.getLocation().distanceSquaredTo(r.getLocation());
            if (d < minDist) { minDist = d; nearestSoldier = r; }
        }
        
        if (nearestSoldier != null && rc.isMovementReady()) {
            Utils.moveGreedy(rc, nearestSoldier.getLocation());
        } else {
            MapLocation enemyTarget = Utils.getEnemyEstimate(rc, RobotPlayer.symmetryMode, RobotPlayer.spawnTowerLoc);
            
            if (rc.canSenseLocation(enemyTarget)) {
                RobotInfo botAtTarget = rc.senseRobotAtLocation(enemyTarget);
                if (botAtTarget == null || !botAtTarget.getType().isTowerType() || botAtTarget.getTeam() == rc.getTeam()) {
                    RobotPlayer.symmetryMode = (RobotPlayer.symmetryMode + 1) % 3;
                    RobotPlayer.roundsNearTarget = 0;
                    enemyTarget = Utils.getEnemyEstimate(rc, RobotPlayer.symmetryMode, RobotPlayer.spawnTowerLoc);
                }
            }

            if (rc.getLocation().distanceSquaredTo(enemyTarget) < 16) {
                RobotPlayer.roundsNearTarget++;
                if (RobotPlayer.roundsNearTarget > 100) { 
                    RobotPlayer.symmetryMode = (RobotPlayer.symmetryMode + 1) % 3; 
                    RobotPlayer.roundsNearTarget = 0; 
                }
            } else {
                RobotPlayer.roundsNearTarget = 0;
                if (rc.isMovementReady()) {
                    Utils.moveGreedy(rc, rc.getLocation().directionTo(enemyTarget));
                }
            }
        }
    }
}