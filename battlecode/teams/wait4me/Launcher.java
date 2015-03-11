package wait4me;

import battlecode.common.*;
import wait4me.Common;
import wait4me.Memory;
import wait4me.Military;
import wait4me.Unit;

// import java.util.*;

/**
 * Launcher unit special class
 */
public class Launcher {

    static void process() throws GameActionException
    {
        launcherAttack();

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
                attackMove();
                break;

            default:
                break;
        }
    }

    static void launcherAttack() throws GameActionException
    {
        if (Unit.rc.getMissileCount() > 0) {
            int targetSeen = Memory.get(Common.Address.MISSILE_TARGET_TIME);
            MapLocation target = Memory.loadLocation(Common.Address.MISSILE_TARGET);

            MapLocation loc = Unit.rc.getLocation();
            int dist = loc.distanceSquaredTo(target);
            if (targetSeen + 5 > Common.turn && dist > 15 && dist < 50) {
                Direction dir = loc.directionTo(target);
                if (Unit.rc.canLaunch(dir)) {
                    Unit.rc.launchMissile(dir);
                }
            }
        }
    }

    static void attackMove() throws GameActionException
    {
        Military.attack();
    }

    static void defaultAction() throws GameActionException
    {
        Unit.setState(Unit.State.SUPPLY);
    }

    static void setLabel()
    {
        String type = Unit.rc.getType().toString();
        String missiles = "missiles: " + Integer.toString(Unit.rc.getMissileCount());

        Unit.rc.setIndicatorString(1, type + " | " + missiles);
    }


}
