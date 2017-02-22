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
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public abstract class MatchFrame<S extends State, A extends Action>
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
	MatchRenderer<S, A> renderer;

	/** The listeners which will be updated once a state is visualized. */
	Array<StateVisualizationListener<S, A>> stateVisualizationListeners;

	/** Called whenever the preferred size (see width, height) of this frame are updated based on its contents. */
	Array<FrameResizeListener> frameResizeListeners;

	// The error message to be displayed if the current State cannot be displayed.
	Label errorMessage;

	// Variables used for handling and interacting with state
	MatchMessageParser<S, A> parser;

	/** The states available for rendering. */
	Array<S> states;

	/** The actions that led to state X. */
	Array<A> actions;

	// The ideal width/height for this Visualization, only valid after setting the initial state.
	int width = 640, height = 480;

	public MatchFrame(GameRules<S, A> rules) {
		this.parser = new MatchMessageParser<S, A>(rules);
		this.parser.addStateListeners(new StateCreationListener<S, A>() {

			@Override
			public void onStateCreation(S state, A action) {
				addState(state, action);
			}

		});

		stateVisualizationListeners = new Array<StateVisualizationListener<S, A>>();
		frameResizeListeners = new Array<FrameResizeListener>();
	}

	@Override
	public final void create() {

		Gdx.graphics.setContinuousRendering(false);

		// Repaint every second
		Timer.schedule(new Task() {

			@Override
			public void run() {
				Gdx.graphics.requestRendering();
			}
		}, 1, 1);

		Skin uiSkin = new Skin();
		uiSkin.addRegions(new TextureAtlas(Gdx.files.internal("game.atlas")));
		uiSkin.addRegions(new TextureAtlas(Gdx.files.internal("uiskin.atlas")));
		uiSkin.load(Gdx.files.internal("uiskin.json"));

		stage = new Stage(new ScreenViewport());
		states = new Array<S>();
		actions = new Array<A>();

		Gdx.input.setInputProcessor(stage);

		errorMessage = new Label("No Error", uiSkin);
		errorMessage.setVisible(true);
		errorMessage.toFront();

		renderer = createRenderer(this, uiSkin);
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
		boardBase.stack(renderer, errorMessage)
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
	private final void stateChange(S newState, A action) {

		S prevState = renderer.getState();

		renderer.setState(newState, action);
		statistics.setStateInfo(getPlayers(newState), getScores(newState));
		onStateChange(prevState, action, newState);

		recalculatePreferredSize(prevState == null);
	}

	@Override
	public synchronized void render() {

		stage.act(Gdx.graphics.getDeltaTime());

		int currentRound = controls.getCurrentRound();

		if (currentRound < states.size) {
			errorMessage.setVisible(false);
			S s = states.get(currentRound);
			A a = actions.get(currentRound);

			if (renderer.getState() != s) {
				stateChange(s, a);
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
		recalculatePreferredSize(false);
	}

	/**
	 * Request a pack() and layout() on the table given the current contents and updates the size of the frame based on
	 * the minimum size required to display everything.
	 */
	public void recalculatePreferredSize(boolean resizeFrame) {

		renderer.invalidateHierarchy();
		rootTable.invalidateHierarchy();
		rootTable.layout();
		rootTable.pack();

		width = (int) rootTable.getPrefWidth();
		height = (int) rootTable.getPrefHeight();

		for (FrameResizeListener listener : frameResizeListeners) {
			listener.updateSize(width, height);
		}

		if (!resizeFrame)
			return;

		Gdx.app.postRunnable(new Runnable() {

			@Override
			public void run() {
				Gdx.app.getGraphics()
						.setWindowedMode(width, height);
			}

		});
	}

	/**
	 * Adds the given state and action that led to it to the Visualization and requesting a new render.
	 * The action is null for the initial state.
	 */
	public synchronized void addState(S state, A action) {
		states.add(state);
		actions.add(action);
		controls.setStateRange(states.size - 1);
		controls.invalidateHierarchy();

		if (states.size == 1) {
			stateChange(state, action);
		}

		Gdx.graphics.requestRendering();
	}

	/** Sets the error message, and hides the board renderer. */
	public void displayError(String error) {
		if (error == null) {
			errorMessage.setText("");
			renderer.setVisible(true);
		} else {
			errorMessage.setText(error);
			renderer.setVisible(false);
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
	 * @param oldState
	 *            The previous state, possibly null (initial state).
	 * @param action
	 *            The action that led to the new state, null if it is the initial state.
	 * @param newState
	 *            The new state.
	 */
	public void onStateChange(S oldState, A action, S newState) {
		// Call for every listener that has subscribed
		for (int i = 0; i < stateVisualizationListeners.size; i++) {
			stateVisualizationListeners.get(i)
										.newStateVisualized(oldState, action, newState);
		}
	}

	/** Subscribe a listener to any state-visualization events. */
	public void addStateVisualizationListeners(StateVisualizationListener<S, A> listener) {
		stateVisualizationListeners.add(listener);
	}

	/** Subscribe a listener to any updates to the preferred size. */
	public void addFrameResizeListeners(FrameResizeListener listener) {
		frameResizeListeners.add(listener);
	}

	/** Called when the MatchVisualization has finished {@link #create()}. */
	public void onCreate(Skin skin) {
	}

	/** Creates the visualization which should render the state. Delayed so we can instantiate the Skin & parent. */
	public abstract MatchRenderer<S, A> createRenderer(MatchFrame<S, A> parent, Skin skin);

	/** Should parse and return the player names in the State. */
	public abstract String[] getPlayers(S state);

	/** Should parse and returns the scores stored in the State. */
	public abstract int[] getScores(S state);

}
