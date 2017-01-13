package net.codepoke.ai.challenges.hunterkiller.client;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerAction;
import net.codepoke.ai.challenge.hunterkiller.HunterKillerState;
import net.codepoke.ai.challenge.hunterkiller.Player;
import net.codepoke.ai.challenges.hunterkiller.HunterKillerVisualization;
import net.codepoke.ai.challenges.hunterkiller.ui.MatchVisualization;
import net.codepoke.ai.network.MatchMessageParser;
import net.codepoke.ai.network.MatchMessageParser.MessageResponse;
import net.codepoke.ai.network.MatchMessageParser.MessageResponseListener;

public class HtmlLauncher extends GwtApplication {

	static final int WIDTH = 480;
	static final int HEIGHT = 320;

	static final String HOST_NAME = "host", MATCH_NAME = "match_name", GAME_NAME = "game_name";

	static HtmlLauncher instance;
	static MatchVisualization<HunterKillerState, HunterKillerAction> gameInstance;

	int previousGraphRound = 0;

	public String getPreloaderBaseURL() {
		return GWT.getModuleBaseURL() + "../assets/";
	}

	@Override
	public GwtApplicationConfiguration getConfig() {

		GwtApplicationConfiguration config = new GwtApplicationConfiguration(WIDTH, HEIGHT);

		Element element = Document.get()
									.getElementById("embed-html");
		VerticalPanel panel = new VerticalPanel();
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
		element.appendChild(panel.getElement());
		config.rootPanel = panel;

		return config;
	}

	@Override
	public ApplicationListener getApplicationListener() {
		instance = this;
		setLogLevel(LOG_DEBUG);
		setLoadingListener(new LoadingListener() {
			@Override
			public void beforeSetup() {

			}

			@Override
			public void afterSetup() {
				matchLoad();
				scaleCanvas();
				setupResizeHook();
			}
		});

		gameInstance = new HunterKillerVisualization();
		gameInstance.getParser()
					.addMessageListeners(new MessageResponseListener() {

						@Override
						public void onMessageResponse(MessageResponse response, Object message) {
							if (response == MessageResponse.MatchStart) {
								instance.scaleCanvas();
							} else if (response == MessageResponse.MatchRound) {
								HunterKillerState lastState = gameInstance.getLastState();

								if (instance.previousGraphRound + 5 <= lastState.getCurrentRound()) {
									instance.previousGraphRound = lastState.getCurrentRound();

									Player[] players = lastState.getPlayers();
									int[] scores = new int[players.length];
									for (int i = 0; i < scores.length; i++) {
										scores[i] = players[i].getScore();
									}
									instance.updateGraphData(lastState.getCurrentRound(), scores);

									if (instance.previousGraphRound % 100 == 0)
										instance.updateGraph();
								}
							}
						}

					});

		return gameInstance;
	}

	void matchLoad() {

		Map<String, List<String>> parameterMap = Window.Location.getParameterMap();

		List<String> hostNameStr = parameterMap.get(HOST_NAME);
		List<String> matchNameStr = parameterMap.get(MATCH_NAME);
		List<String> gameNameStr = parameterMap.get(GAME_NAME);

		if (hostNameStr == null) {
			debug("main", "Error: Host not set as parameters, defaulting");
			hostNameStr = Arrays.asList("ws://ai.codepoke.net/");
		}

		if (matchNameStr == null) {
			debug("main", "Error: Match Name not set as parameters, defaulting");
			matchNameStr = Arrays.asList("HunterKiller-v8mderr9qo2vd1v4b95tv4h05o-6");
		}

		if (gameNameStr == null) {
			debug("main", "Error: Game name not set as parameters, defaulting");
			gameNameStr = Arrays.asList("HunterKiller");
		}

		debug("main", "ID: " + matchNameStr.get(0) + " NAME: " + gameNameStr.get(0) + " URL: " + hostNameStr.get(0));

		attachSocket(hostNameStr.get(0) + "competition/stream_match?game_name=" + gameNameStr.get(0) + "&match_name=" + matchNameStr.get(0));
	}

	void scaleCanvas() {
		Element element = Document.get()
									.getElementById("embed-html");
		int newWidth = gameInstance.getWidth();
		int newHeight = gameInstance.getHeight();

		NodeList<Element> nl = element.getElementsByTagName("canvas");

		if (nl != null && nl.getLength() > 0) {
			Element canvas = nl.getItem(0);
			canvas.setAttribute("width", "" + newWidth + "px");
			canvas.setAttribute("height", "" + newHeight + "px");
			canvas.getStyle()
					.setWidth(newWidth, Style.Unit.PX);
			canvas.getStyle()
					.setHeight(newHeight, Style.Unit.PX);
		}
	}

	static void setError(String error) {
		if (gameInstance != null)
			gameInstance.displayError(error);
	}

	static void handleMessage(String message) {
		if (gameInstance != null) {
			MatchMessageParser<HunterKillerState, HunterKillerAction> parser = gameInstance.getParser();
			parser.parseMessage(gameInstance.getLastState(), message);
		}
	}

	native String getTestVar() /*-{
								return eval('$wnd.testDEV');
								}-*/;

	native int getWindowInnerWidth() /*-{
										return $wnd.innerWidth;
										}-*/;

	native int getWindowInnerHeight() /*-{
										return $wnd.innerHeight;
										}-*/;

	native void setupResizeHook() /*-{
									var htmlLauncher_onWindowResize = $entry(@net.codepoke.ai.challenges.hunterkiller.client.HtmlLauncher::handleResize());
									$wnd.addEventListener('resize', htmlLauncher_onWindowResize, false);
									}-*/;

	native void updateGraphData(int idx, int[] scores) /*-{
														$wnd.matchData.push({
														x : idx,
														a : scores[0],
														b : scores[1],
														c : scores[2],
														d : scores[3],
														});
														}-*/;

	native void updateGraph() /*-{
								$wnd.graph.setData($wnd.matchData);
								}-*/;

	native void attachSocket(String url) /*-{
											var socket = new WebSocket(url);

											socket.onopen = function() {
												socket.send(".");
											}

											socket.onmessage = function(msg) {
												@net.codepoke.ai.challenges.hunterkiller.client.HtmlLauncher::handleMessage(*)(msg.data);
												socket.send(".");
											}

											socket.onerror = function(error) {
												console.error('Error detected: ' + error);
											}

											socket.onclose = function() {
												console.error("!!----closed connection: " + url)
											}

											}-*/;

	public static void handleResize() {
		instance.scaleCanvas();
	}
}