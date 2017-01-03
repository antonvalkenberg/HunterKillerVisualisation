package net.codepoke.ai.challenges.hunterkiller.desktop;

import com.badlogic.gdx.utils.Array;

import net.codepoke.ai.network.AIBot;

import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction.Move;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;

public class TestBot extends AIBot<HunterKillerState, HunterKillerAction> {
	
	Array<Move> potential = Array.with(Move.LEFT, Move.RIGHT, Move.DOWN, Move.UP, Move.PLACE_BOMB);

	public TestBot() {
		super("dpeo9nqfhvchpg3tf1m49ss1hd", HunterKillerState.class, HunterKillerAction.class);
	}

	public HunterKillerAction handle(HunterKillerState state) {
		return new HunterKillerAction(potential.random(), state.getCurrentPlayer(), state.getCurrentRound());
	}
}
