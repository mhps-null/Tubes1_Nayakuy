package alt_2;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.UnitType;

public class Tower {

    public static void run(RobotController rc) throws GameActionException {
        rc.setIndicatorString("Tower WEF | Spawn Count: " + RobotPlayer.spawnCount);

        RobotInfo[] enemies = rc.senseNearbyRobots(9, rc.getTeam().opponent());
        if (enemies.length > 0) {
            RobotInfo target = enemies[0];
            for (RobotInfo e : enemies) if (e.getHealth() < target.getHealth()) target = e;
            if (rc.canAttack(target.getLocation())) rc.attack(target.getLocation());
        }

        Message[] messages = rc.readMessages(-1);
        for (Message msg : messages) {
            int decoded = msg.getBytes();
            int type = (decoded >> 12) & 0x3;
            if (type == Constants.MSG_ENEMY || type == Constants.MSG_SYMMETRY) {
                if (rc.canBroadcastMessage()) rc.broadcastMessage(decoded);
                for (RobotInfo r : rc.senseNearbyRobots(20, rc.getTeam())) {
                    if (!r.getType().isTowerType() && rc.canSendMessage(r.getLocation(), decoded)) {
                        rc.sendMessage(r.getLocation(), decoded);
                    }
                }
            }
        }

        // FASE SELF-UPGRADE
        if (rc.isActionReady()) {
            if (rc.getChips() > 3000) { 
                if (rc.canUpgradeTower(rc.getLocation())) {
                    rc.upgradeTower(rc.getLocation());
                    rc.setIndicatorString("UPGRADING TOWER!");
                    return; 
                }
            }
        }

        if (!rc.isActionReady()) return;

        MapLocation myLoc = rc.getLocation();
        MapLocation enemyBaseEstimate = Utils.getEnemyEstimate(rc, RobotPlayer.symmetryMode, myLoc);
        boolean isMoneyTower = (rc.getType() == UnitType.LEVEL_ONE_MONEY_TOWER || rc.getType() == UnitType.LEVEL_TWO_MONEY_TOWER || rc.getType() == UnitType.LEVEL_THREE_MONEY_TOWER);

        if (isMoneyTower) {
            if (rc.getPaint() >= 200 && rc.getChips() >= 250) {
                MapLocation spawnLoc = Utils.tileToward(rc, enemyBaseEstimate, UnitType.SOLDIER);
                if (spawnLoc != null && rc.canBuildRobot(UnitType.SOLDIER, spawnLoc)) rc.buildRobot(UnitType.SOLDIER, spawnLoc);
            }
            return; 
        }

        if (enemies.length > 0) {
            MapLocation spawnLoc = Utils.tileToward(rc, enemyBaseEstimate, UnitType.MOPPER);
            if (spawnLoc != null && rc.getChips() >= 300 && rc.getPaint() >= 100) {
                if (rc.canBuildRobot(UnitType.MOPPER, spawnLoc)) {
                    rc.buildRobot(UnitType.MOPPER, spawnLoc);
                    return; 
                }
            }
        }

        int slot = RobotPlayer.spawnCount % 4;
        UnitType toSpawn;
        
        if (slot == 0 || slot == 1) toSpawn = UnitType.SOLDIER; // Double Soldier!
        else if (slot == 2) toSpawn = UnitType.MOPPER;
        else toSpawn = (rc.getChips() >= 500) ? UnitType.SPLASHER : UnitType.MOPPER;

        MapLocation spawnLoc = Utils.tileToward(rc, enemyBaseEstimate, toSpawn);
        if (spawnLoc != null && rc.canBuildRobot(toSpawn, spawnLoc)) {
            rc.buildRobot(toSpawn, spawnLoc);
            RobotPlayer.spawnCount++; 
            
            // OPTIONAL: Kirim pesan singkat ke robot yang baru lahir untuk segera menjauh
            // (Ini membantu mencegah penumpukan di mulut tower)
        }
    }
}