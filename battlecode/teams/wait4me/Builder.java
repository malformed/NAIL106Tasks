package wait4me;

import battlecode.common.*;

import wait4me.Common;
import wait4me.Memory;
import wait4me.RobotPlayer;
import wait4me.Unit;

/**
 * Miner logic class
 */
public class Builder {

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
            case BUILD:
                build();
                break;
            default:
                break;
        }
    }

    static void defaultAction()
    {
        Unit.setState(Unit.State.SUPPLY);
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


}
