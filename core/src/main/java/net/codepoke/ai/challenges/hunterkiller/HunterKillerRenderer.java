package net.codepoke.ai.challenges.hunterkiller;

import java.util.HashSet;

import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;
import net.codepoke.ai.challenge.hunterkiller.Map;
import net.codepoke.ai.challenge.hunterkiller.MapLocation;
import net.codepoke.ai.challenge.hunterkiller.enums.Direction;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.GameObject;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Base;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Door;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Floor;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Space;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Wall;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Infected;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Medic;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Soldier;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Unit;
import net.codepoke.ai.challenges.hunterkiller.ui.MatchRenderer;
import net.codepoke.ai.challenges.hunterkiller.ui.MatchVisualization;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class HunterKillerRenderer
		extends MatchRenderer<HunterKillerState, HunterKillerAction> {

	// The size at which the tiles will be displayed.
	public static final int TILE_SIZE_DRAW = 36;
	// The original size of our tile-set images.
	public static final int TILE_SIZE_ORIGINAL = 24;

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

		// Get a collection of the current player's combined field-of-view, we need this to make certain tiles shaded
		HashSet<MapLocation> fovSet = state.getPlayer(state.getCurrentPlayer())
											.getCombinedFieldOfView(map);

		// TODO translate coordinates? cause I'm using different orientation.

		// Go through the map
		for (int i = 0; i < map.getMapWidth(); i++) {
			for (int j = 0; j < map.getMapHeight(); j++) {
				// Check if this location should be tinted
				boolean tinted = !fovSet.contains(new MapLocation(i, j));
				// Save the original color, so we can set it back to this later
				Color originalColor = batch.getColor();
				// Change the color if we need to tint this location
				if (tinted) {
					batch.setColor(Color.GRAY);
				}

				// Get the objects on this tile of the map
				GameObject[] tile = objects[map.toPosition(i, j)];

				// Define some draw locations
				float drawX = i * TILE_SIZE_DRAW + x;
				float drawY = j * TILE_SIZE_DRAW + y;
				float originX = TILE_SIZE_DRAW / 2f;
				float originY = TILE_SIZE_DRAW / 2f;
				float scaleX = 1;
				float scaleY = 1;
				float tileWidth = TILE_SIZE_DRAW;
				float tileHeight = TILE_SIZE_DRAW;

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
						batch.draw(skin.getRegion(baseImg), drawX, drawY, tileWidth, tileHeight);
					} else if (object instanceof Door) {
						// Check for open/closed
						Door door = (Door)object;
						batch.draw(skin.getRegion(door.isOpen() ? "map/door_open" : "map/door_closed"),
						           drawX, drawY, tileWidth, tileHeight);
					} else if (object instanceof Floor) {
						batch.draw(skin.getRegion("map/floor"), drawX, drawY, tileWidth, tileHeight);
					} else if (object instanceof Space) {
						batch.draw(skin.getRegion("map/space"), drawX, drawY, tileWidth, tileHeight);
					} else if (object instanceof Wall) {
						batch.draw(skin.getRegion("map/wall_single"), drawX, drawY, tileWidth, tileHeight);
					}
					//@formatter:on
				} else {
					// This is a problem, there should always be a MapFeature on a tile
				}
				if (tile[Map.INTERNAL_MAP_UNIT_INDEX] != null) {
					// Draw Units
					Unit unit = (Unit) tile[Map.INTERNAL_MAP_UNIT_INDEX];
					// Determine the rotation we need to apply
					Direction unitOrientation = unit.getOrientation();
					float rotation = Math.abs(unitOrientation.angle - 180);

					//@formatter:off
					if (unit instanceof Infected) {
						Infected infected = (Infected)unit;
						String infectedImg = null;
						switch(infected.getSquadPlayerID()) {
							case 0: infectedImg = "units/infected_p1"; break;
							case 1: infectedImg = "units/infected_p2"; break;
							case 2: infectedImg = "units/infected_p3"; break;
							case 3:
							default: infectedImg = "units/infected_p4"; break;
						}
						batch.draw(skin.getRegion(infectedImg), drawX, drawY, originX, originY, tileWidth, tileHeight, scaleX, scaleY, rotation);
					} else if (unit instanceof Medic) {
						//Draw a different color based on team
						Medic medic = (Medic)unit;
						String medicImg = null;
						switch(medic.getSquadPlayerID()) {
							case 0: medicImg = "units/medic_p1"; break;
							case 1: medicImg = "units/medic_p2"; break;
							case 2: medicImg = "units/medic_p3"; break;
							case 3:
							default: medicImg = "units/medic_p4"; break;
						}
						batch.draw(skin.getRegion(medicImg), drawX, drawY, originX, originY, tileWidth, tileHeight, scaleX, scaleY, rotation);
					} else if (unit instanceof Soldier) {
						//Draw a different color based on team
						Soldier soldier = (Soldier)unit;
						String soldierImg = null;
						switch(soldier.getSquadPlayerID()) {
							case 0: soldierImg = "units/soldier_p1"; break;
							case 1: soldierImg = "units/soldier_p2"; break;
							case 2: soldierImg = "units/soldier_p3"; break;
							case 3:
							default: soldierImg = "units/soldier_p4"; break;
						}
						batch.draw(skin.getRegion(soldierImg), drawX, drawY, originX, originY, tileWidth, tileHeight, scaleX, scaleY, rotation);
					}
					//@formatter:on
				}

				// Restore the original color
				batch.setColor(originalColor);
			}
		}
	}

	@Override
	public float getPrefWidth() {
		return (state != null ? state.getMap()
										.getMapWidth() * TILE_SIZE_DRAW : 0);
	}

	@Override
	public float getPrefHeight() {
		return (state != null ? state.getMap()
										.getMapHeight() * TILE_SIZE_DRAW : 0);
	}

}
