package net.codepoke.ai.challenges.hunterkiller;

import java.util.HashSet;

import net.codepoke.ai.challenge.hunterkiller.Constants;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;
import net.codepoke.ai.challenge.hunterkiller.Map;
import net.codepoke.ai.challenge.hunterkiller.MapLocation;
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
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class HunterKillerRenderer
		extends MatchRenderer<HunterKillerState, HunterKillerAction> {

	/**
	 * The original size of our tile-set images.
	 */
	public static final int TILE_SIZE_ORIGINAL = 24;
	/**
	 * The size at which the tiles will be displayed.
	 */
	public static final int TILE_SIZE_DRAW = 48;
	/**
	 * The scale that libgdx should apply when rotating or transforming.
	 */
	private static final float SCALE = 1f;
	/**
	 * The offset that we want to use when drawing text on tiles.
	 */
	private static final int TEXT_OFFSET_PIXELS = 2;
	/**
	 * The default font
	 */
	private BitmapFont defaultFont;
	/**
	 * A smaller font than the default font
	 */
	private BitmapFont smallFont;

	public HunterKillerRenderer(MatchVisualization<HunterKillerState, HunterKillerAction> parent, Skin skin) {
		super(parent, skin);
		defaultFont = skin.getFont("default-font");
		smallFont = skin.getFont("kenny-8-font");
	}

	@Override
	public void onDraw(Batch batch, float parentAlpha) {

		if (state == null)
			return;

		float x = getX(), y = getY();

		// Create a new DrawHelper to assist with calculating the coordinates of where to draw things.
		DrawHelper dh = new DrawHelper(x, y);

		Map map = state.getMap();
		GameObject[][] objects = map.getMapContent();

		// Get a collection of the current player's combined field-of-view, we need this to make certain tiles shaded
		HashSet<MapLocation> fovSet = state.getPlayer(state.getCurrentPlayer())
											.getCombinedFieldOfView(map);

		// Go through the map
		for (int xCoord = 0; xCoord < map.getMapWidth(); xCoord++) {
			for (int yCoord = 0; yCoord < map.getMapHeight(); yCoord++) {
				// Flip our Y-coordinate, since libGdx draws from bottom-left to top-right
				int flippedY = (map.getMapHeight() - 1) - yCoord;

				// Check if this location should be tinted
				boolean tinted = !fovSet.contains(new MapLocation(xCoord, flippedY));
				// Save the original colors, so we can set them back later
				Color originalColor = batch.getColor();
				Color originalDefaultFontColor = defaultFont.getColor();
				Color originalSmallFontColor = smallFont.getColor();

				// Change the color if we need to tint this location
				if (tinted) {
					batch.setColor(Color.GRAY);
				}

				// Get the objects on this tile of the map
				GameObject[] tile = objects[map.toPosition(xCoord, flippedY)];

				// Calculate all our drawing coordinates
				dh.calculateDrawCoordinates(xCoord, yCoord);

				// Handle the two levels of the Map, MapFeatures first, since Units are drawn on top of those
				if (tile[Constants.MAP_INTERNAL_FEATURE_INDEX] != null) {
					// Draw MapFeatures
					GameObject object = tile[Constants.MAP_INTERNAL_FEATURE_INDEX];
					//@formatter:off
					if (object instanceof Base) {
						//Draw a different color based on team
						Base base = (Base)object;
						String baseImg = null;
						switch(base.getControllingPlayerID()) {
							case 0: baseImg = "map/base_p1"; break;
							case 1: baseImg = "map/base_p2"; break;
							case 2: baseImg = "map/base_p3"; break;
							case 3:
							default: baseImg = "map/base_p4"; break;
						}
						batch.draw(skin.getRegion(baseImg), dh.drawX, dh.drawY, dh.tileWidth, dh.tileHeight);
						
						//Draw the player's resource amount
						int resource = state.getPlayer(base.getControllingPlayerID()).getResource();
						defaultFont.setColor(Color.BLUE);
						defaultFont.draw(batch, "" +  resource, dh.drawXBaseRes, dh.drawYBaseRes);
						
						//Draw the base's health
						int health = base.getHpCurrent();
						defaultFont.setColor(Color.RED);
						defaultFont.draw(batch, "" +  health, dh.drawXBaseHP, dh.drawYBaseHP);
						
					} else if (object instanceof Door) {
						// Check for open/closed
						Door door = (Door)object;
						batch.draw(skin.getRegion(door.isOpen() ? "map/door_open" : "map/door_closed"),
						           dh.drawX, dh.drawY, dh.tileWidth, dh.tileHeight);
						//Draw the open-time remaining, if larger than 0
						if (door.getOpenTimer() > 0) {
							int time = door.getOpenTimer();
							defaultFont.setColor(Color.BLUE);
							defaultFont.draw(batch, "" +  time, dh.drawXBaseRes, dh.drawYBaseRes);
						}
					} else if (object instanceof Floor) {
						batch.draw(skin.getRegion("map/floor"), dh.drawX, dh.drawY, dh.tileWidth, dh.tileHeight);
					} else if (object instanceof Space) {
						batch.draw(skin.getRegion("map/space"), dh.drawX, dh.drawY, dh.tileWidth, dh.tileHeight);
					} else if (object instanceof Wall) {
						batch.draw(skin.getRegion("map/wall_single"), dh.drawX, dh.drawY, dh.tileWidth, dh.tileHeight);
					}
					//@formatter:on
				} else {
					// This is a problem, there should always be a MapFeature on a tile
				}
				if (tile[Constants.MAP_INTERNAL_UNIT_INDEX] != null) {
					// Draw Units
					Unit unit = (Unit) tile[Constants.MAP_INTERNAL_UNIT_INDEX];

					// Get the rotation we need to give while drawing, note that a rotation of 0 is the same as the
					// sprite stands in the file (which is facing left, or WEST).
					// These angles are defined in Direction through .getLibgdxRotationAngle()
					float rotation = unit.getOrientation()
											.getLibgdxRotationAngle();

					//@formatter:off
					if (unit instanceof Infected) {
						Infected infected = (Infected)unit;
						String infectedImg = null;
						switch(infected.getControllingPlayerID()) {
							case 0: infectedImg = "units/infected_p1"; break;
							case 1: infectedImg = "units/infected_p2"; break;
							case 2: infectedImg = "units/infected_p3"; break;
							case 3:
							default: infectedImg = "units/infected_p4"; break;
						}
						batch.draw(skin.getRegion(infectedImg), dh.drawX, dh.drawY, dh.originX, dh.originY, dh.tileWidth, dh.tileHeight, dh.scaleX, dh.scaleY, rotation);
					} else if (unit instanceof Medic) {
						//Draw a different color based on team
						Medic medic = (Medic)unit;
						String medicImg = null;
						switch(medic.getControllingPlayerID()) {
							case 0: medicImg = "units/medic_p1"; break;
							case 1: medicImg = "units/medic_p2"; break;
							case 2: medicImg = "units/medic_p3"; break;
							case 3:
							default: medicImg = "units/medic_p4"; break;
						}
						batch.draw(skin.getRegion(medicImg), dh.drawX, dh.drawY, dh.originX, dh.originY, dh.tileWidth, dh.tileHeight, dh.scaleX, dh.scaleY, rotation);
					} else if (unit instanceof Soldier) {
						//Draw a different color based on team
						Soldier soldier = (Soldier)unit;
						String soldierImg = null;
						switch(soldier.getControllingPlayerID()) {
							case 0: soldierImg = "units/soldier_p1"; break;
							case 1: soldierImg = "units/soldier_p2"; break;
							case 2: soldierImg = "units/soldier_p3"; break;
							case 3:
							default: soldierImg = "units/soldier_p4"; break;
						}
						batch.draw(skin.getRegion(soldierImg), dh.drawX, dh.drawY, dh.originX, dh.originY, dh.tileWidth, dh.tileHeight, dh.scaleX, dh.scaleY, rotation);
					}
					
					//Draw the unit's HP and cooldown
					int hp = unit.getHpCurrent();
					defaultFont.setColor(Color.RED);
					defaultFont.draw(batch, "" + hp, dh.drawXUnitHP, dh.drawYUnitHP);
					int cd = unit.getSpecialAttackCooldown();
					defaultFont.setColor(Color.BLUE);
					defaultFont.draw(batch, "" + cd, dh.drawXUnitCD, dh.drawYUnitCD);
					
					//@formatter:on
				}

				// Restore the original colors
				batch.setColor(originalColor);
				defaultFont.setColor(originalDefaultFontColor);
				smallFont.setColor(originalSmallFontColor);
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

	/**
	 * A helper class to hold several coordinates, based on the coordinates of where libgdx starts to draw the map.
	 * 
	 * @author Anton Valkenberg (anton.valkenberg@gmail.com)
	 *
	 */
	private class DrawHelper {

		// region Properties

		/**
		 * The X-coordinate of where libgdx starts drawing the map.
		 */
		private float x = 0;
		/**
		 * The X-coordinate of where libgdx starts drawing the map.
		 */
		private float y = 0;
		/**
		 * The size of the tiles as drawn on the screen.
		 */
		private float drawnTileSize = TILE_SIZE_DRAW;
		/**
		 * The scale libgdx should apply when rotating or transforming.
		 */
		private float scale = SCALE;
		/**
		 * The amount of pixels of room we want to use between the start of a tile and the start of any text.
		 */
		private int textOffset = TEXT_OFFSET_PIXELS;
		/**
		 * The X-coordinate of where libgdx will start drawing the texture.
		 */
		public float drawX;
		/**
		 * The Y-coordinate of where libgdx will start drawing the texture.
		 */
		public float drawY;
		/**
		 * The X-coordinate of the point around which libgdx should rotate the texture.
		 */
		public float originX;
		/**
		 * The Y-coordinate of the point around which libgdx should rotate the texture.
		 */
		public float originY;
		/**
		 * The scale that libgdx should apply on the X-axis.
		 */
		public float scaleX;
		/**
		 * The scale that libgdx should apply on the Y-axis.
		 */
		public float scaleY;
		/**
		 * The width of the tile (in pixels), that the texture represents.
		 */
		public float tileWidth;
		/**
		 * The height of the tile (in pixels), that the texture represents.
		 */
		public float tileHeight;
		/**
		 * The X-coordinate from where libgdx should start drawing the Unit-HP text.
		 */
		public float drawXUnitHP;
		/**
		 * The Y-coordinate from where libgdx should start drawing the Unit-HP text.
		 */
		public float drawYUnitHP;
		/**
		 * The X-coordinate from where libgdx should start drawing the Unit-Cooldown text.
		 */
		public float drawXUnitCD;
		/**
		 * The Y-coordinate from where libgdx should start drawing the Unit-Cooldown text.
		 */
		public float drawYUnitCD;
		/**
		 * The X-coordinate from where libgdx should start drawing the Base-HP text.
		 */
		public float drawXBaseHP;
		/**
		 * The Y-coordinate from where libgdx should start drawing the Base-HP text.
		 */
		public float drawYBaseHP;
		/**
		 * The X-coordinate from where libgdx should start drawing the Base-Resource text.
		 */
		public float drawXBaseRes;
		/**
		 * The Y-coordinate from where libgdx should start drawing the Base-Resource text.
		 */
		public float drawYBaseRes;

		// endregion

		// region Constructor

		/**
		 * Creates a new instance.
		 * 
		 * @param libgdxX
		 *            The X-coordinate of where libgdx starts drawing the map.
		 * 
		 * @param libgdxY
		 *            The Y-coordinate of where libgdx starts drawing the map.
		 * 
		 */
		public DrawHelper(float libgdxX, float libgdxY) {
			x = libgdxX;
			y = libgdxY;
		}

		// endregion

		// region Public methods

		/**
		 * Calculates the coordinates of where we want to draw things.
		 * 
		 * @param xMap
		 *            The X-coordinate on the game {@link Map} of the object that is being drawn.
		 * @param yMap
		 *            The Y-coordinate on the game {@link Map} of the object that is being drawn.
		 */
		public void calculateDrawCoordinates(int xMap, int yMap) {
			drawX = xMap * drawnTileSize + x;
			drawY = yMap * drawnTileSize + y;
			originX = drawnTileSize / 2f;
			originY = drawnTileSize / 2f;
			scaleX = scale;
			scaleY = scale;
			tileWidth = drawnTileSize;
			tileHeight = drawnTileSize;
			drawXUnitHP = drawX + textOffset;
			drawYUnitHP = drawY + (tileHeight / 3);
			drawXUnitCD = drawX + (2 * tileWidth / 3);
			drawYUnitCD = drawYUnitHP;
			drawXBaseHP = drawXUnitHP;
			drawYBaseHP = drawY + tileHeight - textOffset;
			drawXBaseRes = drawXUnitCD;
			drawYBaseRes = drawYBaseHP;
		}

		// endregion

	}
}
