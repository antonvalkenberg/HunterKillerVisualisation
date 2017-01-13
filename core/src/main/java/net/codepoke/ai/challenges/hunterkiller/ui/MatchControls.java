package net.codepoke.ai.challenges.hunterkiller.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.TextInputListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

public class MatchControls
		extends Table {

	MatchVisualization parent;

	// The skin for this UI Widget
	Skin skin;

	/** The slide which controls which state we are currently displaying. */
	Slider timeline;

	/** The controls to be used for playing back the match. ( |< << < >/|| > >> >>| ) */
	Button start, slower, previous, playPause, next, faster, end;

	/** Displays current playbackspeed. */
	Label speedLbl;

	/** Displays the current round. */
	Label roundLbl;

	int playbackSpeed = 1;

	boolean playingBack = false;

	// Counter to go to the next round if we are playing back.
	private float currentDt = 0;

	public MatchControls(MatchVisualization parent, Skin skin) {

		this.parent = parent;
		this.skin = skin;

		timeline = new Slider(0, 99, 1, false, skin, "timeline");

		playPause = new ImageButton(skin, "playPause");

		slower = new ImageButton(skin, "slower");
		faster = new ImageButton(skin, "faster");
		start = new ImageButton(skin, "start");
		end = new ImageButton(skin, "end");
		previous = new ImageButton(skin, "previous");
		next = new ImageButton(skin, "next");

		speedLbl = new Label(">> 1x", skin);
		roundLbl = new Label("Round: 1 / 100", skin);

		/**
		 * Click Listeners
		 */

		roundLbl.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.input.getTextInput(new TextInputListener() {

					@Override
					public void input(String text) {
						try {
							int newRound = Integer.parseInt(text);
							timeline.setValue(newRound - 1);
							Gdx.graphics.requestRendering();
						} catch (Exception e) {
						}
					}

					@Override
					public void canceled() {
					}
				}, "Please enter the round to jump to: (1-" + (int) (timeline.getMaxValue() + 1) + ")", "", ((int) timeline.getValue() + 1)
																											+ "");
			}
		});

		timeline.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				roundLbl.setText("Round: " + (int) (timeline.getValue() + 1) + " / " + (int) (timeline.getMaxValue() + 1));
			}
		});

		next.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				timeline.setValue(timeline.getValue() + 1);
			}
		});
		previous.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				timeline.setValue(timeline.getValue() - 1);
			}
		});

		start.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				timeline.setValue(timeline.getMinValue());
			}
		});

		end.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				timeline.setValue(timeline.getMaxValue());
			}
		});

		// Fast / Slow controls
		slower.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {

				if (playbackSpeed > 1)
					playbackSpeed /= 2;
				else if (playbackSpeed == 1)
					playbackSpeed = -1;
				else if (playbackSpeed <= -1)
					playbackSpeed *= 2;

				playbackSpeed = Math.max(-64, playbackSpeed);
				speedLbl.setText((playbackSpeed < 0 ? "<< " : ">> ") + playbackSpeed + "x");
				Gdx.graphics.requestRendering();
			}

		});

		faster.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				if (playbackSpeed >= 1)
					playbackSpeed *= 2;
				else if (playbackSpeed == -1)
					playbackSpeed = 1;
				else if (playbackSpeed <= -1)
					playbackSpeed /= 2;

				playbackSpeed = Math.min(64, playbackSpeed);
				speedLbl.setText((playbackSpeed < 0 ? "<< " : ">> ") + playbackSpeed + "x");
				Gdx.graphics.requestRendering();
			}

		});

		playPause.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				playingBack = playPause.isChecked();
				Gdx.graphics.requestRendering();
			}
		});

		setupScene();
	}

	protected void setupScene() {

		Table controls = new Table();
		controls.defaults()
				.pad(5);

		controls.add(start);
		controls.add(slower);
		controls.add(previous);
		controls.add(playPause);
		controls.add(next);
		controls.add(faster);
		controls.add(end);

		Table speedOverlay = new Table();
		speedOverlay.add(speedLbl)
					.pad(5)
					.align(Align.bottom | Align.right)
					.expand();

		Table bottomControls = new Table();
		bottomControls.defaults()
						.pad(5);

		bottomControls.add(roundLbl)
						.padBottom(-6)
						.expandX()
						.center();

		bottomControls.row();

		bottomControls.add(timeline)
						.expandX()
						.width(Value.percentWidth(1, parent.board));

		bottomControls.row();

		bottomControls.add(controls)
						.padTop(-8)
						.center()
						.expand();

		stack(bottomControls, speedOverlay).expand()
											.fillX()
											.bottom();

	}

	@Override
	public void act(float delta) {
		currentDt += delta;

		if (currentDt > 0.125f) {
			currentDt = 0;
			if (playingBack) {
				timeline.setValue(timeline.getValue() + playbackSpeed);
			}
		}

		// If we are playing back, request the next render
		if (playingBack)
			Gdx.graphics.requestRendering();
	}

	public void setStateRange(int rounds) {
		timeline.setRange(0, rounds);
		roundLbl.setText("Round: " + (int) (timeline.getValue() + 1) + " / " + (int) (timeline.getMaxValue() + 1));
	}

	public int getCurrentRound() {
		return (int) timeline.getValue();
	}

	public boolean playingBack() {
		return playingBack;
	}

	public int getPlaybackSpeed() {
		return playbackSpeed;
	}

}
