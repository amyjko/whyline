package edu.cmu.hcii.whyline.qa;


public final class ValueOverriddenAnswer extends Answer {

	private final int overriddenID;
	private final int overrideID;
	
	public ValueOverriddenAnswer(Question<?> question, int overriddenID, int overrideID) {

		super(question);

		this.overriddenID = overriddenID;
		this.overrideID = overrideID;
		
		getExplanationFor(overriddenID).explain();
		getExplanationFor(overrideID).explain();
		
	}

	public String getAnswerText() {

		return "<html>" + question.getDescriptionOfSubject() + " <i> did</i> " + question.getDescriptionOfEvent() + ", but it was overriden by a newer value.";

	}

	public int getPriority() { return 1; } 

	public String getKind() { return "It did execute..."; }

}
