package alt3;

import battlecode.common.*;
import alt3.robots.*;

public class RobotPlayer {

    public static void run(RobotController rc) throws GameActionException {

        while (true) {

            try {

                switch (rc.getType()) {

                    case SOLDIER:
                        SoldierBot.run(rc);
                        break;

                    case MOPPER:
                        MopperBot.run(rc);
                        break;

                    case SPLASHER:
                        SplasherBot.run(rc);
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

        Direction[] dirs = Direction.values();

        for (Direction d : dirs) {

            MapLocation loc = rc.getLocation().add(d);

            if (rc.canBuildRobot(UnitType.SOLDIER, loc)) {
                rc.buildRobot(UnitType.SOLDIER, loc);
                return;
            }

            if (rc.canBuildRobot(UnitType.MOPPER, loc)) {
                rc.buildRobot(UnitType.MOPPER, loc);
                return;
            }

            if (rc.canBuildRobot(UnitType.SPLASHER, loc)) {
                rc.buildRobot(UnitType.SPLASHER, loc);
                return;
            }
        }
    }
}