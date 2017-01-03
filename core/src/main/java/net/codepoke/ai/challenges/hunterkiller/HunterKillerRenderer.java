package net.codepoke.ai.challenges.hunterkiller;

import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;
import net.codepoke.ai.challenge.hunterkiller.Map;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.GameObject;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Base;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Door;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Floor;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Space;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Wall;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Infected;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Medic;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Soldier;
import net.codepoke.ai.challenges.hunterkiller.ui.MatchRenderer;
import net.codepoke.ai.challenges.hunterkiller.ui.MatchVisualization;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class HunterKillerRenderer
		extends MatchRenderer<HunterKillerState, HunterKillerAction> {

	// The size at which the tiles will be displayed. (our tileset images are 24px
	public static int TILE_SIZE = 24;

	public HunterKillerRenderer(MatchVisualization<HunterKillerState, HunterKillerAction> parent, Skin skin) {
		super(parent, skin);
	}

	@Override
	public void onDraw(Batch batch, float parentAlpha) {

		if (state == null)
			return;

		float x = getX(), y = getY();

		Map map = state.getMap();
		GameObject[][] objects = map.getMapContent();
		// int[] spawnLocations = board.getSpawnLocations();

		// TODO Get a collection of the current player's combined field-of-view, we need this to make certain tiles
		// shaded
		// Like so:
		// Color clr = batch.getColor();
		// if (bombMan.isInvulnerable()) batch.setColor(Color.GRAY);
		// batch.setColor(clr);

		for (int i = 0; i < map.getMapWidth(); i++) {
			for (int j = 0; j < map.getMapHeight(); j++) {
				int idx = map.toPosition(i, j);
				GameObject[] tile = objects[idx];

				// Handle the two levels of the Map
				if (tile[Map.INTERNAL_MAP_FEATURE_INDEX] != null) {
					// Draw MapFeatures
					GameObject object = tile[Map.INTERNAL_MAP_FEATURE_INDEX];
					//@formatter:off
					if (object instanceof Base) {

					} else if (object instanceof Door) {
						// Check for open/closed
						Door door = (Door) object;
						batch.draw(skin.getRegion(door.isOpen() ? "" : ""),
									i * TILE_SIZE + x,
									j * TILE_SIZE + y,
									TILE_SIZE,
									TILE_SIZE);
					} else if (object instanceof Floor) {

					} else if (object instanceof Space) {

					} else if (object instanceof Wall) {

					}
					//@formatter:on
				} else {
					// This is a problem, there should always be a MapFeature on a tile
				}
				if (tile[Map.INTERNAL_MAP_UNIT_INDEX] != null) {
					// Draw Units
					GameObject object = tile[Map.INTERNAL_MAP_UNIT_INDEX];
					//@formatter:off
					if (object instanceof Infected) {

					} else if (object instanceof Medic) {

					} else if (object instanceof Soldier) {

					}
					//@formatter:on
				}
			}
		}
	}

	@Override
	public float getPrefWidth() {
		return (state != null ? state.getMap()
										.getMapWidth() * TILE_SIZE : 0);
	}

	@Override
	public float getPrefHeight() {
		return (state != null ? state.getMap()
										.getMapHeight() * TILE_SIZE : 0);
	}

}
