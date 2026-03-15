package main_bot;

import battlecode.common.*;

public class RobotPlayer {
    static int turnCount = 0;
    static int spawnCount = 0;
    static int symmetryMode = 0;
    static int roundsNearTarget = 0;

    static MapLocation spawnTowerLoc = null;

    public static void run(RobotController rc) throws GameActionException {

        if (rc.getType().isTowerType()) {
            spawnTowerLoc = rc.getLocation();
        } else {
            RobotInfo initTower = Utils.nearestAllyTower(rc);
            if (initTower != null) {
                spawnTowerLoc = initTower.getLocation();
            } else {
                spawnTowerLoc = rc.getLocation();
            }
        }

        while (true) {
            turnCount += 1;

            try {
                switch (rc.getType()) {
                    case SOLDIER:
                        Soldier.run(rc);
                        break;
                    case MOPPER:
                        Mopper.run(rc);
                        break;
                    case SPLASHER:
                        Splasher.run(rc);
                        break;
                    default:
                        Tower.run(rc);
                        break;
                }
            } catch (GameActionException e) {
                System.out.println("GameActionException in turn " + turnCount);
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception in turn " + turnCount);
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }
}