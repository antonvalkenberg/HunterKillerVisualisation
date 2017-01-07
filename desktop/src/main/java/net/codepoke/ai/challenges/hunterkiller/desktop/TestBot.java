package net.codepoke.ai.challenges.hunterkiller.desktop;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;
import net.codepoke.ai.challenge.hunterkiller.Player;
import net.codepoke.ai.challenge.hunterkiller.enums.UnitOrderType;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Unit;
import net.codepoke.ai.challenge.hunterkiller.orders.UnitOrder;
import net.codepoke.ai.network.AIBot;

public class TestBot
		extends AIBot<HunterKillerState, HunterKillerAction> {

	private static Random r = new Random();

	public TestBot() {
		// TODO Create new BotUID for HunterKiller test bot
		super("dpeo9nqfhvchpg3tf1m49ss1hd", HunterKillerState.class, HunterKillerAction.class);
	}

	public HunterKillerAction handle(HunterKillerState state) {
		// TODO Create random play bot
		int actionIndex = 0;

		HunterKillerAction random = new HunterKillerAction(state);

		Player player = state.getPlayer(state.getCurrentPlayer());

		for (Unit unit : player.getUnits(state.getMap())) {
			List<UnitOrder> rotations = new ArrayList<UnitOrder>();
			rotations.add(new UnitOrder(unit, UnitOrderType.ROTATE_EAST, actionIndex));
			rotations.add(new UnitOrder(unit, UnitOrderType.ROTATE_WEST, actionIndex));

			random.addOrder(rotations.get(r.nextInt(rotations.size())));

			actionIndex++;
		}

		return random;
	}

}
