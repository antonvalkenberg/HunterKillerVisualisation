package net.codepoke.ai.challenges.hunterkiller.desktop;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.codepoke.ai.GameRules;
import net.codepoke.ai.GameRules.Result;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerRules;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerStateFactory;
import net.codepoke.ai.challenges.hunterkiller.HunterKillerVisualization;
import net.codepoke.ai.network.MatchMessageParser;
import net.codepoke.ai.network.MatchStream;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;

public class DesktopLauncher {

	public static void main(String[] arg) throws URISyntaxException {

		// Create the packed asset atlas
		TexturePacker.Settings settings = new TexturePacker.Settings();
		settings.maxHeight = 2048;
		settings.maxWidth = 2048;
		settings.useIndexes = true;
		settings.paddingX = settings.paddingY = 1;
		settings.edgePadding = true;
		settings.bleed = true;
		settings.filterMin = TextureFilter.MipMapNearestNearest;
		settings.filterMag = TextureFilter.MipMapNearestNearest;
		// TexturePacker.process(settings, "imgs/", System.getProperty("user.dir"), "game.atlas");

		// Start up the game
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.forceExit = true;

		final HunterKillerVisualization listener = new HunterKillerVisualization();

		new LwjglApplication(listener, config);

		// testStream(listener, "localhost:9000/competition/stream_match" , "HunterKiller" ,
		// "HunterKiller-glv3jsudnt9gbuvavqtesuhl5k-6");
		simulateStream(listener);
	}

	private static void testStream(final HunterKillerVisualization listener, final String server, final String gameName,
			final String matchID) {
		new Thread() {

			public void run() {
				try {

					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					List<NameValuePair> params = new ArrayList<NameValuePair>();
					params.add(new BasicNameValuePair(MatchMessageParser.GAME_NAME_PARAMETER, gameName));
					params.add(new BasicNameValuePair(MatchMessageParser.MATCH_NAME_PARAMETER, matchID));
					String encodedParams = URLEncodedUtils.format(params, StandardCharsets.UTF_8);

					String format = String.format("ws://%s?%s", server, encodedParams);
					MatchStream stream = new MatchStream(listener.getParser(), new URI(format));
					boolean correct = stream.connectBlocking();

					if (!correct) {
						System.err.println("Could not connect to server: " + server);
						return;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	/**
	 * Tests streaming a match to the visualizer by locally playing a game, and sending it to the
	 * visualizer.
	 * 
	 * @param listener
	 */
	private static void simulateStream(final HunterKillerVisualization vis) {
		final MatchMessageParser<HunterKillerState, HunterKillerAction> listener = vis.getParser();
		new Thread() {

			public void run() {

				// Small wait for visualisation to properly setup
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				GameRules<HunterKillerState, HunterKillerAction> rules = new HunterKillerRules();
				Array<HunterKillerAction> actions = new Array<HunterKillerAction>();

				// Create the initial state
				HunterKillerState state = new HunterKillerStateFactory().generateInitialState(new String[] { "A", "B", "C", "D" }, null);

				HunterKillerState orgState = state.copy();

				RandomBot randomBot = new RandomBot(); // Instantiate your bot here

				Json json = new Json();

				listener.parseMessage(vis.getLastState(), json.toJson(state.getPlayers())); // Players
				listener.parseMessage(vis.getLastState(), json.toJson(Array.with(orgState))); // Initial State

				// The following snippet will run a match with the given AI for all player seats until the match is
				// finished or an error occurs.
				// This would fail if your AI assumes it always operates in the same seat; instead of querying
				// State.getCurrentPlayer().
				Result result;
				do {
					HunterKillerAction action = randomBot.handle(state);

					actions.add(action);
					// Alternatively, send the action immediately: listener.parseMessage(vis.getLastState(),
					// json.toJson(action));
					result = rules.handle(state, action);
				} while (!result.isFinished() && result.isAccepted());

				listener.parseMessage(vis.getLastState(), json.toJson(actions));

			}
		}.start();
	}
}
