package net.codepoke.ai.challenges.hunterkiller.desktop;

import java.util.List;
import java.util.Random;

import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;
import net.codepoke.ai.challenge.hunterkiller.Map;
import net.codepoke.ai.challenge.hunterkiller.MoveGenerator;
import net.codepoke.ai.challenge.hunterkiller.Player;
import net.codepoke.ai.challenge.hunterkiller.enums.UnitOrderType;
import net.codepoke.ai.challenge.hunterkiller.enums.UnitType;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Base;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Unit;
import net.codepoke.ai.challenge.hunterkiller.orders.BaseOrder;
import net.codepoke.ai.challenge.hunterkiller.orders.UnitOrder;
import net.codepoke.ai.network.AIBot;

public class TestBot
		extends AIBot<HunterKillerState, HunterKillerAction> {

	private static Random r = new Random();
	private static final double noUnitOrderThreshold = 0.2;
	private static final double noBaseOrderThreshold = 0.3;

	public TestBot() {
		// TODO Create new BotUID for HunterKiller test bot
		super("dpeo9nqfhvchpg3tf1m49ss1hd", HunterKillerState.class, HunterKillerAction.class);
	}

	public HunterKillerAction handle(HunterKillerState state) {
		// Create a random action
		HunterKillerAction random = new HunterKillerAction(state);
		Player player = state.getPlayer(state.getCurrentPlayer());

		// Check if the base exists and we want to order the base to do something
		if (player.isBaseExists() && r.nextDouble() >= noBaseOrderThreshold) {
			// Get all legal orders for this base
			List<BaseOrder> legalOrders = MoveGenerator.getAllLegalOrders(state, (Base) state.getMap()
																								.getObject(player.getBaseID()));

			// Add a random order
			if (!legalOrders.isEmpty()) {
				random.addOrder(legalOrders.get(r.nextInt(legalOrders.size())));
			}
		}

		Map map = state.getMap();

		// Move through all Units
		for (Unit unit : player.getUnits(state.getMap())) {
			// Check if we want to do nothing
			if (r.nextDouble() <= noUnitOrderThreshold)
				continue;

			// Get all legal rotation orders for this unit
			List<UnitOrder> legalRotationOrders = MoveGenerator.getAllLegalOrders(state, unit, true, false, false);
			// Get all legal move orders for this unit
			List<UnitOrder> legalMoveOrders = MoveGenerator.getAllLegalOrders(state, unit, false, true, false);
			// Get all legal attack orders for this unit
			List<UnitOrder> legalAttackOrders = MoveGenerator.getAllLegalOrders(state, unit, false, false, true);

			// Remove all attacks without a proper target or ally target, unless it's a Medic's special attack
			legalAttackOrders.removeIf((order) -> {
				Unit target = map.getUnitAtLocation(order.getTargetLocation());
				return target == null
						|| (target.getControllingPlayerID() == unit.getControllingPlayerID() && !(order.getUnitType() == UnitType.Medic && order.getOrderType() == UnitOrderType.ATTACK_SPECIAL));
			});

			double attackType = r.nextDouble();
			// Do a random rotation with 20% chance
			if (attackType <= 0.2 && !legalRotationOrders.isEmpty()) {
				random.addOrder(legalRotationOrders.get(r.nextInt(legalRotationOrders.size())));
			}
			// Do a random move with 50% chance
			else if (attackType <= 0.7 && !legalMoveOrders.isEmpty()) {
				random.addOrder(legalMoveOrders.get(r.nextInt(legalMoveOrders.size())));
			}
			// Do a random attack with 30% chance
			else if (!legalAttackOrders.isEmpty()) {
				random.addOrder(legalAttackOrders.get(r.nextInt(legalAttackOrders.size())));
			}
		}

		// Return the randomly created action
		return random;
	}

}
