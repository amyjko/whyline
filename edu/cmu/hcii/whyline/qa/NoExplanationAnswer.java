package edu.cmu.hcii.whyline.qa;


public final class NoExplanationAnswer extends Answer {
	
	public NoExplanationAnswer(Question<?> question, UnexecutedInstruction[] instruction) {

		super(question, instruction);
		
	}

	public int getPriority() { return 1; } 
	
	public String getAnswerText() {

		return "<html>Unable to find a reason.";

	}
	
	public String getKind() { return "Couldn't find a reason..."; }

}