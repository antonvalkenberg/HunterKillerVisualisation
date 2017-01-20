package net.codepoke.ai.challenges.hunterkiller;

import static java.lang.Double.min;
import static java.lang.Math.abs;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import net.codepoke.ai.challenge.hunterkiller.Constants;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;
import net.codepoke.ai.challenge.hunterkiller.Map;
import net.codepoke.ai.challenge.hunterkiller.MapLocation;
import net.codepoke.ai.challenge.hunterkiller.enums.Direction;
import net.codepoke.ai.challenge.hunterkiller.enums.UnitOrderType;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.Controlled;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.GameObject;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Base;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Door;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Floor;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.MapFeature;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Space;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.mapfeature.Wall;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Infected;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Medic;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Soldier;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.unit.Unit;
import net.codepoke.ai.challenge.hunterkiller.orders.HunterKillerOrder;
import net.codepoke.ai.challenge.hunterkiller.orders.UnitOrder;
import net.codepoke.ai.challenges.hunterkiller.ui.MatchRenderer;
import net.codepoke.ai.challenges.hunterkiller.ui.MatchVisualization;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;

public class HunterKillerRenderer
		extends MatchRenderer<HunterKillerState, HunterKillerAction> {

	/**
	 * The original size of our tile-set images.
	 */
	public static final int TILE_SIZE_ORIGINAL = 24;
	/**
	 * The size at which the tiles will be displayed.
	 */
	public static int TILE_SIZE_DRAW = 24;
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
	 * Cache containing non-changing textures.
	 */
	private IntMap<TextureRegion> mapCache;

	/** Cache of Type to a list texture names (indexed on player ID) for Controlled */
	private ObjectMap<Class, String[]> controlledTextures;

	public HunterKillerRenderer(MatchVisualization<HunterKillerState, HunterKillerAction> parent, Skin skin) {
		super(parent, skin);
		defaultFont = skin.getFont("kenny-8outlined-font");
		controlledTextures = new ObjectMap<Class, String[]>();

		controlledTextures.put(Base.class, new String[4]);
		controlledTextures.put(Infected.class, new String[4]);
		controlledTextures.put(Medic.class, new String[4]);
		controlledTextures.put(Soldier.class, new String[4]);

		for (int i = 1; i <= 4; i++) {
			controlledTextures.get(Base.class)[i - 1] = "map/base_p" + i;
			controlledTextures.get(Infected.class)[i - 1] = "units/infected_p" + i;
			controlledTextures.get(Medic.class)[i - 1] = "units/medic_p" + i;
			controlledTextures.get(Soldier.class)[i - 1] = "units/soldier_p" + i;
		}
	}

	@Override
	public void onDraw(Batch batch, float parentAlpha) {

		if (state == null)
			return;

		// Initialize the cache
		if (mapCache == null) {
			createMapCache(state);
		}

		// WARNING: Null on initial state!
		HunterKillerAction action = getAction();

		float x = getX(), y = getY();

		// Make sure we are drawing at the selected scale
		TILE_SIZE_DRAW = (int) (TILE_SIZE_ORIGINAL * scale);

		// Create a new DrawHelper to assist with calculating the coordinates of where to draw things.
		DrawHelper dh = new DrawHelper(x, y);

		Map map = state.getMap();
		GameObject[][] objects = map.getMapContent();

		// Get a collection of the current player's combined field-of-view, we need this to make certain tiles shaded
		HashSet<MapLocation> fovSet = state.getPlayer(state.getCurrentPlayer())
											.getCombinedFieldOfView(map);

		// Go through the map to draw the map features first
		for (int xCoord = 0; xCoord < map.getMapWidth(); xCoord++) {
			for (int yCoord = 0; yCoord < map.getMapHeight(); yCoord++) {
				// Flip our Y-coordinate, since libGdx draws from bottom-left to top-right
				int flippedY = (map.getMapHeight() - 1) - yCoord;

				// Check if this location should be tinted
				boolean tinted = !fovSet.contains(new MapLocation(xCoord, flippedY));
				tinted &= !Gdx.input.isKeyPressed(Keys.ALT_LEFT);

				// Get the objects on this tile of the map
				int mapPosition = map.toPosition(xCoord, flippedY);
				GameObject[] tile = objects[mapPosition];
				
				// Save the original colors, so we can set them back later
				Color originalColor = batch.getColor();
				Color originalFontColor = defaultFont.getColor();

				// Change the color if we need to tint this location
				if (tinted) {
					// Check if space, them don't tint.
					if(!(tile[Constants.MAP_INTERNAL_FEATURE_INDEX] instanceof Space))					
						batch.setColor(Color.GRAY);
				}

				// Change the font's scale
				float ogFontScaleX = defaultFont.getData().scaleX;
				float ogFontScaleY = defaultFont.getData().scaleY;
				defaultFont.getData().scaleX *= scale;
				defaultFont.getData().scaleY *= scale;

				// Calculate all our drawing coordinates
				dh.calculateDrawCoordinates(xCoord, yCoord);

				// Draw MapFeatures first, since Units are drawn on top of those

				GameObject object = tile[Constants.MAP_INTERNAL_FEATURE_INDEX];

				// Check if this position has been cached
				if (mapCache.containsKey(mapPosition)) {
					batch.draw(mapCache.get(mapPosition), dh.drawX, dh.drawY, dh.tileWidth * dh.scaleX, dh.tileHeight * dh.scaleY);
				}

				else if (object instanceof Base) {
					// Draw a different color based on team
					Base base = (Base) object;
					String baseImg = getTextureLocation(base);
					batch.draw(skin.getRegion(baseImg), dh.drawX, dh.drawY, dh.tileWidth * dh.scaleX, dh.tileHeight * dh.scaleY);

					// Draw the player's resource amount
					int resource = state.getPlayer(base.getControllingPlayerID())
										.getResource();
					defaultFont.setColor(Color.CYAN);
					defaultFont.draw(batch, "" + resource, dh.drawXBaseHP, dh.drawYBaseHP);

					// Draw the base's health
					int health = base.getHpCurrent();
					defaultFont.setColor(Color.RED);
					defaultFont.draw(batch, "" + health, dh.drawXUnitHP, dh.drawYUnitHP);

				} else if (object instanceof Door) {
					// Get the positions around the Door
					MapFeature[] features = map.getMapFeaturesAround(map.toLocation(mapPosition));
					// If indexes 1 and 7 have a Wall, we'll need to rotate the Door 90 degrees
					float rotation = (features[1] instanceof Wall && features[7] instanceof Wall) ? 90 : 0;

					// Check for open/closed
					Door door = (Door) object;

					// If the door is open, we want to draw a Floor as background image, so draw it first
					if (door.isOpen()) {
						batch.draw(skin.getRegion("map/floor_1"), dh.drawX, dh.drawY, dh.tileWidth, dh.tileHeight);
					}

					// Draw the door
					batch.draw(	skin.getRegion(door.isOpen() ? "map/door_open" : "map/door_closed"),
								dh.drawX,
								dh.drawY,
								dh.originX,
								dh.originY,
								dh.tileWidth,
								dh.tileHeight,
								dh.scaleX,
								dh.scaleY,
								rotation);

					// If the door is open, we want to draw a timer to show when it closes, this should be on top
					if (door.isOpen()) {
						// Draw the open-time remaining
						int time = door.getOpenTimer();
						defaultFont.setColor(Color.CYAN);
						defaultFont.draw(batch, "" + time, dh.drawXBaseRes, dh.drawYBaseRes);
					}

				} else if (object instanceof Floor) {
					batch.draw(skin.getRegion("map/floor_1"), dh.drawX, dh.drawY, dh.tileWidth, dh.tileHeight);
				} else if (object instanceof Space) {
					batch.draw(skin.getRegion("map/space"), dh.drawX, dh.drawY, dh.tileWidth, dh.tileHeight);
				} else if (object instanceof Wall) {
					batch.draw(skin.getRegion("map/wall_single"), dh.drawX, dh.drawY, dh.tileWidth, dh.tileHeight);
				}

				// Restore the original settings
				batch.setColor(originalColor);
				defaultFont.setColor(originalFontColor);
				defaultFont.getData().scaleX = ogFontScaleX;
				defaultFont.getData().scaleY = ogFontScaleY;
			}
		}

		// Go through the map a second time to draw the units
		for (int xCoord = 0; xCoord < map.getMapWidth(); xCoord++) {
			for (int yCoord = map.getMapHeight(); yCoord --> 0 ;) {
				// Flip our Y-coordinate, since libGdx draws from bottom-left to top-right
				int flippedY = (map.getMapHeight() - 1) - yCoord;

				// Check if this location should be tinted
				boolean tinted = !fovSet.contains(new MapLocation(xCoord, flippedY));
				tinted &= !Gdx.input.isKeyPressed(Keys.ALT_LEFT);
				
				// Save the original colors, so we can set them back later
				Color originalColor = batch.getColor();
				Color originalFontColor = defaultFont.getColor();

				// Change the color if we need to tint this location
				if (tinted) {
					batch.setColor(Color.GRAY);
				}

				// Change the font's scale
				float ogFontScaleX = defaultFont.getData().scaleX;
				float ogFontScaleY = defaultFont.getData().scaleY;
				defaultFont.getData().scaleX *= scale;
				defaultFont.getData().scaleY *= scale;

				// Get the objects on this tile of the map
				int mapPosition = map.toPosition(xCoord, flippedY);
				GameObject[] tile = objects[mapPosition];

				// Calculate all our drawing coordinates
				dh.calculateDrawCoordinates(xCoord, yCoord);

				// Check if there is a unit on this location
				if (tile[Constants.MAP_INTERNAL_UNIT_INDEX] != null) {
					// Draw Units
					Unit unit = (Unit) tile[Constants.MAP_INTERNAL_UNIT_INDEX];

					// Get the rotation we need to give while drawing, note that a rotation of 0 is the same as the
					// sprite stands in the file (which is facing left, or WEST).
					float rotation = getUnitRotationAngle(unit.getOrientation());
					float unitScaleX = dh.scaleX;
					float unitScaleY = dh.scaleY;
					// However, if the rotation is 180, don't rotate, but flip the texture over the X-axis.
					if (rotation == 180) {
						// We don't rotate here, because we want to sprite to have it's feet on the bottom edge.
						rotation = 0;
						unitScaleX = -unitScaleX;
					}

					String unitImg = getTextureLocation(unit);
					batch.draw(skin.getRegion(unitImg), dh.drawX, dh.drawY + 4 * scale, // Raise the unit off the base
																						// of the tile slightly, to
																						// cause a 3D effect
								dh.originX,
								dh.originY,
								dh.tileWidth,
								dh.tileHeight,
								unitScaleX,
								unitScaleY,
								rotation);

					// Draw the unit's HP and cooldown
					int hp = unit.getHpCurrent();
					if(hp < unit.getHpMax()){
						defaultFont.setColor(Color.RED);
						defaultFont.draw(batch, "" + hp, dh.drawXUnitHP, dh.drawYUnitHP);
					}

					int cd = unit.getSpecialAttackCooldown();
					// Only draw the cooldown if it's on (>0)
					if (cd > 0) {
						defaultFont.setColor(Color.CYAN);
						defaultFont.draw(batch, "" + cd, dh.drawXUnitCD, dh.drawYUnitCD);
					}

					// Draw a Unit's ID if CTRL is pressed
					if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT)) {
						int unitID = unit.getID();
						defaultFont.setColor(Color.GREEN);
						defaultFont.draw(batch, "" + unitID, dh.drawXBaseHP, dh.drawYBaseHP);
					}
				}

				// Restore the original settings
				batch.setColor(originalColor);
				defaultFont.setColor(originalFontColor);
				defaultFont.getData().scaleX = ogFontScaleX;
				defaultFont.getData().scaleY = ogFontScaleY;
			}
		}

		// Check if we need to draw any actions
		if (action != null) {

			// Adjust the color of the batch, to draw with a lighter alpha
			Color originalColor = batch.getColor();
			batch.setColor(originalColor.r, originalColor.g, originalColor.b, 0.8f);

			for (HunterKillerOrder order : action.getOrders()) {
				// Only draw orders that were accepted
				if (!order.isAccepted())
					continue;

				if (order instanceof UnitOrder) {
					UnitOrder unitOrder = (UnitOrder) order;
					UnitOrderType type = unitOrder.getOrderType();
					MapLocation target = unitOrder.getTargetLocation();

					// We will need the order to have a target set, otherwise we can't draw anywhere
					if (target != null && map.isOnMap(target)) {

						// Flip our Y-coordinate, since libGdx draws from bottom-left to top-right
						int flippedY = (map.getMapHeight() - 1) - target.getY();
						// Calculate all our drawing coordinates
						dh.calculateDrawCoordinates(target.getX(), flippedY);

						// Only draw attack orders
						if (type == UnitOrderType.ATTACK) {
							// Check what type of unit the order was for
							switch (unitOrder.getUnitType()) {
							case Infected:
								batch.draw(skin.getRegion("fx/melee"), dh.drawX, dh.drawY, dh.tileWidth, dh.tileHeight);
								break;
							case Medic:
								// Fall through here, because we draw the Soldier's and Medic's basic attack the same
							case Soldier:
								batch.draw(skin.getRegion("fx/attack"), dh.drawX, dh.drawY, dh.tileWidth, dh.tileHeight);
								break;
							default:
								throw new RuntimeException("Unsupported UnitType found: " + unitOrder.getUnitType());
							}
						} else if (type == UnitOrderType.ATTACK_SPECIAL) {
							// Check what type of unit the order was for
							switch (unitOrder.getUnitType()) {
							case Infected:
								// Ignore, this special can't be ordered
								break;
							case Medic:
								batch.draw(skin.getRegion("fx/heal"), dh.drawX, dh.drawY, dh.tileWidth, dh.tileHeight);
								break;
							case Soldier:
								// Get the area of the Soldier's effect
								List<MapLocation> soldierAOE = map.getAreaAround(target, true);
								for (MapLocation loc : soldierAOE) {
									// Don't draw AOE on Walls
									if (map.getFeatureAtLocation(loc) instanceof Wall)
										continue;
									// Make a temporary draw-helper
									dh.calculateDrawCoordinates(loc.getX(), (map.getMapHeight() - 1) - loc.getY());
									batch.draw(skin.getRegion("fx/aoe"), dh.drawX, dh.drawY, dh.tileWidth, dh.tileHeight);
								}
								break;
							default:
								throw new RuntimeException("Unsupported UnitType found: " + unitOrder.getUnitType());
							}
						}
					}
				}
			}

			// Reset the correct color
			batch.setColor(originalColor);
		}
	}

	/**
	 * Returns the location and name of a texture for a given Controlled instance. (Based on class and controlling
	 * player)
	 */
	private String getTextureLocation(Controlled target) {
		return controlledTextures.get(target.getClass())[target.getControllingPlayerID()];
	}

	/**
	 * Returns the rotation needed to correctly render a Unit that is facing this direction. Because the sprites in the
	 * texture files we are using are facing left (or WEST), that direction is considered to have a rotational angle of
	 * 0 degrees. This angle increases counter-clockwise, as defined by
	 * {@link Batch#draw(TextureRegion, float, float, float, float, float, float, float, float, float)}.
	 */
	public float getUnitRotationAngle(Direction direction) {
		switch (direction) {
		case WEST:
			// This is defined as being 0 degrees, since the texture files (the sprites we use for representing
			// Units) are facing left/WEST.
			return 0;
		case SOUTH:
			// The rotation that is used measures counter-clockwise, which means bottom/SOUTH is at 90 degrees.
			return 0;
		case EAST:
			return 180;
		case NORTH:
			return 0;
		default:
			throw new RuntimeException("Unsupported Direction value " + direction);
		}
	}

	/**
	 * Create a cache of {@link TextureRegion}s, indexed by their position on the {@link Map}. This method currently
	 * caches the following MapFeature-objects: Wall, Floor, Space.
	 * 
	 * @param orgState
	 *            The initial state of the game.
	 */
	public void createMapCache(HunterKillerState orgState) {
		// Create a new cache
		mapCache = new IntMap<TextureRegion>();

		Map map = orgState.getMap();
		// Get the map content we are trying to cache
		GameObject[][] content = map.getMapContent();

		Random r = new Random(4);

		// Traverse the content of the map
		for (int position = 0; position < content.length; position++) {

			// Create a weight
			double weight = abs(min(r.nextGaussian(), 2));

			// We are only caching MapFeatures, because they change so infrequently
			MapFeature feature = (MapFeature) content[position][Constants.MAP_INTERNAL_FEATURE_INDEX];
			// Check if the feature is a Wall
			if (feature instanceof Wall) {
				// Get the features around the wall
				// We identify these as 9 locations (our wall in the middle), starting top-left and going right->down,
				// starting from index 0, to 8.
				MapFeature[] features = map.getMapFeaturesAround(map.toLocation(position));

				// Make a list of indexes that are Walls
				IntArray wI = new IntArray();
				for (int i = 0; i < features.length; i++) {
					// Check for null value, also: treat Doors as Walls here, since that makes the cleanest look
					if (features[i] != null && (features[i] instanceof Wall || features[i] instanceof Door))
						wI.add(i);
				}

				// There are several situations we are looking for, because this piece of Wall looks different in all:
				// Note: never need to check index 4, this is already our centre Wall.

				// 16: T-section pointing to the right; 1,4,5,7 are Walls, 3 is non-Wall.
				if (wI.contains(1) && wI.contains(5) && wI.contains(7) && !wI.contains(3)) {
					mapCache.put(position, skin.getRegion("map/wall_16"));
					continue;
				}
				// 15: T-section pointing to the left; 1,3,4,7 are Walls, 5 is non-Wall.
				else if (wI.contains(1) && wI.contains(3) && wI.contains(7) && !wI.contains(5)) {
					mapCache.put(position, skin.getRegion("map/wall_15"));
					continue;
				}
				// 14: T-section pointing to the top; 1,3,4,5 are Walls, 7 is non-Wall.
				else if (wI.contains(1) && wI.contains(3) && wI.contains(5) && !wI.contains(7)) {
					mapCache.put(position, skin.getRegion("map/wall_14"));
					continue;
				}
				// 13: T-section pointing to the bottom; 3,4,5,7 are Walls, 1 is non-Wall.
				else if (wI.contains(3) && wI.contains(5) && wI.contains(7) && !wI.contains(1)) {
					mapCache.put(position, skin.getRegion("map/wall_13"));
					continue;
				}
				// 12: Top end of a wall section; 4,7 are Walls, 1,3,5 are non-Wall.
				else if (wI.contains(7) && !wI.contains(1) && !wI.contains(3) && !wI.contains(5)) {
					// mapCache.put(position, skin.getRegion("map/wall_12"));
					mapCache.put(position, skin.getRegion("map/wall_09"));
					continue;
				}
				// 11: Left end of a wall section; 4,5 are Walls, 1,3,7 are non-Wall
				else if (wI.contains(5) && !wI.contains(1) && !wI.contains(3) && !wI.contains(7)) {
					// mapCache.put(position, skin.getRegion("map/wall_11"));
					mapCache.put(position, skin.getRegion("map/wall_10"));
					continue;
				}
				// 10: Right end of a wall section; 3,4 are Walls, 1,5,7 are non-Wall
				else if (wI.contains(3) && !wI.contains(1) && !wI.contains(5) && !wI.contains(7)) {
					// mapCache.put(position, skin.getRegion("map/wall_10"));
					mapCache.put(position, skin.getRegion("map/wall_11"));
					continue;
				}
				// 09: Bottom end of a wall section; 1,4 are Walls, 3,5,7 are non-Wall
				else if (wI.contains(1) && !wI.contains(3) && !wI.contains(5) && !wI.contains(7)) {
					// mapCache.put(position, skin.getRegion("map/wall_09"));
					mapCache.put(position, skin.getRegion("map/wall_12"));
					continue;
				}
				// 08: A cross; 1,3,4,5,7 are Walls
				else if (wI.contains(1) && wI.contains(3) && wI.contains(5) && wI.contains(7)) {
					mapCache.put(position, skin.getRegion("map/wall_08"));
					continue;
				}
				// 07: Vertically oriented wall; 1,4,7 are Walls, 3,5 are non-Walls
				else if (wI.contains(1) && wI.contains(7) && !wI.contains(3) && !wI.contains(5)) {
					// Grab one of the alternative tiles with some distribution
					mapCache.put(position, skin.getRegion(weight < 1.2 ? "map/wall_07" : "map/wall_07b"));

					continue;
				}
				// 06: Horizontally oriented wall; 3,4,5 are Walls, 1,7 are non-Walls
				else if (wI.contains(3) && wI.contains(5) && !wI.contains(1) && !wI.contains(7)) {
					mapCache.put(position, skin.getRegion(weight < 1.2 ? "map/wall_06" : "map/wall_06b"));
					continue;
				}
				// 05: Top-right corner; 3,4,7 are Walls, 1,5 are non-Walls
				else if (wI.contains(3) && wI.contains(7) && !wI.contains(1) && !wI.contains(5)) {
					mapCache.put(position, skin.getRegion("map/wall_05"));
					continue;
				}
				// 04: Bottom-left corner; 1,4,5 are Walls, 3,7 are non-Walls
				else if (wI.contains(1) && wI.contains(5) && !wI.contains(3) && !wI.contains(7)) {
					mapCache.put(position, skin.getRegion("map/wall_04"));
					continue;
				}
				// 03: Bottom-right corner; 1,3,4 are Walls, 5,7 are non-Walls
				else if (wI.contains(1) && wI.contains(3) && !wI.contains(5) && !wI.contains(7)) {
					mapCache.put(position, skin.getRegion("map/wall_03"));
					continue;
				}
				// 02: Top-left corner; 4,5,7 are Walls, 1,3 are non-Walls
				else if (wI.contains(5) && wI.contains(7) && !wI.contains(1) && !wI.contains(3)) {
					mapCache.put(position, skin.getRegion("map/wall_02"));
					continue;
				}
				// 01: Pillar (any other situation);
				else {
					mapCache.put(position, skin.getRegion("map/wall_01"));
					continue;
				}
			} else if (feature instanceof Floor) {
				// Grab one of the alternative tiles with some distribution
				if (weight < 1.2) {
					mapCache.put(position, skin.getRegion("map/floor_1"));
				} else if (weight < 1.4) {
					mapCache.put(position, skin.getRegion("map/floor_2"));
				} else if (weight < 1.8) {
					mapCache.put(position, skin.getRegion("map/floor_3"));
				} else {
					mapCache.put(position, skin.getRegion("map/floor_4"));
				}
			} else if (feature instanceof Space) {
				mapCache.put(position, skin.getRegion("map/space"));
			}
		}
	}

	@Override
	public float getPrefWidth() {
		return (state != null ? state.getMap()
										.getMapWidth() * TILE_SIZE_ORIGINAL * scale : 0);
	}

	@Override
	public float getPrefHeight() {
		return (state != null ? state.getMap()
										.getMapHeight() * TILE_SIZE_ORIGINAL * scale : 0);
	}

	/**
	 * A helper class to hold several coordinates, based on the coordinates of where libgdx starts to draw the map.
	 * 
	 * @author Anton Valkenberg (anton.valkenberg@gmail.com)
	 *
	 */
	private class DrawHelper {

		// region Properties

		/** The X/Y-coordinate of where libgdx starts drawing the map. */
		private float x = 0, y = 0;

		/** The size of the tiles as drawn on the screen. */
		private float drawnTileSize = TILE_SIZE_DRAW;

		/** The scale libgdx should apply when rotating or transforming. */
		private float scale = SCALE;

		/** The amount of pixels of room we want to use between the start of a tile and the start of any text. */
		private int textOffset = TEXT_OFFSET_PIXELS;

		/** The X-coordinate of where libgdx will start drawing the texture. */
		public float drawX;

		/** The Y-coordinate of where libgdx will start drawing the texture. */
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
