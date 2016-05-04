package edu.cmu.hcii.whyline.qa;


public final class MessageAnswer extends Answer {

	private final String longMessage;

	public MessageAnswer(Question<?> question, String longMessage) {

		super(question);

		this.longMessage = longMessage;
		
	}

	public int getPriority() { return 1; } 
	
	public String getKind() { return "Missing code..."; }

	public String getAnswerText() { return longMessage; }

}