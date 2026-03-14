package alt_2;

import battlecode.common.GameActionException;
import battlecode.common.MapInfo;
import battlecode.common.MapLocation;
import battlecode.common.PaintType;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Splasher {
    // Memori Status
    static boolean isRefilling = false;

    public static void run(RobotController rc) throws GameActionException {
        rc.setIndicatorString("Splasher Vanguard!");

        // ═══════════════════════════════════════════════════════════
        // P0: STATE MACHINE REFILL
        // ═══════════════════════════════════════════════════════════
        if (rc.getPaint() <= 50) isRefilling = true;
        if (rc.getPaint() >= 250) isRefilling = false;

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
                isRefilling = false; // Tower kosong, batal antre
            }
        }

        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        MapInfo[] nearby = rc.senseNearbyMapInfos();

        // ═══════════════════════════════════════════════════════════
        // BRANCH A: BISA MENEMBAK (Action Ready)
        // ═══════════════════════════════════════════════════════════
        if (rc.isActionReady()) {
            
            // A1: Tower musuh HP rendah dalam range
            RobotInfo weakTower = null;
            for (RobotInfo e : enemies) {
                if (!e.getType().isTowerType() || e.getHealth() >= 500) continue;
                if (rc.getLocation().distanceSquaredTo(e.getLocation()) > 4) continue;
                if (!rc.canAttack(e.getLocation())) continue;
                if (weakTower == null || e.getHealth() < weakTower.getHealth()) weakTower = e;
            }
            if (weakTower != null) { rc.attack(weakTower.getLocation()); return; }

            // A2: GREEDY COVERAGE MAXIMIZER (Kalkulator Skor Area)
            MapLocation bestCenter = null; 
            int bestScore = 0; 
            
            for (MapInfo tile : nearby) {
                MapLocation center = tile.getMapLocation();
                if (rc.getLocation().distanceSquaredTo(center) > 4) continue;
                if (!rc.canAttack(center)) continue;
                
                int score = 0;
                for (MapInfo t2 : nearby) {
                    int distToCenter = center.distanceSquaredTo(t2.getMapLocation());
                    if (distToCenter <= 4) { 
                        PaintType pt = t2.getPaint();
                        if (distToCenter <= 2) { // Inner Blast
                            if (pt.isEnemy()) score += 3; 
                            else if (pt == PaintType.EMPTY) score += 1;
                        } else { // Outer Blast
                            if (pt == PaintType.EMPTY) score += 1;
                        }
                    }
                }
                
                if (score >= 4 && score > bestScore) {
                    bestScore = score; 
                    bestCenter = center; 
                }
            }
            
            if (bestCenter != null) { rc.attack(bestCenter); return; }

            // A3: Tower musuh biasa
            RobotInfo anyTower = Utils.weakestEnemyTower(enemies);
            if (anyTower != null && rc.getLocation().distanceSquaredTo(anyTower.getLocation()) <= 4 && rc.canAttack(anyTower.getLocation())) {
                rc.attack(anyTower.getLocation()); return;
            }
        }

        // ═══════════════════════════════════════════════════════════
        // BRANCH B: NAVIGASI & RADAR CERDAS
        // ═══════════════════════════════════════════════════════════
        MapLocation enemyTarget = Utils.getEnemyEstimate(rc, RobotPlayer.symmetryMode, RobotPlayer.spawnTowerLoc);
        
        // Radar Cerdas: Instant Switch
        if (rc.canSenseLocation(enemyTarget)) {
            RobotInfo botAtTarget = rc.senseRobotAtLocation(enemyTarget);
            if (botAtTarget == null || !botAtTarget.getType().isTowerType() || botAtTarget.getTeam() == rc.getTeam()) {
                RobotPlayer.symmetryMode = (RobotPlayer.symmetryMode + 1) % 3;
                RobotPlayer.roundsNearTarget = 0;
                enemyTarget = Utils.getEnemyEstimate(rc, RobotPlayer.symmetryMode, RobotPlayer.spawnTowerLoc);
            }
        }

        if (!rc.isActionReady() && rc.isMovementReady()) {
            // Mundur sedikit saat cooldown panjanng jika ada musuh
            if (enemies.length > 0) Utils.moveGreedy(rc, enemies[0].getLocation().directionTo(rc.getLocation()));
            else {
                if (rc.getLocation().distanceSquaredTo(enemyTarget) < 16) {
                    RobotPlayer.roundsNearTarget++;
                } else {
                    RobotPlayer.roundsNearTarget = 0;
                    Utils.moveGreedy(rc, rc.getLocation().directionTo(enemyTarget));
                }
            }
        } else if (rc.isMovementReady()) {
            if (rc.getLocation().distanceSquaredTo(enemyTarget) < 16) {
                RobotPlayer.roundsNearTarget++;
                if (RobotPlayer.roundsNearTarget > 100) { 
                    RobotPlayer.symmetryMode = (RobotPlayer.symmetryMode + 1) % 3; 
                    RobotPlayer.roundsNearTarget = 0; 
                }
            } else {
                RobotPlayer.roundsNearTarget = 0;
                Utils.moveGreedy(rc, rc.getLocation().directionTo(enemyTarget));
            }
        }
    }
}