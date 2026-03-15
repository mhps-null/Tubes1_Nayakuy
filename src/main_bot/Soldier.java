package main_bot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.PaintType;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;

public class Soldier {
    static boolean isRefilling = false;

    public static void run(RobotController rc) throws GameActionException {
        rc.setIndicatorString("Soldier Menyerang!");
        if (rc.getPaint() <= 40) isRefilling = true;
        if (rc.getPaint() >= 160) isRefilling = false;

        if (isRefilling) {
            RobotInfo paintTower = Utils.nearestAllyPaintTower(rc);
            if (paintTower != null && paintTower.getPaintAmount() >= 50) {
                if (rc.getLocation().distanceSquaredTo(paintTower.getLocation()) <= 2) {
                    if (rc.isActionReady()) {
                        int amountNeeded = rc.getType().paintCapacity - rc.getPaint();
                        int transferAmt = Math.min(amountNeeded, paintTower.getPaintAmount() - 10);
                        if (transferAmt > 0 && rc.canTransferPaint(paintTower.getLocation(), -transferAmt)) {
                            rc.transferPaint(paintTower.getLocation(), -transferAmt);
                        }
                    }
                } else {
                    if (rc.isMovementReady()) Utils.moveGreedy(rc, paintTower.getLocation());
                }
                return; 
            } else {
                isRefilling = false; 
            }
        }

        Message[] messages = rc.readMessages(-1);
        for (Message msg : messages) {
            int decoded = msg.getBytes();
            if (((decoded >> 12) & 0x3) == Constants.MSG_SYMMETRY) RobotPlayer.symmetryMode = (decoded >> 14) & 0x3;
        }

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        if (enemies.length > 0) {
            int confirmed = Utils.hitungSimetriDariObservasi(rc, enemies[0].getLocation(), RobotPlayer.spawnTowerLoc);
            RobotPlayer.symmetryMode = confirmed;
            RobotInfo tower = Utils.nearestAllyTower(rc);
            
            if (tower != null) {
                int symType = Constants.MSG_SYMMETRY << 12;
                int symData = confirmed << 14;
                int symMsg  = symData | symType;

                MapLocation enLoc = enemies[0].getLocation();
                int enemyType = Constants.MSG_ENEMY << 12;
                int enemyMsg  = (enLoc.y << 6) | enLoc.x | enemyType;

                if (rc.canSendMessage(tower.getLocation(), symMsg)) rc.sendMessage(tower.getLocation(), symMsg);
                if (rc.canSendMessage(tower.getLocation(), enemyMsg)) rc.sendMessage(tower.getLocation(), enemyMsg);
            }
        }

        RobotInfo targetTower = Utils.weakestEnemyTower(enemies);
        if (targetTower != null) {
            if (rc.isActionReady() && rc.canAttack(targetTower.getLocation())) { rc.attack(targetTower.getLocation()); return; }
            if (rc.isMovementReady()) { Utils.moveGreedy(rc, targetTower.getLocation()); return; }
        }

        if (enemies.length > 0 && rc.isActionReady()) {
            for (RobotInfo e : enemies) {
                if (e.getType().isTowerType()) continue; 
                for (Direction d : Constants.DIRECTIONS) {
                    MapLocation adj = e.getLocation().add(d);
                    if (!rc.canAttack(adj)) continue;
                    MapInfo info = rc.senseMapInfo(adj);
                    if (info.getPaint() == PaintType.EMPTY) { rc.attack(adj, false); return; }
                }
            }
        }

        MapLocation[] ruins = rc.senseNearbyRuins(-1);
        for (MapLocation ruin : ruins) {
            if (rc.senseMapInfo(ruin).getMark() == PaintType.EMPTY) continue;
            if (rc.getLocation().distanceSquaredTo(ruin) > 9) {
                if (rc.isMovementReady()) Utils.moveGreedy(rc, ruin);
                break;
            }
            MapInfo[] tiles = rc.senseNearbyMapInfos(ruin, 8);
            for (MapInfo tile : tiles) {
                if (tile.getMark() == PaintType.EMPTY || tile.getPaint() == tile.getMark()) continue;
                if (!rc.canAttack(tile.getMapLocation()) || !rc.isActionReady()) continue;
                rc.attack(tile.getMapLocation(), tile.getMark() == PaintType.ALLY_SECONDARY);
                break;
            }
            if (rc.getLocation().distanceSquaredTo(ruin) <= 2) {
                UnitType t = Utils.inferTowerType(rc, ruin);
                if (t != null) rc.completeTowerPattern(t, ruin);
            }
            break; 
        }

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

        if (rc.isActionReady()) {
            MapInfo cur = rc.senseMapInfo(rc.getLocation());
            if (cur.getPaint() == PaintType.EMPTY && rc.canAttack(rc.getLocation())) rc.attack(rc.getLocation(), false);
        }
    }
}