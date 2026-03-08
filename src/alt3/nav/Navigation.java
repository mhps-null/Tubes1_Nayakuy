package alt3.nav;

import battlecode.common.*;

import java.util.Random;

public class Navigation {

    static Random rng = new Random();

    public static void moveToward(RobotController rc, MapLocation target) throws GameActionException {

        Direction dir = rc.getLocation().directionTo(target);

        if (rc.canMove(dir)) {
            rc.move(dir);
            return;
        }

        Direction left = dir.rotateLeft();
        Direction right = dir.rotateRight();

        if (rc.canMove(left)) {
            rc.move(left);
            return;
        }

        if (rc.canMove(right)) {
            rc.move(right);
        }
    }

    public static void randomMove(RobotController rc) throws GameActionException {

        Direction[] dirs = Direction.values();

        for (Direction d : dirs) {

            if (rc.canMove(d)) {
                rc.move(d);
                return;
            }
        }
    }
}