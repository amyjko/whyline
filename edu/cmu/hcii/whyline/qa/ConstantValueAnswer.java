package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.trace.ConstantValue;

public class ConstantValueAnswer extends Answer {

	private final ConstantValue constant;
	
	public ConstantValueAnswer(Question<?> question, ConstantValue constantStackValue) {
	
		super(question);
		
		this.constant = constantStackValue;
		
		Explanation explanation = getExplanationFor(constant.getEventID());

	}

	public String getAnswerText() {

		return "It was a constant.";
		
	}

	public String getKind() { return "constant value";  }

	protected int getPriority() { return 0; }

}
