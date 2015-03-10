package wait4me;

import java.util.*;

import battlecode.common.*;

public class Common {

    static Random rand;

    static MapLocation hqLocation;
    static MapLocation enemyLocation;

    static class Costs
    {
        // units
        static final int BEAVER = 100;
        static final int MINER = 60;
        static final int SOLDIER = 60;
        static final int TANK = 250;

        // buildings
        static final int BARRACKS = 300;
        static final int FACTORY = 500;
        static final int MINER_FACTORY = 500;
        static final int SUPPLY_DEPOT = 100;
    }

    static class Address
    {
        static final int NUM_BEAVERS = 1;
        static final int NUM_MINERS = 2;
        static final int NUM_SOLDIERS = 3;
        static final int NUM_BASHERS = 4;
        static final int NUM_TANKS = 5;

        static final int NUM_BARRACKS = 10;
        static final int NUM_FACTORIES = 11;
        static final int NUM_MINERFACTORIES = 12;
        static final int NUM_SUPPLY_DEPOTS = 13;

        static final int SUPPLY = 20;

        static final int MOVE_TARGET = 40;   // <x,y> ... 40,41
        static final int ATTACK_TARGET = 42; // <x,y> ... 42,43
    }

    static class Constants
    {
        static final int TRANSFER_RADIUS = 15;
    }

    static class Helper
    {
        static boolean isWorker(RobotInfo robotInfo)
        {
            RobotType type = robotInfo.type;
            return type == RobotType.BEAVER || type == RobotType.MINER;
        }
    }
}


