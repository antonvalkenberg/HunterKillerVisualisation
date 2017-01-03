package net.codepoke.ai.challenges.hunterkiller.ui;

import static net.codepoke.ai.challenges.hunterkiller.ui.MatchVisualizationConstants.PLAYER_TAG;

import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;

/**
 * Displays the Scoreboard of the game.
 * 
 * The board consists of:
 * * The match name
 * * The player names & scores
 * * The score bar which shows the respective strength of all players.
 * 
 * @author GJ Roelofs <gj.roelofs@codepoke.net>
 */
public class MatchStatistics
		extends Table {

	MatchVisualization parent;

	Skin skin;

	Label matchName;

	// The images, labels & table used for the score board.
	Array<Image> playerImages;
	Array<Label> scoreAndNames;
	Table scoreBoard;

	// The images, labels & table used for the scorebar
	Array<Cell<Stack>> barCells;
	Array<Label> barScores;
	Image scoreStart, scoreEnd;
	Table scoreBar;

	public MatchStatistics(MatchVisualization parent, Skin skin) {

		this.parent = parent;
		this.skin = skin;

		matchName = new Label("Local Match", skin);

		playerImages = new Array<Image>();
		scoreAndNames = new Array<Label>();
		barCells = new Array<Cell<Stack>>();
		barScores = new Array<Label>();

		scoreBoard = new Table();
		scoreBar = new Table();

		add(matchName).expand()
						.center()
						.pad(10);

		row();

		add(scoreBoard).expand()
						.fill();

		row();

		add(scoreBar).expandX()
						.width(Value.percentWidth(1, parent.board))
						.pad(10)
						.fill();

		scoreStart = new Image(skin.getDrawable("ui/bars/bar_left_1"));
		scoreEnd = new Image(skin.getDrawable("ui/bars/bar_right_1"));

		scoreStart.setScaling(Scaling.none);
		scoreEnd.setScaling(Scaling.none);

	}

	public void setMatchName(String name) {
		matchName.setText(name);
	}

	public void setStateInfo(String[] playerNames, int[] scores) {

		if (scoreAndNames.size != playerNames.length) {
			// Reset scores and names and layout everything
			playerImages.clear();
			barScores.clear();
			scoreAndNames.clear();
			scoreBar.clear();

			scoreBoard.clear();
			scoreBar.clear();

			for (int i = 0; i < playerNames.length; i++) {
				Label lbl = new Label("", skin);
				Image img = new Image(skin.getRegion(PLAYER_TAG + (i + 1)));
				img.setScaling(Scaling.none);

				scoreAndNames.add(lbl);
				playerImages.add(img);

				if (i % 2 == 0) {
					scoreBoard.add(lbl)
								.uniformX()
								.align(Align.right)
								.pad(10, 50, 10, 5);
					scoreBoard.add(img)
								.size(img.getWidth(), img.getHeight())
								.pad(10, 5, 10, 0);
				} else {
					scoreBoard.add(img)
								.size(img.getWidth(), img.getHeight())
								.pad(10, 5, 10, 0);
					scoreBoard.add(lbl)
								.uniformX()
								.align(Align.left)
								.pad(10, 5, 10, 50);

					scoreBoard.row();
				}
			}

			scoreBar.add(scoreStart);

			for (int i = 0; i < playerNames.length; i++) {
				Image bar = new Image(skin.getDrawable("ui/bars/bar_" + (i + 1)));
				bar.setScaling(Scaling.stretchX);

				Label lbl = new Label("", skin);
				lbl.setAlignment(Align.center);
				barCells.add(scoreBar.stack(bar, lbl)
										.width(0));
				barScores.add(lbl);
			}

			scoreBar.add(scoreEnd);
		}

		// Figure out what width the bars need to be, and which ones we can hide
		float total = 0;
		int min = 0, minIdx = 0;
		int maxIdx = 0;

		// Update the labels / images
		for (int i = 0; i < playerNames.length; i++) {

			scoreAndNames.get(i)
							.setText(i % 2 == 0 ? scores[i] + " : " + playerNames[i] : playerNames[i] + " : " + scores[i]);	// Can't
																															// use
																															// String.format

			int score = scores[i];
			if (min == 0 && score > min) {
				min = score;
				minIdx = i;
			}

			if (min == 0 || score > 0) {
				maxIdx = i;
			}

			total += scores[i];

			scoreStart.setDrawable(skin.getDrawable("ui/bars/bar_left_" + (minIdx + 1)));
			scoreEnd.setDrawable(skin.getDrawable("ui/bars/bar_right_" + (maxIdx + 1)));
		}

		for (int i = 0; i < barCells.size; i++) {
			Cell<Stack> bar = barCells.get(i);
			if (i < minIdx || i > maxIdx || (scores[i] == 0 && total != 0)) {
				bar.width(Value.zero);
				barScores.get(i)
							.setText("");
			} else {
				barScores.get(i)
							.setText(scores[i] + "");
				bar.expand()
					.minWidth(Value.minWidth)
					.maxWidth(Value.maxWidth)
					.prefWidth(Value.percentWidth(1 * (total == 0 ? 1f / playerNames.length : scores[i] / total), scoreBar));
			}

		}

		scoreBar.invalidateHierarchy();

	}

}
