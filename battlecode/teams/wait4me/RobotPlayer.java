package wait4me;

import battlecode.common.*;
import java.util.*;

import wait4me.Common;
import wait4me.Memory;
import wait4me.Strategy;
import wait4me.Unit;

public class RobotPlayer {

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

                Common.turn = Memory.get(Common.Address.CURRENT_TURN);

			} catch (Exception e) {
				System.out.println("Unexpected exception");
				e.printStackTrace();
			}

			if (rc.getType() == RobotType.HQ) {
				try {
                    // saves order of current turn 
                    Memory.set(Common.Address.CURRENT_TURN, Common.turn + 1);

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
                    int numMilitary = 0;

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
                            ++numMilitary;
							if (type == RobotType.SOLDIER) {
                                ++numSoldiers;
                            } else {
                                ++numTanks;
                            }

                            if (Unit.getState() == Unit.State.WAIT) {
                                ++soldiersWaiting;
                            }

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
                                System.out.printf("+: %d -: %d\n", hp, (int)r.health);
                            }
                            int dist = r.location.distanceSquaredTo(Common.enemyLocation);
                            if (dist < dist2enemy) {
                                dist2enemy = dist;
                                closest_tower = r.location;
                            }
                            // ++numTowers;
                        } else if (type == RobotType.MINERFACTORY) {
                            ++numMinerFactories;
                        } else if (type == RobotType.SUPPLYDEPOT) {
                            ++numSupplies;
                        }
                        Memory.setRobotData(r.ID, Unit.data);
					}

                    /*
                    if (soldiersWaiting >= SQUAD_SIZE) {
                        for (RobotInfo r : myRobots) {
                            if (r.type == RobotType.SOLDIER || r.type == RobotType.TANK) {
                                Unit.data = Memory.getRobotData(r.ID);
                                if (Unit.getState() == Unit.State.WAIT) {

                                    Memory.storeLocation(Common.Address.ATTACK_TARGET,
                                                         nearestTarget);
                                    rc.setIndicatorDot(nearestTarget, 0, 0, 255);

                                    Unit.setState(Unit.State.ATTACK);
                                }
                                Memory.setRobotData(r.ID, Unit.data);
                            }
                        }
                    }
                    */


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

                    int da = Memory.get(Common.Address.DEFEND_ALERT);
                    boolean defenseAlert = da + 100 > Common.turn;

                    if (defenseAlert) {
                        rallyPoint = Memory.loadLocation(Common.Address.DEFEND_TARGET);
                    }

                    Direction enemyDir = rallyPoint.directionTo(Common.enemyLocation);
                    MapLocation gatherPoint = rallyPoint.add(enemyDir).add(enemyDir);

                    Memory.storeLocation(Common.Address.MOVE_TARGET, gatherPoint);

                    rc.setIndicatorDot(gatherPoint, 0, 255, 0);
                    rc.setIndicatorString(1, "gather point: " + gatherPoint.toString() +
                                             " !!! " + Integer.toString(da) +
                                             "/" + Integer.toString(Common.turn));
                    // rc.setIndicatorString(2, "waiting: " + Integer.toString(soldiersWaiting));
                    rc.setIndicatorString(2, "waiting: " + Integer.toString(soldiersWaiting));

                    for (RobotInfo r : myRobots) {
                        if (!Common.Helper.isWorker(r)) {
                            Unit.data = Memory.getRobotData(r.ID);
                            if (defenseAlert) {
                                Unit.setState(Unit.State.MOVE);
                            } else if (soldiersWaiting >= SQUAD_SIZE) {

                                Memory.storeLocation(Common.Address.ATTACK_TARGET,
                                                     nearestTarget);
                                rc.setIndicatorDot(nearestTarget, 0, 0, 255);

                                Unit.setState(Unit.State.ATTACK);
                            }
                            Memory.setRobotData(r.ID, Unit.data);
                        }
                    }

					if (rc.isWeaponReady()) {
						attackSomething();
					}

                    if (numBeavers < 3 && rc.isCoreReady() &&
                        rc.getTeamOre() >= Common.Costs.BEAVER) {
						trySpawn(directions[rand.nextInt(8)], RobotType.BEAVER);
                    }

                    int supply = (int)rc.getSupplyLevel(); 
                    Memory.set(Common.Address.SUPPLY, supply);
                    /*
                    if (numSoldiers > 40) {
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

		                RobotInfo[] enemies = rc.senseNearbyRobots(2 * myRange, enemyTeam);
                        int size = enemies.length;

                        if (size > 1) {
                            Memory.storeLocation(Common.Address.DEFEND_TARGET,
                                                 rc.getLocation());
                            Memory.set(Common.Address.DEFEND_ALERT, Common.turn);
                            rc.setIndicatorString(2, "DEFEND ALERT turn "
                                                      + Integer.toString(Common.turn));
                        }

                        // int hp = Unit.data;// Memory.getRobotData(rc);
                        rc.setIndicatorString(1, "nearby enemies: " + Integer.toString(size));
                        // Unit.data = (int)rc.getHealth();
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
