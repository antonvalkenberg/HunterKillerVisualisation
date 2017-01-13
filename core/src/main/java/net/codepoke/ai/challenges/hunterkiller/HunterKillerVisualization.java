package net.codepoke.ai.challenges.hunterkiller;

import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerRules;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;
import net.codepoke.ai.challenge.hunterkiller.Player;
import net.codepoke.ai.challenges.hunterkiller.ui.MatchVisualization;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class HunterKillerVisualization
		extends MatchVisualization<HunterKillerState, HunterKillerAction> {

	public HunterKillerRenderer renderer;

	public HunterKillerVisualization() {
		super(new HunterKillerRules());
	}

	@Override
	public HunterKillerRenderer createRenderer(MatchVisualization<HunterKillerState, HunterKillerAction> parent, Skin skin) {
		renderer = new HunterKillerRenderer(parent, skin);
		return renderer;
	}

	@Override
	public String[] getPlayers(HunterKillerState state) {
		Player[] players = state.getPlayers();
		String[] names = new String[players.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = players[i].getName();
		}
		return names;
	}

	@Override
	public int[] getScores(HunterKillerState state) {
		Player[] players = state.getPlayers();
		int[] scores = new int[players.length];
		for (int i = 0; i < scores.length; i++) {
			scores[i] = players[i].getScore();
		}
		return scores;
	}

}
