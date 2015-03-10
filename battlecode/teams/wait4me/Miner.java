package wait4me;

import battlecode.common.*;

import wait4me.Common;
import wait4me.Memory;
import wait4me.RobotPlayer;
import wait4me.Unit;

import java.util.*;

/**
 * Miner logic class
 */
public class Miner {

    static final int BEAVER_MINER_MASK = (1 << 16);

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
            case BUILD:
                build();
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

    static void build() throws GameActionException
    {
        int fate = Common.rand.nextInt(1000);
        int numRax = Memory.get(Common.Address.NUM_BARRACKS);
        int numFactories = Memory.get(Common.Address.NUM_FACTORIES);
		int numMiners = Memory.get(Common.Address.NUM_MINERS);
        int numMinerFactories = Memory.get(Common.Address.NUM_MINERFACTORIES);
        int numDepots = Memory.get(Common.Address.NUM_SUPPLY_DEPOTS);

        double ore = Unit.rc.getTeamOre();

        if (numMinerFactories < 1 && ore >= Common.Costs.MINER_FACTORY)
        {
            RobotPlayer.tryBuild(Unit.dirFromInt(Common.rand.nextInt(8)), RobotType.MINERFACTORY);
        }
        else if (numMinerFactories > 0 && numRax < 1 && ore >= Common.Costs.BARRACKS)
        {
            RobotPlayer.tryBuild(Unit.dirFromInt(Common.rand.nextInt(8)), RobotType.BARRACKS);
        }
        else if (numFactories > 1 && numRax < 4 && ore >= Common.Costs.BARRACKS)
        {
            // RobotPlayer.tryBuild(Unit.dirFromInt(Common.rand.nextInt(8)), RobotType.BARRACKS);
        }
        else if (numRax > 0 && numFactories < 1 && ore > Common.Costs.FACTORY)
        {
            RobotPlayer.tryBuild(Unit.dirFromInt(Common.rand.nextInt(8)), RobotType.TANKFACTORY);
        }
        else if (numRax > 0 && numFactories < 2 && ore > Common.Costs.FACTORY + Common.Costs.TANK)
        {
            RobotPlayer.tryBuild(Unit.dirFromInt(Common.rand.nextInt(8)), RobotType.TANKFACTORY);
        }
        else if (numMiners >= 20  && numDepots < 5 && ore >= (Common.Costs.SUPPLY_DEPOT + Common.Costs.TANK))
        {
            RobotPlayer.tryBuild(Unit.dirFromInt(Common.rand.nextInt(8)), RobotType.SUPPLYDEPOT);
        } else if (fate < 600) {
            Unit.rc.mine();
        } else if (fate < 900) {
            Unit.tryMove(Unit.dirFromInt(Common.rand.nextInt(8)));
        } else {
            Unit.tryMove(Unit.rc.senseHQLocation().directionTo(Unit.rc.getLocation()));
        }
    }

    static void mine() throws GameActionException
    {
        MapLocation loc = Unit.rc.getLocation();

        if (Unit.rc.senseOre(loc) > 0.1) {
            Unit.rc.mine();
        } else {

            int[] offsets = { -2, -1, 0, 1, 2};
            Collections.shuffle(Arrays.asList(offsets));

            MapLocation maxLocation = null;

            double maxOre = Double.MIN_VALUE;

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
                Unit.tryMove(loc.directionTo(maxLocation));
            } else {
                // int rnd = Common.rand.nextInt(8);
                // Unit.tryMove(Unit.dirFromInt(rnd));
                Unit.tryMove(Common.hqLocation.directionTo(loc));
            }

            /*
            int fate = Common.rand.nextInt(10);

            if (fate < 7) {
                int rnd = Common.rand.nextInt(8);
                Unit.tryMove(Unit.dirFromInt(rnd));
            } else {
                Unit.tryMove(Unit.rc.senseHQLocation().directionTo(loc));
            }
            */
        }
    }

    static boolean isMiner()
    {
        return Unit.rc.getType() == RobotType.MINER;
    }

    static void setLabel()
    {
        String label = "worker type: " + (isMiner() ? "miner" : "beaver");
        Unit.rc.setIndicatorString(1, label);
    }
}
