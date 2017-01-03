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

		int currentPlayer = state.getCurrentPlayer();
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

				// Handle the two levels of the Map, MapFeatures first, since Units are drawn on top of those
				if (tile[Map.INTERNAL_MAP_FEATURE_INDEX] != null) {
					// Draw MapFeatures
					GameObject object = tile[Map.INTERNAL_MAP_FEATURE_INDEX];
					//@formatter:off
					if (object instanceof Base) {
						//Draw a different color based on team
						Base base = (Base)object;
						String baseImg = null;
						switch(base.getPlayerID()) {
							case 0: baseImg = "map/base_p1"; break;
							case 1: baseImg = "map/base_p2"; break;
							case 2: baseImg = "map/base_p3"; break;
							case 3:
							default: baseImg = "map/base_p4"; break;
						}
						batch.draw(skin.getRegion(baseImg), i * TILE_SIZE + x, j * TILE_SIZE + y, TILE_SIZE, TILE_SIZE);
					} else if (object instanceof Door) {
						// Check for open/closed
						Door door = (Door)object;
						batch.draw(skin.getRegion(door.isOpen() ? "map/door_open" : "map/door_closed"),
									i * TILE_SIZE + x,
									j * TILE_SIZE + y,
									TILE_SIZE,
									TILE_SIZE);
					} else if (object instanceof Floor) {
						batch.draw(skin.getRegion("map/foor"), i * TILE_SIZE + x, j * TILE_SIZE + y, TILE_SIZE, TILE_SIZE);
					} else if (object instanceof Space) {
						batch.draw(skin.getRegion("map/space"), i * TILE_SIZE + x, j * TILE_SIZE + y, TILE_SIZE, TILE_SIZE);
					} else if (object instanceof Wall) {
						batch.draw(skin.getRegion("map/wall_single"), i * TILE_SIZE + x, j * TILE_SIZE + y, TILE_SIZE, TILE_SIZE);
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
						Infected infected = (Infected)object;
						String infectedImg = null;
						switch(infected.getSquadPlayerID()) {
							case 0: infectedImg = "units/infected_p1"; break;
							case 1: infectedImg = "units/infected_p2"; break;
							case 2: infectedImg = "units/infected_p3"; break;
							case 3:
							default: infectedImg = "units/infected_p4"; break;
						}
						batch.draw(skin.getRegion(infectedImg), i * TILE_SIZE + x, j * TILE_SIZE + y, TILE_SIZE, TILE_SIZE);
					} else if (object instanceof Medic) {
						//Draw a different color based on team
						Medic medic = (Medic)object;
						String medicImg = null;
						switch(medic.getSquadPlayerID()) {
							case 0: medicImg = "units/medic_p1"; break;
							case 1: medicImg = "units/medic_p2"; break;
							case 2: medicImg = "units/medic_p3"; break;
							case 3:
							default: medicImg = "units/medic_p4"; break;
						}
						batch.draw(skin.getRegion(medicImg), i * TILE_SIZE + x, j * TILE_SIZE + y, TILE_SIZE, TILE_SIZE);
					} else if (object instanceof Soldier) {
						//Draw a different color based on team
						Soldier soldier = (Soldier)object;
						String soldierImg = null;
						switch(soldier.getSquadPlayerID()) {
							case 0: soldierImg = "units/soldier_p1"; break;
							case 1: soldierImg = "units/soldier_p2"; break;
							case 2: soldierImg = "units/soldier_p3"; break;
							case 3:
							default: soldierImg = "units/soldier_p4"; break;
						}
						batch.draw(skin.getRegion(soldierImg), i * TILE_SIZE + x, j * TILE_SIZE + y, TILE_SIZE, TILE_SIZE);
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
