package net.codepoke.ai.challenges.hunterkiller.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;

import net.codepoke.ai.Action;
import net.codepoke.ai.State;

public abstract class MatchRenderer<S extends State, A extends Action> extends Widget {

	/** The current State we are rendering. */
	protected S state;
	
	protected MatchVisualization<S, A> parent;
	
	protected Skin skin;

	public MatchRenderer(MatchVisualization<S,A> parent, Skin skin) {
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
	
	
	public S getState() {
		return state;
	}

	public void setState(S state) {
		this.state = state;
		setSize(getPrefWidth(), getPrefHeight());
		invalidateHierarchy();
	}
	
}
