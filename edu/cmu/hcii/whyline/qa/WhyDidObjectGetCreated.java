package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.analysis.AnalysisException;
import edu.cmu.hcii.whyline.trace.nodes.ObjectState;

/**
 * Explains why a particular instruction was executed, focusing on control flow dependencies and their direct data dependencies.
 * 
 * @author Andrew J. Ko
 *
 */
public final class WhyDidObjectGetCreated extends WhyDidQuestion<ObjectState> {

	public WhyDidObjectGetCreated(Asker asker, ObjectState object, String descriptionOfEvent) {

		super(asker, object, descriptionOfEvent);
		
	}
	
	protected Answer answer() throws AnalysisException {
		
		int initializationID = trace.getInitializationOfObjectID(subject.getObjectID());

		if(initializationID >= 0) {
			return new CauseAnswer(this, initializationID, "This is why <b>" + getTrace().getDescriptionOfObjectID(subject.getObjectID()) + "</b> was instantiated.");
		}
		else
			return new MessageAnswer(this, "Unfortunately, the Whyline didn't record the creation of this event, so an explanation of its creation is unavailable.");
		
	}

	public String getQuestionExplanation() {
		
		return "explain why <b>" + getTrace().getDescriptionOfObjectID(subject.getObjectID()) + "</b> was instantiated";
		
	}

}