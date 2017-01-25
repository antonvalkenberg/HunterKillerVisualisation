package net.codepoke.ai.challenges.hunterkiller.desktop;

import static net.codepoke.ai.challenges.hunterkiller.desktop.StreamExtensions.stream;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerRules;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;
import net.codepoke.ai.challenge.hunterkiller.Map;
import net.codepoke.ai.challenge.hunterkiller.MapLocation;
import net.codepoke.ai.challenge.hunterkiller.MoveGenerator;
import net.codepoke.ai.challenge.hunterkiller.Player;
import net.codepoke.ai.challenge.hunterkiller.enums.Direction;
import net.codepoke.ai.challenge.hunterkiller.enums.UnitType;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Structure;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Wall;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Medic;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Soldier;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Unit;
import net.codepoke.ai.challenge.hunterkiller.orders.HunterKillerOrder;
import net.codepoke.ai.challenge.hunterkiller.orders.StructureOrder;
import net.codepoke.ai.challenge.hunterkiller.orders.UnitOrder;
import net.codepoke.ai.network.AIBot;
import one.util.streamex.StreamEx;

/**
 * Represents an {@link AIBot} for the HunterKiller game that generates orders for it's structures and units by
 * following a simple rules-hierarchy.
 * 
 * @author Anton Valkenberg (anton.valkenberg@gmail.com)
 *
 */
