package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.trace.EventKind;

public final class InvocationBlock extends ExplanationBlock {

	private final boolean invocationWasInstrumented;

	public InvocationBlock(Answer answer, int eventID) {
		
		super(answer, eventID);

		int nextEventID = answer.getTrace().getNextEventIDInThread(eventID);
		invocationWasInstrumented = answer.getTrace().getKind(nextEventID) == EventKind.START_METHOD;
		
	}
	
	public boolean invocationWasInstrumented() { return invocationWasInstrumented; }
	
}
