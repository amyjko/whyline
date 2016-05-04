package edu.cmu.hcii.whyline.ui.qa;

import java.awt.BorderLayout;
import java.util.Hashtable;

import javax.swing.SwingConstants;

import edu.cmu.hcii.whyline.qa.*;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.components.WhylineLabel;
import edu.cmu.hcii.whyline.ui.components.WhylinePanel;

/**
 * @author Andrew J. Ko
 *
 */
public class AnswerUI extends WhylinePanel {

	private final WhylineUI whylineUI;
	private final Question<?> question;
	
	private SituationUI currentSituation;
	
	private final Hashtable<Answer, SituationUI> situationsByAnswers = new Hashtable<Answer, SituationUI>();
	
	private final WhylinePanel situationsPanel;
	private final WhylinePanel status;
	
	public AnswerUI(WhylineUI whylineUI, Question<?> question) {

		super();

		this.whylineUI = whylineUI;
		this.question = question;
		
		setLayout(new BorderLayout());

		WhylineLabel answering = new WhylineLabel("<html><center>Answering question...</center>", UI.getLargeFont().deriveFont(24.0f));
		answering.setHorizontalAlignment(SwingConstants.CENTER);
		
		status = new WhylinePanel(new BorderLayout());
		status.add(answering, BorderLayout.CENTER);

		situationsPanel = new WhylinePanel(new BorderLayout());
				
		add(status, BorderLayout.CENTER);
		
	}

	public SituationUI getSituationSelected() { return currentSituation; }
	
	public WhylineUI getWhylineUI() { return whylineUI; }
	public Question<?> getQuestion() { return question; }

	public void showSituation(Answer answer) {
		
		remove(status);
		
		SituationUI situationUI = situationsByAnswers.get(answer);
		if(situationUI == null) {
			
			situationUI = new SituationUI(this, answer);
			situationsByAnswers.put(answer, situationUI);
			
		}
		
		currentSituation = situationUI;
		add(currentSituation, BorderLayout.CENTER);
		
		validate();

		currentSituation.getVisualizationUI().getVisualization().layoutEvents(true, false);
		currentSituation.getVisualizationUI().getVisualization().initializeToLastEvent();

		situationUI.getVisualizationUI().requestFocusInWindow();

		situationUI.getVisualizationUI().setSelection(situationUI.getVisualizationUI().getSelection(), true);
		
	}
		
}
