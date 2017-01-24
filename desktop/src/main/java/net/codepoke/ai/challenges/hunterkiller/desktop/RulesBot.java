package net.codepoke.ai.challenges.hunterkiller.desktop;

import java.util.List;

import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;
import net.codepoke.ai.challenge.hunterkiller.Map;
import net.codepoke.ai.challenge.hunterkiller.Player;
import net.codepoke.ai.challenge.hunterkiller.enums.StructureOrderType;
import net.codepoke.ai.challenge.hunterkiller.enums.UnitType;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Structure;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Unit;
import net.codepoke.ai.challenge.hunterkiller.orders.StructureOrder;
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
		List<Structure> structures = player.getStructures(copyState.getMap());
		List<Unit> units = player.getUnits(copyState.getMap());

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

				// Check medics first, these have highest priority
				if (medicCount <= soldierCount && medicCount <= infectedCount) {
					// Order the spawning of a medic
					StructureOrder order = new StructureOrder(structure, StructureOrderType.SPAWN_MEDIC, orderCounter);
					// We created an order, so up our internal counter
					orderCounter++;
					// Add the order to our action
					rulesAction.addOrder(order);
					// Don't create another order for this structure
					continue;
				}
				// Next, check infected
				else if (infectedCount <= medicCount && infectedCount <= soldierCount) {

				}
			}

		}

		return rulesAction;
	}

}
