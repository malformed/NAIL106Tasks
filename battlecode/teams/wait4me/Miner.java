package wait4me;

import battlecode.common.*;

import wait4me.Common;
import wait4me.Memory;
import wait4me.Unit;

import java.util.*;

/**
 * Miner logic class
 */
public class Miner {

    // static final int BEAVER_MINER_MASK = (1 << 16);
    static final int TURNS_WITHOUT_ORE_MASK = 0xff0000;

    static void process() throws GameActionException
    {
        boolean genericState = Unit.process();

        Miner.setLabel();

        if (genericState) {
            return;
        }

        switch (Unit.getState()) {
            case UNDEFINED:
                defaultAction();
                break;
            case MINE:
                mine();
                break;
            case SEARCH_ORE:
                searchOre();
                break;
            default:
                break;
        }

        // supply replenish
        replenishSupply();
    }

    static void defaultAction() throws GameActionException
    {
        Unit.setState(Unit.State.SUPPLY);
        /*
        if (isMiner()) {
            mine();
        } else {
            build();
        }
        */
    }

    static void replenishSupply() throws GameActionException
    {
        int supplyHQ = Memory.get(Common.Address.SUPPLY);

        if (Unit.rc.getSupplyLevel() < Strategy.SUPPLY_REPLENISH_TRESHOLD &&
                            supplyHQ > Strategy.MIN_HQ_SUPPLY_TO_REPLENISH) {
            Unit.setState(Unit.State.SUPPLY);
        }
    }

    static void mine() throws GameActionException
    {
        MapLocation loc = Unit.rc.getLocation();

        if (Unit.rc.senseOre(loc) > Strategy.MIN_ORE_TO_MINE) {
            Unit.rc.mine();
        } else {

            Integer[] offsets = new Integer[]{ -2, -1, 0, 1, 2};
            Collections.shuffle(Arrays.asList(offsets));

            MapLocation maxLocation = null;

            double maxOre = Unit.rc.senseOre(loc); // Double.MIN_VALUE;

            for (int dy : offsets) {
                for (int dx : offsets) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }
                    MapLocation l = new MapLocation(loc.x + dx, loc.y + dy);
                    double ore = Unit.rc.senseOre(l);
                    if (ore > maxOre) {
                        maxLocation = l;
                        maxOre = ore;
                    }
                }
            }

            if (maxLocation != null) {
                Unit.setDirection(loc.directionTo(maxLocation));
            } else {
                Unit.setDirection(Common.Helper.randomDirection());
            }

            Unit.setState(Unit.State.SEARCH_ORE);
        }
    }

    static void searchOre() throws GameActionException
    {
        MapLocation loc = Unit.rc.getLocation();

        if (Unit.rc.senseOre(loc) > Strategy.MIN_ORE_TO_MINE) {
            Unit.setState(Unit.State.MINE);
        } else {
            Direction dir = Unit.getDirection();
            Unit.tryMove(dir);

            int noOre = getTurnsWithoutOre();
            if (noOre > 20) {
                Unit.setDirection(Common.Helper.randomDirection());
                setTurnsWithoutOre(0);
            } else {
                setTurnsWithoutOre(noOre + 1);
            }
        }
    }

    static boolean isMiner()
    {
        return Unit.rc.getType() == RobotType.MINER;
    }

    static void setLabel()
    {
        String label1 = "worker type: " + (isMiner() ? "miner" : "beaver");
        Unit.rc.setIndicatorString(1, label1);

        String label2 = "turns withour ore: " + Integer.toString(getTurnsWithoutOre());
        Unit.rc.setIndicatorString(2, label2);
    }

    static int getTurnsWithoutOre()
    {
        return (Unit.data & TURNS_WITHOUT_ORE_MASK) >> 16;
    }

    static void setTurnsWithoutOre(int turns)
    {
        Unit.data = ((Unit.data & ~TURNS_WITHOUT_ORE_MASK) | (turns << 16));
    }
}
