package net.codepoke.ai.challenges.hunterkiller.ui;

import net.codepoke.ai.Action;
import net.codepoke.ai.State;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;

public abstract class MatchRenderer<S extends State, A extends Action>
		extends Widget {

	/** The current State we are rendering. */
	protected S state;

	/** The action that led to this state, potentially null. */
	protected A action;

	protected MatchFrame<S, A> parent;

	protected Skin skin;

	/** The scale with which should be rendered, set through controls to 1, 1.5, 2 */
	protected float scale = 1;

	public MatchRenderer(MatchFrame<S, A> parent, Skin skin) {
		this.skin = skin;
		this.parent = parent;
	}

	public void draw(Batch batch, float parentAlpha) {
		super.draw(batch, parentAlpha);
		onDraw(batch, parentAlpha);
	}

	public abstract void onDraw(Batch batch, float parentAlpha);

	@Override
	public abstract float getPrefWidth();

	@Override
	public abstract float getPrefHeight();

	/**
	 * Getters / Setters
	 */

	public void setScale(float scale) {
		this.scale = scale;
	}

	/** The current state the renderer is visualizing. */
	public S getState() {
		return state;
	}

	/** The action that led to this state, null if initial state or going backwards. */
	public A getAction() {
		return action;
	}

	public void setState(S state, A action) {
		this.state = state;
		this.action = action;
		setSize(getPrefWidth(), getPrefHeight());
		invalidateHierarchy();
	}

}