public class RulesBot
		extends AIBot<HunterKillerState, HunterKillerAction> {

	private static final boolean DEBUG = true;
	private static final Random r = new Random();
	HunterKillerRules rulesEngine = new HunterKillerRules();

	public RulesBot() {
		super("", HunterKillerState.class, HunterKillerAction.class);
	}

	@Override
	public HunterKillerAction handle(HunterKillerState state) {
		// string builders for debugging
		StringBuilder possibleCheckFails = new StringBuilder();
		StringBuilder orderFailures = new StringBuilder();

		// Create an action
		HunterKillerAction rulesAction = new HunterKillerAction(state);

		// Make a copy of the state, so we can mutate it
		HunterKillerState copyState = state.copy();

		// Prepare the state for this player, this is a temporary hack to simulate the server-environment
		copyState.prepare(state.getActivePlayerID());
		Player player = copyState.getActivePlayer();
		Map map = copyState.getMap();

		// Maintain a counter on the amount of orders we create, to correctly set their index in the action
		int orderCounter = 0;

		// Get some things we'll need to access
		List<Structure> structures = player.getStructures(map);
		List<Unit> units = player.getUnits(map);
		List<Structure> enemyStructures = stream(map, Structure.class).filter(i -> i.isUnderControl() && !i.isControlledBy(player))
																		.toList();
		List<Unit> enemyUnits = stream(map, Unit.class).filter(i -> !i.isControlledBy(player))
														.toList();

		// Go through our structures
		for (Structure structure : structures) {
			// Check if the structure can spawn anything in this state
			if (structure.canSpawn(copyState)) {
				// Spawn a type we don't have yet, or have the least of
				long soldierCount = units.stream()
											.filter(i -> i.getType() == UnitType.Soldier)
											.count();
				long medicCount = units.stream()
										.filter(i -> i.getType() == UnitType.Medic)
										.count();
				long infectedCount = units.stream()
											.filter(i -> i.getType() == UnitType.Infected)
											.count();

				boolean spawnUnit = false;
				UnitType spawnType = null;
				// Check infected first, these have highest priority
				if (infectedCount <= medicCount && infectedCount <= soldierCount) {
					spawnUnit = true;
					spawnType = UnitType.Infected;
				}
				// Next, check medics
				else if (medicCount <= soldierCount && medicCount <= infectedCount) {
					spawnUnit = true;
					spawnType = UnitType.Medic;
				}
				// Check soldiers
				else if (soldierCount <= medicCount && soldierCount <= infectedCount) {
					spawnUnit = true;
					spawnType = UnitType.Soldier;
				}

				// Check if we want to spawn a unit, have set the type we want, and if we can execute an order of that
				// type
				if (spawnUnit && spawnType != null && structure.canSpawn(copyState, spawnType)) {
					// Order the spawning of a medic
					StructureOrder order = structure.spawn(spawnType);
					// Add the order if it's possible
					if (addOrderIfPossible(rulesAction, orderCounter, copyState, order, possibleCheckFails, orderFailures)) {
						// Don't create another order for this object
						continue;
					}
				}
			}
		}

		// Go through our Units
		for (Unit unit : units) {
			// If we are a Medic and can use our heal ability
			if (unit instanceof Medic && unit.canUseSpecialAttack()) {
				// Check if there are any ally units within range and field-of-view that are damaged
				List<Unit> friendlyDamagedInRange = units.stream()
															.filter(i -> unit.isWithinAttackRange(i.getLocation())
																			&& unit.getFieldOfView()
																					.contains(i.getLocation()) && i.isDamaged())
															.collect(Collectors.toList());
				if (!friendlyDamagedInRange.isEmpty()) {
					// Create an order to heal the first unit in the list
					UnitOrder order = unit.attack(friendlyDamagedInRange.get(0), map, true);
					// Add the order if it's possible
					if (addOrderIfPossible(rulesAction, orderCounter, copyState, order, possibleCheckFails, orderFailures)) {
						// Don't create another order for this unit
						continue;
					}
				}
			}

			// Check if there are any enemy units within our field-of-view
			List<Unit> enemiesInRange = StreamEx.of(enemyUnits)
												.filter(i -> unit.isWithinAttackRange(i.getLocation()))
												.filter(i -> unit.getFieldOfView()
																	.contains(i.getLocation()))
												.toList();
			if (!enemiesInRange.isEmpty()) {

				// If we are a Soldier and can use our grenade ability
				if (unit instanceof Soldier && unit.canUseSpecialAttack()) {
					// Find the maximum units within the blast range (don't use a special for just 1 unit)
					long maxUnitsInBlastRange = 1;
					MapLocation targetLocation = null;

					for (Unit enemy : enemiesInRange) {
						// Get the blast area of the attack, if targeted on this enemy
						List<MapLocation> aoe = map.getAreaAround(enemy.getLocation(), true);
						// Get the units in the blast area
						List<Unit> unitsInBlast = StreamEx.of(aoe)
															.filter(i -> map.getUnitAtLocation(i) != null)
															.map(i -> (Unit) map.getUnitAtLocation(i))
															.toList();

						// Check if there are any allies in the blast
						if (StreamEx.of(unitsInBlast)
									.anyMatch(i -> i.isControlledBy(player)))
							continue;

						// Count the number of enemy units within the area
						long enemiesInBlast = StreamEx.of(unitsInBlast)
														.filter(i -> !i.isControlledBy(player))
														.count();

						// Check if there are more enemies in this area than our current maximum
						if (enemiesInBlast > maxUnitsInBlastRange) {
							// Remember this new max and target
							maxUnitsInBlastRange = enemiesInBlast;
							targetLocation = enemy.getLocation();
						}
					}

					// Check if we found a target location
					if (targetLocation != null) {
						UnitOrder order = unit.attack(targetLocation, true);

						// Add the order if it's possible
						if (addOrderIfPossible(rulesAction, orderCounter, copyState, order, possibleCheckFails, orderFailures)) {
							// Don't create another order for this unit
							continue;
						}
					}
				}

				// Attack any enemy unit
				Unit enemy = enemiesInRange.get(0);
				UnitOrder order = unit.attack(enemy, map, false);
				// Add the order if it's possible
				if (addOrderIfPossible(rulesAction, orderCounter, copyState, order, possibleCheckFails, orderFailures)) {
					// Don't create another order for this unit
					continue;
				}
			}

			// Rotate to face enemy structure / enemy unit
			Optional<Unit> closestEnemy = StreamEx.of(enemyUnits)
													.sortedBy(i -> MapLocation.getManhattanDist(unit.getLocation(), i.getLocation()))
													.findFirst();
			if (closestEnemy.isPresent()) {
				Direction directionToEnemy = MapLocation.getDirectionTo(unit.getLocation(), closestEnemy.get()
																										.getLocation());
				// Check if any direction could be found, and we are not already facing that direction
				if (directionToEnemy != null && unit.getOrientation() != directionToEnemy) {
					UnitOrder order = unit.rotate(Direction.rotationRequiredToFace(unit, directionToEnemy));
					// Add the order if it's possible
					if (addOrderIfPossible(rulesAction, orderCounter, copyState, order, possibleCheckFails, orderFailures)) {
						// Don't create another order for this unit
						continue;
					}
				}
			}
			// If there is no enemy within our player's field-of-view, try to rotate towards an enemy structure
			else if (false) {
				Optional<Structure> closestStructure = StreamEx.of(enemyStructures)
																.sortedBy(i -> MapLocation.getManhattanDist(unit.getLocation(),
																											i.getLocation()))
																.findFirst();
				if (closestStructure.isPresent()) {
					Direction directionToStructure = MapLocation.getDirectionTo(unit.getLocation(), closestStructure.get()
																													.getLocation());
					// Check if any direction could be found, and we are not already facing that direction
					if (directionToStructure != null && unit.getOrientation() != directionToStructure) {
						UnitOrder order = unit.rotate(Direction.rotationRequiredToFace(unit, directionToStructure));
						// Add the order if it's possible
						if (addOrderIfPossible(rulesAction, orderCounter, copyState, order, possibleCheckFails, orderFailures)) {
							// Don't create another order for this unit
							continue;
						}
					}
				}
			}

			// TODO walk along path to an enemy base

			// Rotate away from a wall
			if (map.getFeatureAtLocation(map.getLocationInDirection(unit.getLocation(), unit.getOrientation(), 1)) instanceof Wall) {
				UnitOrder order = unit.rotate(r.nextBoolean());
				// Add the order if it's possible
				if (addOrderIfPossible(rulesAction, orderCounter, copyState, order, possibleCheckFails, orderFailures)) {
					// Don't create another order for this unit
					continue;
				}
			}

			// Random walk
			List<UnitOrder> moveOrders = MoveGenerator.getAllLegalMoveOrders(copyState, unit);
			if (!moveOrders.isEmpty()) {
				UnitOrder order = moveOrders.get(r.nextInt(moveOrders.size()));
				// Add the order if it's possible
				if (addOrderIfPossible(rulesAction, orderCounter, copyState, order, possibleCheckFails, orderFailures)) {
					// Don't create another order for this unit
					continue;
				}
			}
		}

		if (DEBUG && possibleCheckFails.length() > 0) {
			System.out.printf(	"RB(%d)R(%d)T(%d): some orders not possible, Reasons:%n%s%n",
								player.getID(),
								state.getCurrentRound(),
								state.getMap().currentTick,
								possibleCheckFails.toString());
		}
		if (DEBUG && orderFailures.length() > 0) {
			System.out.printf(	"RB(%d)R(%d)T(%d): some orders failed, Reasons:%n%s%n",
								player.getID(),
								state.getCurrentRound(),
								state.getMap().currentTick,
								orderFailures.toString());
		}

		return rulesAction;
	}

	/**
	 * Adds an order to an action if it is possible to execute it on the provided state.
	 * 
	 * @param action
	 *            The {@link HunterKillerAction} that the order should be added to.
	 * @param orderIndex
	 *            The index the order will have within the action.
	 * @param state
	 *            The {@link HunterKillerState} to apply the order on.
	 * @param order
	 *            The order.
	 * @param possibleCheckFails
	 *            If the order is not possible in the provided state, this will contain the reason(s) why.
	 * @param orderFails
	 *            If the order failed to execute on the provided state, this will contain the reason(s) why.
	 * @return Whether or not the order was added to the action.
	 */
	private boolean addOrderIfPossible(HunterKillerAction action, int orderIndex, HunterKillerState state, HunterKillerOrder order,
			StringBuilder possibleCheckFails, StringBuilder orderFails) {
		boolean addedOrder = false;
		// Set the order's index to the correct number
		order.setActionIndex(orderIndex);

		// Make sure this order works on the copyState
		if (rulesEngine.isOrderPossible(state, order, possibleCheckFails)) {
			// We created an order, so up our internal counter
			orderIndex++;
			// Add the order to our action
			action.addOrder(order);
			addedOrder = true;
			// Execute this order on the state
			rulesEngine.executeOrder(state, order, orderFails);
		}

		return addedOrder;
	}

}
