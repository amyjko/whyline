package edu.cmu.hcii.whyline.ui.qa;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.qa.*;
import edu.cmu.hcii.whyline.source.FileInterface;
import edu.cmu.hcii.whyline.ui.UI;
import edu.cmu.hcii.whyline.ui.UserFocusListener;
import edu.cmu.hcii.whyline.ui.WhylineUI;
import edu.cmu.hcii.whyline.ui.components.WhylinePanel;

/**
 * @author Andrew J. Ko
 *
 */
public final class QuestionsUI extends WhylinePanel implements UserFocusListener {

	private final WhylineUI whylineUI;
	
	private final ArrayList<Question<?>> questions = new ArrayList<Question<?>>();
	private final HashMap<Question<?>,AnswerUI> answerPanelsByQuestion = new HashMap<Question<?>,AnswerUI>();
	
	private Question<?> questionSelected = null;
	
	public QuestionsUI(WhylineUI whylineUI) {

		super();

		setLayout(new BorderLayout());
		setMinimumSize(new Dimension(UI.getDefaultInfoPaneWidth(whylineUI), (int) (UI.getDefaultInfoPaneHeight(whylineUI) * 1.5)));
		
		this.whylineUI = whylineUI;
		
	}
	
	public void handleArrowOverChanged() {
		
		AnswerUI answerUI = getAnswerUIVisible();
		SituationUI situationUI = answerUI == null ? null : answerUI.getSituationSelected();
		VisualizationUI visualizationUI = situationUI == null ? null : situationUI.getVisualizationUI();
		if(visualizationUI != null)
			visualizationUI.getVisualization().handleArrowOverChanged();
		
	}

	public void setQuestion(Question<?> question) {

		questionSelected = question;
		
		removeAll();
		
		if(question == null) {
			
		}
		else {
			
			AnswerUI answerUI = answerPanelsByQuestion.get(question);
			if(answerUI == null) {
				
				answerUI = new AnswerUI(whylineUI, question);
				answerPanelsByQuestion.put(question, answerUI);
				questions.add(question);

			}

			add(answerUI, BorderLayout.CENTER);

			if(answerUI.getSituationSelected() != null) 
				answerUI.getSituationSelected().getVisualizationUI().requestFocusInWindow();
			
		}
		
	}
	
	public Question<?> getQuestionVisible() { return questionSelected; }
	
	public void removeQuestion(Question<?> question) {

		answerPanelsByQuestion.remove(question);
		questions.remove(question);
		
	}
	
	public SituationUI getSituationVisible() { return getQuestionVisible() == null ? null : answerPanelsByQuestion.get(getQuestionVisible()).getSituationSelected(); }

	public AnswerUI getAnswerUIVisible() { return getQuestionVisible() == null ? null : answerPanelsByQuestion.get(getQuestionVisible()); }

	public void showInstruction(Instruction subject) {}
	public void showInstructions(Iterable<? extends Instruction> subject) {}
	public void showEvent(int eventID) {}
	public void showMethod(MethodInfo subject) {}
	public void showClass(Classfile subject) {}
	public void showFile(FileInterface subject) {}
	public void showUnexecutedInstruction(UnexecutedInstruction subject) {}
	public void showExplanation(Explanation subject) { 

		if(getQuestionVisible() != null && subject != null) {

			AnswerUI answerPanel = answerPanelsByQuestion.get(getQuestionVisible());
			if(answerPanel != null) {
				SituationUI answerVisible = answerPanel.getSituationSelected();
				if(answerVisible != null) {
					answerVisible.getVisualizationUI().show(subject);
				}
			}
			
		}

	}

}