package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.bytecode.Instruction;

public final class CauseAnswer extends Answer {

	private final int eventID;
	private final String answerText;
	
	public CauseAnswer(Question<?> question, int eventID, String answerText) {

		super(question);

		this.answerText = answerText;
		
		// If there's no source for the event in question, see if we can find an upstream dependency.
		Instruction inst = getTrace().getInstruction(eventID);
		if(inst.getClassfile().getSourceFile() == null) {
			
			int sourceID = getTrace().getSourceOfValueID(eventID); 
			if(sourceID >= 0)
				eventID = sourceID;

		}

		this.eventID = eventID;
		
		Explanation explanation = getExplanationFor(this.eventID);
		
		assert explanation != null : "Whaaa? Why couldn't we get an explanation for event " + this.eventID;
		
		explanation.explain();
		
	}

	public int getEventID() { return eventID; }
	
	public String getKind() { return "Certain events occurred..."; }

	public int getPriority() { return 3; } 

	public String getAnswerText() {
		
		return "<html>" + answerText;
		
	}	

}