package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.analysis.AnalysisException;
import edu.cmu.hcii.whyline.qa.Question.EventID;

/**
 * Explains the reason for a particular value being produced, emphasizing the data dependencies and not the control dependencies.
 * 
 * @author Andrew J. Ko
 *
 */
public final class WhyDidEventOccur extends WhyDidQuestion<EventID> {

	public WhyDidEventOccur(Asker asker, int eventID, String descriptionOfEvent) {

		super(asker, new EventID(eventID), descriptionOfEvent);

	}

	protected Answer answer() throws AnalysisException {

		return new CauseAnswer(this, subject.getEventID(), "These events were responsible.");

	}

	public String getQuestionExplanation() {
		
		return "explain why <b>" + getDescriptionOfSubject() + "</b> " + getDescriptionOfEvent();
		
	}
		
}