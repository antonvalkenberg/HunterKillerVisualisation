package net.codepoke.ai.challenges.hunterkiller.ui;

public interface StateVisualizationListener<State, Action> {

	/**
	 * Called from visualization when we switch to a new state.
	 * 
	 * @param oldState
	 *            The previous visualized state, possibly null (initial state), not necessarily preceding state (if
	 *            hopping over timeline).
	 * @param action
	 *            The action that led to the new state, possibly null (initial state).
	 * @param newState
	 *            The next state, not null.
	 */
	void newStateVisualized(State oldState, Action action, State newState);

}
