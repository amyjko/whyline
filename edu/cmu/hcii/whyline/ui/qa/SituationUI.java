package edu.cmu.hcii.whyline.ui.qa;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.border.EmptyBorder;

import edu.cmu.hcii.whyline.qa.Answer;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.components.WhylineButton;
import edu.cmu.hcii.whyline.ui.components.WhylineLabel;
import edu.cmu.hcii.whyline.ui.components.WhylinePanel;
import edu.cmu.hcii.whyline.ui.components.WhylineToolbar;
import edu.cmu.hcii.whyline.ui.views.View;

/**
 * @author Andrew J. Ko
 *
 */
public class SituationUI extends WhylinePanel {

	private static final String PREVIOUS_BLOCK_TEXT = "<html><center>" + UI.UP_WHITE_ARROW + "<br><i>block";

	private static final String PREVIOUS_EVENT_TEXT = "<html><center>" + UI.LEFT_ARROW + "<br><i>event";
	private static final String NEXT_EVENT_TEXT = "<html><center>" + UI.RIGHT_ARROW + "<br><i>event";

	private static final String PREVIOUS_IN_METHOD_TEXT = "<html><center>" + UI.LEFT_ARROW + " in<br><i>method</i>";
	private static final String NEXT_IN_METHOD_TEXT = "<html><center>" + UI.RIGHT_ARROW + " in<br><i>method</i>";

	private static final String PREVIOUS_IN_THREAD_TEXT = "<html><center>" + UI.LEFT_ARROW + " in<br><i>thread</i>";
	private static final String NEXT_IN_THREAD_TEXT = "<html><center>" + UI.RIGHT_ARROW + " in<br><i>thread</i>";

	private static final String HIDE_THREADS_TEXT = "<html><center>hide<br>threads";
	private static final String SHOW_THREADS_TEXT = "<html><center>show<br>threads";
	private static final String SINGLE_THREAD_TEXT = "<html><center>single<br>thread";
	private static final String COLLAPSE_TEXT = "<html><center>collapse/<br>expand";
	
	private final Answer answer;

	private final WhylineUI whylineUI;

	private final AnswerUI answerUI;
	private final VisualizationUI visualizationUI;
	private final WhylineLabel qa;

	private final WhylineButton previousEventLabel, nextEventLabel, previousBlockLabel, collapseLabel;
	private final WhylineButton previousInMethod, nextInMethod;
	private final WhylineButton previousInThread, nextInThread;
	private final WhylineButton showHideThreads;

