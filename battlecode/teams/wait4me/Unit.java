package wait4me;

import battlecode.common.*;
import wait4me.Memory;

/**
 * Common class for all units - military and miners.
 */
public class Unit
{
    enum State {
        UNDEFINED, WAIT, SUPPLY, MOVE, ATTACK, SCOUT, MINE, SEARCH_ORE, BUILD;

        static State get(int index)
        {
            return State.values()[index];
        }
    };

    static final int DIRECTION_MASK = 0x7;
    static final int BUGGING_BIT = 0x8;
    static final int STATE_MASK = 0xF0;
    static final int SUPPLY_REQUEST_BIT = 0x100;
    static final int SKIP_TURN_BIT = 0x200;
    static final int HALT_ON_WAIT_BIT = 0x400;

    static RobotController rc;
    static int data;

    /**
     * Performs robot actions based on its internal state.
     */
    static boolean process() throws GameActionException
    {
        setLabel();

        boolean processed = true;
        boolean skip = skipTurn();

        if (!rc.isCoreReady() || skip) {
            if (skip) {
                setSkipTurn(false);
            }
            return true;
        }

        switch (getState()) {
            case WAIT:
                waitAction();
                break;
            case SUPPLY:
                supply();
                break;
            case MOVE:
                move();
                break;
            case SCOUT:
                break;
            default:
                processed = false;
        }

        senseEnemy();

        return processed;
    }

    static void setLabel()
    {
        String state = "state: " + getState().toString();
        String direction = "direction: " + getDirection().toString();
        String bugging = (isBugging() ? "bugging" : "not bugging");
        String supplyRequest = (hasSupplyRequest() ? "supply request" : "no request");
        String halt = (haltOnWait() ? "halt" : "don't halt");
        String label = state + " | " + direction + " | " + bugging + " | " + supplyRequest + 
                       " | " + halt;
        Unit.rc.setIndicatorString(0, label);
    }

    // --------------------- common state actions -----------------------------
    //
    static void senseEnemy() throws GameActionException
    {
        if (Common.rand.nextInt(10) > 3) {
            return;
        }

        RobotInfo[] enemies = rc.senseNearbyRobots(4 * Unit.rc.getType().attackRadiusSquared,
                                                   rc.getTeam().opponent());
        int size = enemies.length;

        if (size > 1) {
            // lauch target
            MapLocation enemy = enemies[0].location;
            Memory.storeLocation(Common.Address.MISSILE_TARGET, enemy);
            Memory.set(Common.Address.MISSILE_TARGET_TIME, Common.turn);
            // rc.setIndicatorDot(enemy, 255, 0, 0);
            // System.out.println("enemy: " + enemy.toString());
        }
    }

    /**
     * units does nothing
     */
    static void waitAction() throws GameActionException
    {
        MapLocation loc = rc.getLocation();
        if (!haltOnWait()) {
            MapLocation target = Memory.loadLocation(Common.Address.MOVE_TARGET);
            if (loc.equals(target)) {
                // stands right on target
                setHaltOnWait(true);
            } else {
                Direction dir = rc.getLocation().directionTo(target);
                RobotInfo ri = rc.senseRobotAtLocation(loc.add(dir));
                if (ri != null) {
                    int tmp = Unit.data;
                    Unit.data = Memory.getRobotData(ri.ID);
                    boolean halt = Unit.haltOnWait();
                    Unit.data = tmp;
                    if (halt){
                        setHaltOnWait(true);
                    }
                } else {
                    if (!tryMove(dir)) {
                        setHaltOnWait(true);
                    }
                }


            }
        }
    }

    /**
     * obtains supply from HQ
     */
    static void supply() throws GameActionException
    {
        if (!hasSupplyRequest() && rc.getSupplyLevel() < 1.0) {
            int dist = rc.getLocation().distanceSquaredTo(Common.hqLocation);
            if (dist < Common.Constants.TRANSFER_RADIUS) {
                setSupplyRequest(true);
            } else {
                moveBugging(Common.hqLocation);
            }
        } else {
            if (rc.getSupplyLevel() > 0 /* Strategy.SUPPLY_TRANSFER_AMOUNT */) {
                setSupplyRequest(false);
            } else {

            }
        }
    }

