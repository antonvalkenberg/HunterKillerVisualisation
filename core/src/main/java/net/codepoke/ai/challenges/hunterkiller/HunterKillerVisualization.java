package net.codepoke.ai.challenges.hunterkiller;

import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerRules;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;
import net.codepoke.ai.challenge.hunterkiller.Player;
import net.codepoke.ai.challenges.hunterkiller.ui.MatchFrame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class HunterKillerVisualization
		extends MatchFrame<HunterKillerState, HunterKillerAction> {

	HunterKillerRenderer renderer;

	public HunterKillerVisualization() {
		super(new HunterKillerRules());
	}

	@Override
	public HunterKillerRenderer createRenderer(MatchFrame<HunterKillerState, HunterKillerAction> parent, Skin skin) {
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

	/**
	 * Visualises a layer of values on top of the rendering.
	 * 
	 * @param map
	 *            Two dimensional array of normalised values that will be used to overlay on top of the rendering.
	 * @param min
	 * @param med
	 * @param max
	 *            Color values which will be visualised using: value < 0.5 ? min.lerp(med, value / 0.5) :
	 *            med.lerp(max, (value - 0.5) / 0.5);
	 * @param ignore
	 *            Color value to give values that are not set (i.e. < 0).
	 * 
	 */
	public void visualise(float[][] map, Color min, Color med, Color max, Color ignore) {
		// Create the array of colors
		Color[][] valueMap = new Color[map.length][map[0].length];

		// Set the array of color values into the HunterKillerRenderer
		for (int x = 0; x < valueMap.length; x++) {
			for (int y = 0; y < valueMap[x].length; y++) {
				float value = map[x][y];
				// Note: copy the Color values before lerping, because that method adjusts the object itself.
				valueMap[x][y] = value > 0 ? value < 0.5 ? min.cpy()
																.lerp(med, value / 0.5f) : med.cpy()
																								.lerp(max, (value - 0.5f) / 0.5f) : ignore;
			}
		}

		// Set the value map into the renderer
		renderer.setValueMap(valueMap);
	}
}
