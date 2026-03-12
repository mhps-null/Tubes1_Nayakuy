package alt3.nav;

import battlecode.common.*;

import java.util.Random;

public class Navigation {

    static Random rng = new Random();

    public static void moveToward(RobotController rc, MapLocation target) throws GameActionException {

        Direction dir = rc.getLocation().directionTo(target);

        Direction[] tryDirs = new Direction[] {
                dir,
                dir.rotateLeft(),
                dir.rotateRight(),
                dir.rotateLeft().rotateLeft(),
                dir.rotateRight().rotateRight(),
                dir.rotateLeft().rotateLeft().rotateLeft(),
                dir.rotateRight().rotateRight().rotateRight()
        };

        for (Direction d : tryDirs) {

            if (rc.canMove(d)) {
                rc.move(d);
                return;
            }
        }

        randomMove(rc);
    }

    public static void randomMove(RobotController rc) throws GameActionException {

        Direction[] dirs = Direction.values();

        int start = rng.nextInt(dirs.length);

        for (int i = 0; i < dirs.length; i++) {

            Direction d = dirs[(start + i) % dirs.length];

            if (rc.canMove(d)) {
                rc.move(d);
                return;
            }
        }
    }
}