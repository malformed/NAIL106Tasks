package wait4me;

import battlecode.common.*;

public class Memory {
    static final int ROBOT_DATA_OFFSET = 33535;

    static RobotController rc;

    static void init(RobotController rc)
    {
        Memory.rc = rc;
    }

    static int get(int address) throws GameActionException
    {
        return rc.readBroadcast(address);
    }

    static void set(int address, int value) throws GameActionException
    {
        rc.broadcast(address, value);
    }

    /**
     * saves MapLocation at given addres
     */
    static void storeLocation(int address, MapLocation loc) throws GameActionException
    {
        rc.broadcast(address, loc.x);
        rc.broadcast(address + 1, loc.y);
    }

    static MapLocation loadLocation(int address) throws GameActionException
    {
        int x = rc.readBroadcast(address);
        int y = rc.readBroadcast(address + 1);

        return new MapLocation(x,y);
    }

    static int getRobotData(RobotController rc) throws GameActionException
    {
        return getRobotData(rc.getID());
    }

    static int getRobotData(int id) throws GameActionException
    {
        return rc.readBroadcast(id + ROBOT_DATA_OFFSET);
    }

    static void setRobotData(RobotController rc, int data) throws GameActionException
    {
        setRobotData(rc.getID(), data);
    }

    static void setRobotData(int id, int data) throws GameActionException
    {
        rc.broadcast(id + ROBOT_DATA_OFFSET, data);
    }


    static boolean getBit(int data, int mask)
    {
        return (data & mask) == mask;
    }

    static int setBit(int data, int mask, boolean value)
    {
        return value ? setBit(data, mask) : clearBit(data, mask);
    }


    static int setBit(int data, int mask)
    {
        return (data | mask);
    }

    static int clearBit(int data, int mask)
    {
        return (data & ~mask);
    }
}

