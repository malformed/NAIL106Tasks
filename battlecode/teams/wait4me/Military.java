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
            default:
                break;
        }
    }

    static void defaultAction() throws GameActionException
    {
        // Unit.setSupplyRequest(true);
        Unit.setState(Unit.State.SUPPLY);
        /*
        int fate = Common.rand.nextInt(1000);
        if (fate < 800) {
            Unit.tryMove(Unit.dirFromInt(Common.rand.nextInt(8)));
        } else {
            Unit.tryMove(Unit.rc.getLocation().directionTo(Unit.rc.senseEnemyHQLocation()));
        }
        */
    }
            

    static void setLabel()
    {
        Unit.rc.setIndicatorString(1, "Military unit: " + Unit.rc.getType().toString());
    }


}
