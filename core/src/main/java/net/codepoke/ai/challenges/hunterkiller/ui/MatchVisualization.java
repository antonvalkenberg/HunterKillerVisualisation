package net.codepoke.ai.challenges.hunterkiller.ui;

import net.codepoke.ai.Action;
import net.codepoke.ai.GameRules;
import net.codepoke.ai.State;
import net.codepoke.ai.network.MatchMessageParser;
import net.codepoke.ai.network.MatchMessageParser.StateCreationListener;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public abstract class MatchVisualization<S extends State, A extends Action>
		extends ApplicationAdapter {

	// The root on which we layout the UI and rendering of the state.
	Stage stage;

	/** Rendering Variables. */
	OrthographicCamera cam, uiCam;
	SpriteBatch batch;

	/** The root of the UI to be rendered. */
	Table rootTable;

	// UI Widget for displaying the statistics & the controls
	MatchStatistics statistics;
	MatchControls controls;
	MatchRenderer<S, A> board;

	// The error message to be displayed if the current State cannot be displayed.
	Label errorMessage;

	// Variables used for handling and interacting with state
	MatchMessageParser<S, A> parser;

	/** The states available for rendering. */
	Array<S> states;

	// The ideal width/height for this Visualization, only valid after setting the initial state.
	int width = 640, height = 480;

	public MatchVisualization(GameRules<S, A> rules) {
		this.parser = new MatchMessageParser<S, A>(rules);
		this.parser.addStateListeners(new StateCreationListener<S, A>() {

			@Override
			public void onStateCreation(S state, A action) {
				addState(state);
			}

		});
	}

	@Override
	public final void create() {

		Gdx.graphics.setContinuousRendering(false);

		Skin uiSkin = new Skin();
		uiSkin.addRegions(new TextureAtlas(Gdx.files.internal("game.atlas")));
		uiSkin.addRegions(new TextureAtlas(Gdx.files.internal("uiskin.atlas")));
		uiSkin.load(Gdx.files.internal("uiskin.json"));

		stage = new Stage(new ScreenViewport());
		states = new Array<S>();

		Gdx.input.setInputProcessor(stage);

		errorMessage = new Label("No Error", uiSkin);
		errorMessage.setVisible(true);
		errorMessage.toFront();

		board = createRenderer(this, uiSkin);
		controls = new MatchControls(this, uiSkin);
		statistics = new MatchStatistics(this, uiSkin);

		rootTable = new Table();
		rootTable.setFillParent(true);

		setupScene(uiSkin);

		onCreate(uiSkin);
	}

	protected void setupScene(Skin uiSkin) {
		final Table boardBase = new Table();
		boardBase.setBackground(new NinePatchDrawable(uiSkin.getPatch("ui/panel_btn_brown")));
		boardBase.stack(board, errorMessage)
					.pad(10);

		rootTable.add(statistics)
					.expandX()
					.fill()
					.align(Align.top | Align.center);

		rootTable.row();

		rootTable.add(boardBase);

		rootTable.row();

		rootTable.add(controls)
					.pad(0, 20, 0, 20)
					.expandX()
					.fill()
					.align(Align.bottom | Align.center);

		rootTable.setBackground(new NinePatchDrawable(uiSkin.getPatch("ui/panel_btn_beige")));

		stage.addActor(rootTable);
	}

	// Called whenever we want to display a different state
	private final void stateChange(S newState) {

		S prevState = board.getState();

		board.setState(newState);
		statistics.setStateInfo(getPlayers(newState), getScores(newState));
		onStateChange(newState);

		board.invalidateHierarchy();
		rootTable.layout();
		rootTable.pack();

		// Resize to the ideal size upon receiving the first State
		if (prevState == null) {
			Gdx.app.postRunnable(new Runnable() {

				@Override
				public void run() {
					Gdx.app.getGraphics()
							.setWindowedMode((int) rootTable.getPrefWidth(), (int) rootTable.getPrefHeight());
				}

			});

			width = (int) rootTable.getPrefWidth();
			height = (int) rootTable.getPrefHeight();
		}
	}

	@Override
	public synchronized void render() {
		stage.act(1 / 60f);

		int currentRound = controls.getCurrentRound();

		if (currentRound < states.size) {
			errorMessage.setVisible(false);
			S s = states.get(currentRound);

			if (board.getState() != s) {
				stateChange(s);
			}
		} else {
			errorMessage.setText("Downloading Match... ");
		}

		stage.draw();
	}

	@Override
	public void dispose() {
		stage.dispose();
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		stage.getViewport()
				.update(width, height, true);
		rootTable.invalidateHierarchy();
	}

	/**
	 * Adds the given state to the Visualization and requesting a new render.
	 */
	public synchronized void addState(S state) {
		states.add(state);
		controls.setStateRange(states.size - 1);
		controls.invalidateHierarchy();

		if (states.size == 1) {
			stateChange(state);
		}

		Gdx.graphics.requestRendering();
	}

	/** Sets the error message, and hides the board renderer. */
	public void displayError(String error) {
		if (error == null) {
			errorMessage.setText("");
			board.setVisible(true);
		} else {
			errorMessage.setText(error);
			board.setVisible(false);
		}
	}

	/**
	 * Returns the parser associated with this visualization.
	 */
	public MatchMessageParser<S, A> getParser() {
		return this.parser;
	}

	/**
	 * Returns the last state available in this Visualization, or null if no states are available.
	 */
	public S getLastState() {
		if (states.size == 0)
			return null;

		return states.peek();
	}

	/**
	 * Returns the height of the visualization, only properly set if the initial state is set.
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Returns the width of the visualization, only properly set if the initial state is set.
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Called whenever the state is changed.
	 * 
	 * @param newState
	 *            The new state
	 */
	public void onStateChange(S newState) {
	}

	/** Called when the MatchVisualization has finished {@link #create()}. */
	public void onCreate(Skin skin) {
	}

	/** Creates the visualization which should render the state. Delayed so we can instantiate the Skin & parent. */
	public abstract MatchRenderer<S, A> createRenderer(MatchVisualization<S, A> parent, Skin skin);

	/** Should parse and return the player names in the State. */
	public abstract String[] getPlayers(S state);

	/** Should parse and returns the scores stored in the State. */
	public abstract int[] getScores(S state);

}