    /**
     * moves unit to target given by ADDR_MOVE_TARGET
     */
    static void move() throws GameActionException
    {
        MapLocation target = Memory.loadLocation(Common.Address.MOVE_TARGET);

        // moveSimple(target);
        moveBugging(target);

        int proxTrashold = 15;

        if (rc.getLocation().distanceSquaredTo(target) < proxTrashold) {
            setState(State.WAIT);
        }
    }

    // --------------------- auxiliary methods --------------------------------
    
    static void moveSimple(MapLocation target) throws GameActionException
    {
        tryMove(Unit.rc.getLocation().directionTo(target));
    }
    
    static void moveBugging(MapLocation target) throws GameActionException
    {
        Direction dir = getDirection();
        boolean bugging = isBugging();

        if (!bugging) {
            dir = rc.getLocation().directionTo(target);
            if (rc.canMove(dir)) {
                rc.move(dir);
            } else {
                bugging = true;
            }
        } else {

            Direction left = dir.rotateLeft();
            if (rc.canMove(left)) {
                dir = left;
            } else {
                if (!rc.canMove(dir)) {
                    dir = dir.rotateRight();
                }
            }
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
            Direction targetDir = rc.getLocation().directionTo(target);
            if (rc.canMove(targetDir)) {
                dir = targetDir;
                bugging = false;
            }
        }

        setBugging(bugging);
        setDirection(dir);
    }
    

	/**
     * Attempts to move in Direction d (or as close to it as possible)
     *
     * @note    taken from ExamplePlayer.java
     */
	static boolean tryMove(Direction d) throws GameActionException
    {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		int dirint = d.ordinal();

		while (offsetIndex < 5 && !rc.canMove(dirFromInt((dirint+offsets[offsetIndex]+8)%8))) {
			++offsetIndex;
		}

		if (offsetIndex < 5) {
            Direction dir = dirFromInt((dirint+offsets[offsetIndex]+8)%8);
            if (rc.canMove(dir)) {
			    rc.move(dir);
                return true;
            }
		}
        return false;
	}

    // --------------------- utility methods ----------------------------------

    static void init(RobotController rc) throws GameActionException
    {
        Unit.rc = rc;
        data = Memory.getRobotData(rc);
    }

    static void done() throws GameActionException
    {
        Memory.setRobotData(rc, data);
    }

    static State getState()
    {
        return State.get((data & STATE_MASK) >> 4);
    }

    static void setState(State state)
    {
        setState(state.ordinal());
    }

    static void setState(int state)
    {
        onStateTransition();
        data = ((data & ~STATE_MASK) | (state << 4));
    }

    static boolean isBugging()
    {
        return Memory.getBit(data, BUGGING_BIT);
    }

    static void setBugging(boolean bugging)
    {
        data = Memory.setBit(data, BUGGING_BIT, bugging);
    }

    static boolean hasSupplyRequest()
    {
        return Memory.getBit(data, SUPPLY_REQUEST_BIT);
    }

    static void setSupplyRequest(boolean set)
    {
        data = Memory.setBit(data, SUPPLY_REQUEST_BIT, set);
    }

    static boolean skipTurn()
    {
        return Memory.getBit(data, SKIP_TURN_BIT);
    }

    static void setSkipTurn(boolean set)
    {
        data = Memory.setBit(data, SKIP_TURN_BIT, set);
    }

    static boolean haltOnWait()
    {
        return Memory.getBit(data, HALT_ON_WAIT_BIT);
    }

    static void setHaltOnWait(boolean halt)
    {
        data = Memory.setBit(data, HALT_ON_WAIT_BIT, halt);
    }

    static Direction getDirection()
    {
        return dirFromInt(data & DIRECTION_MASK);
    }

    static void setDirection(Direction dir)
    {
        data = (data & ~DIRECTION_MASK | dir.ordinal());
    }

    static Direction dirFromInt(int index)
    {
        return Direction.values()[index];
    }

    static void onStateTransition()
    {
        if (getState() == Unit.State.WAIT) {
            setHaltOnWait(false);
        }
    }
}

