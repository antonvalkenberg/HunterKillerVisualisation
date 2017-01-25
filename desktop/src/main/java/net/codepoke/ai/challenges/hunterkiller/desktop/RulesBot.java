package net.codepoke.ai.challenges.hunterkiller.desktop;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.codepoke.ai.challenge.hunterkiller.Constants;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;
import net.codepoke.ai.challenge.hunterkiller.Map;
import net.codepoke.ai.challenge.hunterkiller.MapLocation;
import net.codepoke.ai.challenge.hunterkiller.Player;
import net.codepoke.ai.challenge.hunterkiller.enums.StructureOrderType;
import net.codepoke.ai.challenge.hunterkiller.enums.UnitType;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Structure;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Medic;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Unit;
import net.codepoke.ai.challenge.hunterkiller.orders.StructureOrder;
import net.codepoke.ai.challenge.hunterkiller.orders.UnitOrder;
import net.codepoke.ai.network.AIBot;

/**
 * Represents an {@link AIBot} for the HunterKiller game that generates orders for it's structures and units by
 * following a simple rules-hierarchy.
 * 
 * @author Anton Valkenberg (anton.valkenberg@gmail.com)
 *
 */
public class RulesBot
		extends AIBot<HunterKillerState, HunterKillerAction> {

	public RulesBot() {
		super("", HunterKillerState.class, HunterKillerAction.class);
	}

	@Override
	public HunterKillerAction handle(HunterKillerState state) {
		// Create an action
		HunterKillerAction rulesAction = new HunterKillerAction(state);

		// Make a copy of the state, so we can mutate it
		HunterKillerState copyState = state.copy();

		// Maintain a counter on the amount of orders we create, to correctly set their index in the action
		int orderCounter = 0;

		// Get some things we'll need to access
		Player player = state.getActivePlayer();
		Map map = state.getMap();
		List<Structure> structures = player.getStructures(map);
		List<Unit> units = player.getUnits(map);

		List<Unit> enemyUnits = Arrays.asList(map.getObjects().items)
										.stream()
										.filter(i -> i != null && i instanceof Unit
														&& ((Unit) i).getControllingPlayerID() != player.getID())
										.map(i -> (Unit) i)
										.collect(Collectors.toList());

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
				StructureOrderType spawnType = null;
				// Check medics first, these have highest priority
				if (medicCount <= soldierCount && medicCount <= infectedCount) {
					spawnUnit = true;
					spawnType = StructureOrderType.SPAWN_MEDIC;
				}
				// Next, check infected
				else if (infectedCount <= medicCount && infectedCount <= soldierCount) {
					spawnUnit = true;
					spawnType = StructureOrderType.SPAWN_INFECTED;
				}
				// Check soldiers
				else if (soldierCount <= medicCount && soldierCount <= infectedCount) {
					spawnUnit = true;
					spawnType = StructureOrderType.SPAWN_SOLDIER;
				}

				// Check if we want to spawn a unit, have set the type we want, and if we can execute an order of that
				// type
				if (spawnUnit && spawnType != null && structure.canExecute(copyState, spawnType)) {
					// Order the spawning of a medic
					StructureOrder order = new StructureOrder(structure, spawnType, orderCounter);
					// We created an order, so up our internal counter
					orderCounter++;
					// Add the order to our action
					rulesAction.addOrder(order);
				}
			}
		}

		// Go through our Units
		for (Unit unit : units) {
			// If we are a Medic and can use our heal ability
			if (unit instanceof Medic && unit.canUseSpecialAttack()) {
				// Check if there are any ally units within range that are damaged
				List<Unit> friendlyDamagedInRange = units.stream()
															.filter(i -> i.getHpCurrent() != i.getHpMax()
																			&& MapLocation.getManhattanDist(unit.getLocation(),
																											i.getLocation()) <= Constants.MEDIC_ATTACK_RANGE)
															.collect(Collectors.toList());
				if (!friendlyDamagedInRange.isEmpty()) {
					// Create an order to heal the first unit in the list
					Unit first = friendlyDamagedInRange.get(0);
					UnitOrder order = unit.attack(first, map, true);
					// We created an order, so up our internal counter
					orderCounter++;
					// Add the order to our action
					rulesAction.addOrder(order);
				}
			}
		}

		return rulesAction;
	}

}
