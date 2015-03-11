package wait4me;

import battlecode.common.*;
import wait4me.Common;
import wait4me.Memory;
import wait4me.Unit;

// import java.util.*;

/**
 * Common class for military units (soldiers, tanks,...)
 */
public class Military {

    static void process() throws GameActionException
    {
        boolean genericState = Unit.process();

        Military.setLabel();

        if (genericState) {
            return;
        }

        switch (Unit.getState()) {
            case UNDEFINED:
                defaultAction();
                break;
            case ATTACK:
                attack();
                break;
            default:
                break;
        }


        after();
    }

    static void after() throws GameActionException
    {
        if (Unit.isLeader()) {
            Memory.storeLocation(Common.Address.LEADER_POSITION,
                                 Unit.rc.getLocation());
        }
    }

    static void defaultAction() throws GameActionException
    {
        Unit.setState(Unit.State.SUPPLY);
    }

    static void setLabel()
    {
        String leader = (Unit.isLeader() ? "(leader)" : "(follower)");
        Unit.rc.setIndicatorString(1, leader);
    }

    static void attack() throws GameActionException
    {
        boolean regroup = (Common.turn % 3 == 0);
        if (Unit.isLeader() || !regroup) {
            MapLocation target = Memory.loadLocation(Common.Address.ATTACK_TARGET);
            Unit.moveBugging(target);
        } else {
            MapLocation leaderLoc = Memory.loadLocation(Common.Address.LEADER_POSITION);
            Unit.moveBugging(leaderLoc);

            /*
            MapLocation myLoc = Unit.rc.getLocation();
            MapLocation leaderLoc = Memory.loadLocation(Common.Address.LEADER_POSITION);

            Direction fromLeader = leaderLoc.directionTo(myLoc);
            MapLocation target = leaderLoc.add(fromLeader, 3);
            Direction toTarget = myLoc.directionTo(target);

            Unit.moveSimple(target);
            */
        }
    }

}