	public SituationUI(AnswerUI answerUI, Answer answer) {

		this.answerUI = answerUI;
		this.whylineUI = answerUI.getWhylineUI();
		this.answer = answer;
		
		setLayout(new BorderLayout());

		visualizationUI = new VisualizationUI(this, answer);

		//// BUILD THE TOOLBAR STUFF

		final String tab = "&nbsp;&nbsp;";
		final String qanda =
			"<html>" +
			"<font size=\"+1\"><b>Q</b></font>" + tab +  answer.getQuestion().getQuestionText() + 
			"<br>" + 
			"<font size=\"+1\"><b>A</b></font>" + tab + answer.getAnswerText();

		qa = new WhylineLabel("");
		qa.setFont(UI.getMediumFont());
		// Why is this negative you ask? The rendered HTML has some strange whitespace above the text. Don't know why its there. It's ugly!
		qa.setBorder(new EmptyBorder(-UI.getBorderPadding(), 0, 0, 0));
		qa.setText(qanda);
		
		if(answer.hasVisualizationContent()) {
						
			FontMetrics metrics = whylineUI.getGraphics().getFontMetrics(UI.getSmallFont());
			Dimension maxSize = new Dimension(metrics.charWidth('e') * 9, (metrics.getHeight() * 2));
			
			previousEventLabel = new WhylineButton(PREVIOUS_EVENT_TEXT, whylineUI.getActions().goToPreviousEvent, maxSize, UI.getSmallFont(), "select event shown before selection in visualization");
			nextEventLabel = new WhylineButton(NEXT_EVENT_TEXT, whylineUI.getActions().goToNextEvent, maxSize, UI.getSmallFont(), "select event after selection in visualization");
	
			previousInMethod = new WhylineButton(PREVIOUS_IN_METHOD_TEXT, 
					new AbstractAction() { public void actionPerformed(ActionEvent e) {
						visualizationUI.getVisualization().showPreviousOrNextEventInThreadOrMethod(true, true);
					}},
					maxSize, UI.getSmallFont(), "select the previous event in this method");
			nextInMethod = new WhylineButton(NEXT_IN_METHOD_TEXT, 
					new AbstractAction() { public void actionPerformed(ActionEvent e) {
						visualizationUI.getVisualization().showPreviousOrNextEventInThreadOrMethod(false, true);
					}},
					maxSize, UI.getSmallFont(), "select the next event in this method");
	
			previousInThread = new WhylineButton(PREVIOUS_IN_THREAD_TEXT, 
					new AbstractAction() { public void actionPerformed(ActionEvent e) {
						visualizationUI.getVisualization().showPreviousOrNextEventInThreadOrMethod(true, false);
					}},
					maxSize, UI.getSmallFont(), "select the previous event in this thread");
			nextInThread = new WhylineButton(NEXT_IN_THREAD_TEXT, 
					new AbstractAction() { public void actionPerformed(ActionEvent e) {
						visualizationUI.getVisualization().showPreviousOrNextEventInThreadOrMethod(false, false);
					}},
					maxSize, UI.getSmallFont(), "select the next event in this thread");
			
			previousBlockLabel = new WhylineButton(PREVIOUS_BLOCK_TEXT, whylineUI.getActions().goToPreviousBlock, maxSize, UI.getSmallFont(), "select the previous call or conditional");
			collapseLabel = new WhylineButton(COLLAPSE_TEXT, whylineUI.getActions().collapseBlock, maxSize, UI.getSmallFont(), "collapse or expand the selected call or conditional");
			showHideThreads = new WhylineButton(SHOW_THREADS_TEXT, whylineUI.getActions().showHideThreads, maxSize, UI.getSmallFont(), "collapse or expand threads");
	
			WhylineToolbar toolbar = new WhylineToolbar(WhylineToolbar.HORIZONTAL);
	
			toolbar.add(qa);
			toolbar.addSeparator();
			toolbar.add(previousEventLabel);
			toolbar.add(nextEventLabel);
			toolbar.addSeparator();
			toolbar.add(previousInMethod);
			toolbar.add(nextInMethod);
			toolbar.addSeparator();
			toolbar.add(previousInThread);
			toolbar.add(nextInThread);
			toolbar.addSeparator();
			toolbar.add(previousBlockLabel);
			toolbar.addSeparator();
			toolbar.add(collapseLabel);
			toolbar.addSeparator();
			toolbar.add(showHideThreads);
			
			add(toolbar, BorderLayout.NORTH);
			add(visualizationUI, BorderLayout.CENTER);
			
		}
		else {
			
			previousEventLabel = null;
			nextEventLabel = null;
			previousInMethod = null;
			nextInMethod = null;
			previousInThread = null;
			nextInThread = null;
			previousBlockLabel = null;
			collapseLabel = null;
			showHideThreads = null;

			add(qa, BorderLayout.CENTER);
			
			qa.setFont(UI.getLargeFont().deriveFont(Font.PLAIN));
			
		}

		updateHintsWith(null);
		
	}
	
	public AnswerUI getAnswerUI() { return answerUI; }
	public WhylineUI getWhylineUI() { return whylineUI; }
	public Answer getAnswer() { return answer; }
	public VisualizationUI getVisualizationUI() { return visualizationUI; }

	public void updateHintsWith(View view) { 
		
		if(!isVisible()) return;
		if(!answer.hasVisualizationContent()) return;
		
		Color enabled = UI.getControlTextColor();
		
		Visualization viz = visualizationUI.getVisualization();
		
		previousEventLabel.setEnabled(visualizationUI.getEventBefore(view) != null);
		nextEventLabel.setEnabled(visualizationUI.getEventAfter(view) != null);

		previousInMethod.setEnabled(viz.getPreviousOrNextEventInThreadOrMethod(true, true) != null);
		nextInMethod.setEnabled(viz.getPreviousOrNextEventInThreadOrMethod(false, true) != null);

		previousInThread.setEnabled(viz.getPreviousOrNextEventInThreadOrMethod(true, false) != null);
		previousInThread.setEnabled(viz.getPreviousOrNextEventInThreadOrMethod(false, false) != null);
		
		previousBlockLabel.setEnabled(visualizationUI.getEnclosingBlock(view) != null);

		collapseLabel.setEnabled(view instanceof EventBlockView);
		
		if(visualizationUI.getVisualization().getNumberOfThreadRows() == 1) {
			showHideThreads.setEnabled(false);
			showHideThreads.setText(SINGLE_THREAD_TEXT);
			showHideThreads.setVisible(false);
		}
		else {
			if(visualizationUI.getVisualization().areThreadsVisible()) showHideThreads.setText(HIDE_THREADS_TEXT);
			else showHideThreads.setText(SHOW_THREADS_TEXT);
		}
		
	}

}