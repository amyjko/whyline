package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.util.IntegerVector;

public final class ThisCodeDidExecuteAnswer extends Answer {

	private final IntegerVector events;
	
	public ThisCodeDidExecuteAnswer(Question<?> question, IntegerVector events) {

		super(question);

		this.events = events;
		
		for(int i = 0; i < events.size(); i++)
			getExplanationFor(events.get(i)).explain();
		
	}

	public String getAnswerText() {

		return "<html>" + question.getDescriptionOfSubject() + " <i> did</i> " + question.getDescriptionOfEvent() + (events.size() > 1 ? ", " + events.size() + " times, in fact." : ".");

	}

	public boolean containsEventID(int eventID) { return events.contains(eventID); }
	
	public int getPriority() { return 1; } 

	public String getKind() { return "It did execute..."; }

}
