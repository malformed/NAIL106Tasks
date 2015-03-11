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
    }

    static void defaultAction() throws GameActionException
    {
        Unit.setState(Unit.State.SUPPLY);
    }

    static void setLabel()
    {
        Unit.rc.setIndicatorString(1, "Military unit: " + Unit.rc.getType().toString());
    }

    static void attack() throws GameActionException
    {
        MapLocation target = Memory.loadLocation(Common.Address.ATTACK_TARGET);

        Unit.moveBugging(target);
    }

}
