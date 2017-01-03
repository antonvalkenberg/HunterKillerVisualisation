package net.codepoke.ai.challenges.hunterkiller;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;

import net.codepoke.ai.challenge.hunterkiller.Board;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction.Move;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.Bomb;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.BombMan;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.Coin;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.Fire;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.GameObject;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.Item;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.Item.UpgradeType;
import net.codepoke.ai.challenge.hunterkiller.gameobjects.Wall;
import net.codepoke.ai.challenges.hunterkiller.ui.MatchRenderer;
import net.codepoke.ai.challenges.hunterkiller.ui.MatchVisualization;

public class HunterKillerRenderer extends MatchRenderer<HunterKillerState, HunterKillerAction> {

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
		
		Board board = state.getBoard();
		Array<GameObject>[] objects = board.getObjects();
		int[] spawnLocations = board.getSpawnLocations();

		for (int i = 0; i < board.getWidth(); i++) {
			for (int j = 0; j < board.getHeight(); j++) {
				int idx = board.toIndex(i, j);
				Array<GameObject> tile = objects[idx];

				// Draw the floor and spawn
				batch.draw(skin.getRegion("game/floor"), i * TILE_SIZE + x, j * TILE_SIZE + y, TILE_SIZE, TILE_SIZE);
				
				for (int l = 0; l < spawnLocations.length; l++) {
					if(spawnLocations[l] == idx) {
						batch.draw(skin.getRegion("ingame/spawn_" + (l + 1)), i * TILE_SIZE + x, j * TILE_SIZE + y, TILE_SIZE, TILE_SIZE);
					}
				}

				for (int k = 0; k < tile.size; k++) {
					GameObject gameObject = tile.get(k);

					//@formatter:off
					if (gameObject instanceof Fire) {
						// How can we most dastardly avoid String allocation?
						int options = 0;
						options |= board.containsType(Fire.class, board.getLocationInDirection(idx, Move.DOWN)) ? 1 : 0;
						options |= board.containsType(Fire.class, board.getLocationInDirection(idx, Move.LEFT))  ? 2 : 0;
						options |= board.containsType(Fire.class, board.getLocationInDirection(idx, Move.UP))  ? 4 : 0;
						options |= board.containsType(Fire.class, board.getLocationInDirection(idx, Move.RIGHT))  ? 8: 0;
						
						String fireImg = null;
						switch(options) {
							// Clean Orthogonals
							case 1 : fireImg = "game/explosion-top"; break;
							case 2 : fireImg = "game/explosion-right"; break;
							case 4 : fireImg = "game/explosion-bottom"; break;
							case 8 : fireImg = "game/explosion-left"; break;
							// Two-Ways
							case 3 : fireImg = "game/explosion-right-top"; break;
							case 5 : fireImg = "game/explosion-vertical"; break;
							case 6 : fireImg = "game/explosion-right-bottom"; break;
							case 9 : fireImg = "game/explosion-left-top"; break;
							case 10 : fireImg = "game/explosion-horizontal"; break;
							case 12 : fireImg = "game/explosion-left-bottom"; break;
							// Three-Ways & rest
							default : fireImg = "game/explosion-center"; break;
						}
						
						batch.draw(skin.getRegion(fireImg),
									i * TILE_SIZE + x,
									j * TILE_SIZE + y,
									TILE_SIZE,
									TILE_SIZE);

					} else if (gameObject instanceof Coin) {
						Coin coin = (Coin) gameObject;
						batch.draw(	skin.getRegion(coin.isCoolingDown() ? "game/coin-cooldown" : "game/coin"),
									i * TILE_SIZE + x,
									j * TILE_SIZE + y,
									TILE_SIZE,
									TILE_SIZE);
					} else if (gameObject instanceof Bomb) {
						batch.draw(skin.getRegion("game/bomb"), i * TILE_SIZE + x, j * TILE_SIZE + y, TILE_SIZE, TILE_SIZE);
					} else if (gameObject instanceof Wall) {
						Wall wall = (Wall) gameObject;
						batch.draw(	skin.getRegion(wall.isDestructible() ? "game/wall-destroyable" : "game/wall-non-destroyable"),
									i * TILE_SIZE + x,
									j * TILE_SIZE + y,
									TILE_SIZE,
									TILE_SIZE);
					} else if (gameObject instanceof Item) {
						Item item = (Item) gameObject;
						batch.draw(	skin.getRegion(item.getUpgradeType() == UpgradeType.BOMB_COUNT	? "game/powerup-bomb"
																									: "game/powerup-flame"),
									i * TILE_SIZE + x,
									j * TILE_SIZE + y,
									TILE_SIZE,
									TILE_SIZE);
					} else if (gameObject instanceof BombMan) {
						BombMan bombMan = (BombMan) gameObject;

						Color clr = batch.getColor();
						if (bombMan.isInvulnerable()) {
							batch.setColor(Color.GRAY);
						}

						batch.draw(	skin.getRegion(bombMan.isDead()	? "ingame/grave_" + (bombMan.getOwner() + 1)
																	: "ingame/player_" + (bombMan.getOwner() + 1)),
									i * TILE_SIZE + x,
									j * TILE_SIZE + y,
									TILE_SIZE,
									TILE_SIZE);

						batch.setColor(clr);
					} 
					
					//@formatter:on
				}
			}
		}

	}

	@Override
	public float getPrefWidth() {
		return (state != null ? state.getBoard()
									.getWidth() * TILE_SIZE : 0) ;
	}

	@Override
	public float getPrefHeight() {
		return (state != null ? state.getBoard()
									.getHeight() * TILE_SIZE : 0);
	}

}
