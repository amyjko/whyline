package edu.cmu.hcii.whyline.qa;

import java.util.Set;

import edu.cmu.hcii.whyline.analysis.*;
import edu.cmu.hcii.whyline.qa.Question.EventID;

/**
 * Explains why a particular instruction was executed, focusing on control flow dependencies and their direct data dependencies.
 * 
 * @author Andrew J. Ko
 *
 */
public final class WhyDidntArgumentHaveValue extends WhyDidntQuestion<EventID> {

	private final int argument;
	private long[] objectIDs;
	private final Set<ValueSource> sourcesOfValue;
	
	public WhyDidntArgumentHaveValue(Asker asker, int eventID, int argument, long[] objectIDs, Set<ValueSource> sourcesOfValue, String descriptionOfEvent) {

		super(asker, new EventID(eventID), descriptionOfEvent);

		this.argument = argument;
		this.sourcesOfValue = sourcesOfValue;
		this.objectIDs = objectIDs;

		if(objectIDs == null) throw new NullPointerException("Must provide a list of expected objectIDs.");
		
	}
	
	protected Answer answer() throws AnalysisException {

		// This question is used to answer questions about why various values of fields or arguments to
		// graphical output instructions were not reached.

		return new MessageAnswer(this, "Haven't implemented yet.");
		
//		for(ValueSource source : sourcesOfValue)
//			addAnswer(
//					WhyNotValueAnalyzer.compare(
//							this, 
//							source, 
//							getTrace(), 
//							new ExpectedObject(new TLongHashSet(objectIDs)), 
//							getTrace().getOperandStackValue(subject.getEventID(), argument).getEventID()));
		
	}

	public String getQuestionExplanation() {
		
		return "explain why <b>" + getDescriptionOfSubject() + "</b> wasn't used";
		
	}

}