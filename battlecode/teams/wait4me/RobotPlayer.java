package wait4me;

import battlecode.common.*;
import java.util.*;

import wait4me.Common;
import wait4me.Memory;
import wait4me.Strategy;
import wait4me.Unit;

public class RobotPlayer {

    static final int ATTACK_CMD = 100;
    static final int BACKUP_CMD = 101;

    static final int SOLDIER_DIRECTION_MASK = 0x7;
    static final int NEW_SOLDIER_MASK = 0x8;
    static final int SOLDIER_STATE_MASK = 0xF0;

    // ......
    
    static final int SQUAD_SIZE = 20;

	static RobotController rc;
	static Team myTeam;
	static Team enemyTeam;
	static int myRange;
	static Random rand;

	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};

    /**
     * unit states:
     *   (0) undefined - spawned in that state
     *   (1) wait      - waits on spot
     *   (2) move      - move to the target stored at MOVE_TARGET
     *   (3) attack    - attack taget with location at ATTACK_TARGET
     */

	public static void run(RobotController tomatojuice) {
		rc = tomatojuice;
		rand = new Random(rc.getID());

        Common.rand = new Random(rc.getID());

		myRange = rc.getType().attackRadiusSquared;
        Common.hqLocation = rc.senseHQLocation();
		Common.enemyLocation = rc.senseEnemyHQLocation();
		// Direction lastDirection = null;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		RobotInfo[] myRobots;

        // boolean attack = false;
        // boolean backup = false;

		while(true) {

            Memory.init(rc);

			try {
				rc.setIndicatorString(0, "robot type: " + rc.getType());

                // attack = Commands.get(ATTACK_CMD);
                // backup = Commands.get(BACKUP_CMD);

			} catch (Exception e) {
				System.out.println("Unexpected exception");
				e.printStackTrace();
			}


			if (rc.getType() == RobotType.HQ) {
				try {
                    MapLocation rallyPoint = null;

					myRobots = rc.senseNearbyRobots(999999, myTeam);

                    MapLocation[] towers = rc.senseEnemyTowerLocations();
                    MapLocation nearestTarget = null;
                    int d = Integer.MAX_VALUE;

                    for (MapLocation tower : towers) {
                        int hqDist = tower.distanceSquaredTo(Common.hqLocation);
                        if (hqDist < d) {
                            nearestTarget = tower;
                            d = hqDist;
                        }
                    }

                    if (nearestTarget == null) {
                        nearestTarget = Common.enemyLocation;
                    }

                    Memory.storeLocation(Common.Address.ATTACK_TARGET, nearestTarget);
                    rc.setIndicatorDot(nearestTarget, 0, 0, 255);

                    // MapLocation loc = rc.senseHQLocation().add(Direction.NORTH,2);
                    // rc.setIndicatorDot(loc, 255, 0, 0);
                    //
                    int dist2enemy = Integer.MAX_VALUE;
                    MapLocation closest_tower = null;

					int numBeavers = 0;
					int numMiners = 0;
					int numSoldiers = 0;
					int numTanks = 0;
					int numBashers = 0;
					int numBarracks = 0;
					int numFactories = 0;
                    // int numTowers = 0;
                    int numMinerFactories = 0;
                    int numSupplies = 0;

                    int soldiersWaiting = 0;

					for (RobotInfo r : myRobots) {
						RobotType type = r.type;
                        Unit.data = Memory.getRobotData(r.ID);

                        if (Unit.getState() == Unit.State.SUPPLY) {

                            if (Unit.hasSupplyRequest()) {
                                rc.transferSupplies(Strategy.SUPPLY_TRANSFER_AMOUNT, r.location);
                            } else if (r.supplyLevel > 0/* Strategy.SUPPLY_TRANSFER_AMOUNT */) {
                                if (Common.Helper.isWorker(r)) {
                                    if (r.type == RobotType.BEAVER) {
                                        Unit.setState(Unit.State.BUILD);
                                    } else {
                                        Unit.setState(Unit.State.MINE);
                                    }
                                } else {
                                    Unit.setState(Unit.State.MOVE);
                                }
                            }
                        }

						if (type == RobotType.SOLDIER || type == RobotType.TANK) {
							if (type == RobotType.SOLDIER) {
                                ++numSoldiers;
                            } else {
                                ++numTanks;
                            }

                            if (Unit.getState() == Unit.State.WAIT) {
                                ++soldiersWaiting;
                            }

                            /*
                            Unit.data = Memory.getRobotData(r.ID);
                            if (Unit.getState() == Unit.State.UNDEFINED) {
                                Unit.setState(Unit.State.MOVE);
                            }
                            if (Unit.getState() == Unit.State.WAIT) {
                                ++soldiersWaiting;
                            }
                            Memory.setRobotData(r.ID, Unit.data);
                            */
						} else if (type == RobotType.BASHER) {
							numBashers++;
						} else if (type == RobotType.BEAVER) {
							numBeavers++;
						} else if (type == RobotType.MINER) {
							numMiners++;
						} else if (type == RobotType.BARRACKS) {
							numBarracks++;
						} else if (type == RobotType.TANKFACTORY) {
							++numFactories;
                        } else if (type == RobotType.TOWER) {
                            int hp = Memory.getRobotData(r.ID);
                            if (hp > r.health) {
                                rallyPoint = r.location;
                            }
                            int dist = r.location.distanceSquaredTo(Common.enemyLocation);
                            if (dist < dist2enemy) {
                                dist2enemy = dist;
                                closest_tower = r.location;
                            }
                            // ++numTowers;
                            Memory.setRobotData(r.ID, (int)r.health);
                        } else if (type == RobotType.MINERFACTORY) {
                            ++numMinerFactories;
                        } else if (type == RobotType.SUPPLYDEPOT) {
                            ++numSupplies;
                        }
                        Memory.setRobotData(r.ID, Unit.data);
					}

                    if (soldiersWaiting >= SQUAD_SIZE) {
                        for (RobotInfo r : myRobots) {
                            if (r.type == RobotType.SOLDIER || r.type == RobotType.TANK) {
                                Unit.data = Memory.getRobotData(r.ID);
                                if (Unit.getState() == Unit.State.WAIT) {
                                    Unit.setState(Unit.State.ATTACK);
                                }
                                Memory.setRobotData(r.ID, Unit.data);
                            }
                        }
                    }

                    rc.setIndicatorString(1, "target: " + nearestTarget.toString());
                    rc.setIndicatorString(2, "waiting: " + Integer.toString(soldiersWaiting));

					Memory.set(Common.Address.NUM_BEAVERS, numBeavers);
					Memory.set(Common.Address.NUM_SOLDIERS, numSoldiers);
					Memory.set(Common.Address.NUM_BASHERS, numBashers);
					Memory.set(Common.Address.NUM_TANKS, numTanks);
					Memory.set(Common.Address.NUM_BARRACKS, numBarracks);
					Memory.set(Common.Address.NUM_FACTORIES, numFactories);
					// rc.broadcast(NUM_FACTORIES, numFactories);
                    Memory.set(Common.Address.NUM_MINERFACTORIES, numMinerFactories);
                    Memory.set(Common.Address.NUM_MINERS, numMiners);
                    Memory.set(Common.Address.NUM_SUPPLY_DEPOTS, numSupplies);

                    if (rallyPoint == null) {
                        rallyPoint = closest_tower;
                    } else {
                        rc.setIndicatorDot(rallyPoint, 255, 0, 0);
                    }

                    if (closest_tower == null) {
                        rallyPoint = Common.hqLocation;
                    }

                    Direction enemyDir = rallyPoint.directionTo(Common.enemyLocation);
                    MapLocation gatherPoint = rallyPoint.add(enemyDir).add(enemyDir);
                    Memory.storeLocation(Common.Address.MOVE_TARGET, gatherPoint);
                    rc.setIndicatorDot(gatherPoint, 0, 255, 0);

					if (rc.isWeaponReady()) {
						attackSomething();
					}

                    /*
					if (rc.isCoreReady() && rc.getTeamOre() >= 100 &&
                        fate < Math.pow(1.2,12-numBeavers)*10000) {
						trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
					}
                    */
                    if (numBeavers < 3 && rc.isCoreReady() &&
                        rc.getTeamOre() >= Common.Costs.BEAVER) {
						trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
                    }

                    int supply = (int)rc.getSupplyLevel(); 
                    Memory.set(Common.Address.SUPPLY, supply);
                    /*
                    if (numSoldiers > 40) {
                        Commands.set(ATTACK_CMD, true);
                    }
                    */
				} catch (Exception e) {
					System.out.println("HQ Exception");
					e.printStackTrace();
				}
			}

            try {
                Unit.init(rc);
            } catch (GameActionException e) {
				System.out.println("Unit::init Exception");
				e.printStackTrace();
            }

			if (rc.getType() == RobotType.TOWER) {
				try {
					if (rc.isWeaponReady()) {
						attackSomething();
					}
				} catch (Exception e) {
					System.out.println("Tower Exception");
					e.printStackTrace();
				}
			}

			if (rc.getType() == RobotType.BASHER) {
			}

			if (rc.getType() == RobotType.SOLDIER) {
				try {
					if (rc.isWeaponReady()) {
						attackSomething();
					}
                    Military.process();
                } catch (GameActionException e) {
					System.out.println("Soldier exception");
                    e.printStackTrace();
                }
			}

			if (rc.getType() == RobotType.TANK) {
				try {
					if (rc.isWeaponReady()) {
						attackSomething();
                    }
                    Military.process();
                } catch (GameActionException e) {
					System.out.println("Soldier exception");
                    e.printStackTrace();
                }
			}

			if (rc.getType() == RobotType.BEAVER) {
				try {
					if (rc.isWeaponReady()) {
						attackSomething();
					}

                    Miner.process();

				} catch (Exception e) {
					System.out.println("Beaver Exception");
					e.printStackTrace();
				}
			}

            if (rc.getType() == RobotType.MINER) {
                try {
                    Miner.process();
                } catch (GameActionException ex) {
					System.out.println("Miner Exception");
					ex.printStackTrace();
                }
            }

			if (rc.getType() == RobotType.MINERFACTORY) {
                try {
					int numMiners = Memory.get(Common.Address.NUM_MINERS);

                    if (numMiners < 20 && rc.isCoreReady() && rc.getTeamOre() >= Common.Costs.MINER) {
                        trySpawn(directions[rand.nextInt(8)], RobotType.MINER);
                    }
                } catch(GameActionException e) {
					System.out.println("Barracks Exception");
                }
			}

			if (rc.getType() == RobotType.BARRACKS) {
                try {
					int factories = Memory.get(Common.Address.NUM_FACTORIES);

                    if (rc.isCoreReady() &&
                            rc.getTeamOre() >= Common.Costs.SOLDIER) {
                        trySpawn(directions[rand.nextInt(8)],RobotType.SOLDIER);
                        // trySpawn(Direction.NORTH,RobotType.SOLDIER);
                    }
                } catch(GameActionException e) {
					System.out.println("Barracks Exception");
                }
			}

			if (rc.getType() == RobotType.TANKFACTORY) {
                try {
					int numMiners = Memory.get(Common.Address.NUM_MINERS);
                    if (rc.isCoreReady() && rc.getTeamOre() >= Common.Costs.TANK) {
                        trySpawn(directions[rand.nextInt(8)],RobotType.TANK);
                    }
                } catch(GameActionException e) {
					System.out.println("Barracks Exception");
                }
			}

            try {
                Unit.done();
            } catch (GameActionException e) {
				System.out.println("Unit::done Exception");
				e.printStackTrace();
            }

			rc.yield();
		}
	}

	// This method will attack an enemy in sight, if there is one
	static void attackSomething() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		if (enemies.length > 0) {
			rc.attackLocation(enemies[0].location);
		}
	}

	// This method will attempt to move in Direction d (or as close to it as possible)
	static void tryMove(Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2};
		int dirint = directionToInt(d);
		// boolean blocked = false;
		while (offsetIndex < 5 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			++offsetIndex;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint+offsets[offsetIndex]+8)%8]);
		}
	}

	// This method will attempt to spawn in the given direction (or as close to it as possible)
	static void trySpawn(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = directionToInt(d);
		// boolean blocked = false;
		while (offsetIndex < 8 && !rc.canSpawn(directions[(dirint+offsets[offsetIndex]+8)%8], type)) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.spawn(directions[(dirint+offsets[offsetIndex]+8)%8], type);
		}
	}

	// This method will attempt to build in the given direction (or as close to it as possible)
	static void tryBuild(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = directionToInt(d);
		// boolean blocked = false;
		while (offsetIndex < 8 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.build(directions[(dirint+offsets[offsetIndex]+8)%8], type);
		}
	}

	static int directionToInt(Direction d) {
		switch(d) {
			case NORTH:
				return 0;
			case NORTH_EAST:
				return 1;
			case EAST:
				return 2;
			case SOUTH_EAST:
				return 3;
			case SOUTH:
				return 4;
			case SOUTH_WEST:
				return 5;
			case WEST:
				return 6;
			case NORTH_WEST:
				return 7;
			default:
				return -1;
		}
	}

    static class Commands
    {
        static void set(int address, boolean value) throws GameActionException 
        { 
            rc.broadcast(address, (value ? 1: 0));
        }

        static boolean get(int address) throws GameActionException
        {
            return rc.readBroadcast(address) == 1;
        }
    }
}
