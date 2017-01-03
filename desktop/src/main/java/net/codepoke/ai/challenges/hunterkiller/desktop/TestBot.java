package net.codepoke.ai.challenges.hunterkiller.desktop;

import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;
import net.codepoke.ai.challenge.hunterkiller.orders.NullMove;
import net.codepoke.ai.network.AIBot;

public class TestBot
		extends AIBot<HunterKillerState, HunterKillerAction> {

	public TestBot() {
		// TODO Create new BotUID for HunterKiller test bot
		super("dpeo9nqfhvchpg3tf1m49ss1hd", HunterKillerState.class, HunterKillerAction.class);
	}

	public HunterKillerAction handle(HunterKillerState state) {
		// TODO Create random play bot
		return new NullMove(state);
	}
}
