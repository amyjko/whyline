package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.bytecode.FieldInfo;
import edu.cmu.hcii.whyline.trace.nodes.ObjectState;

public class DefaultValueAnswer extends Answer {

	private final int initializationID;
	private ObjectState object;
	private FieldInfo field;
	
	public DefaultValueAnswer(Question<?> question,ObjectState object, FieldInfo field,  int initID) {
	
		super(question);
		
		this.object = object;
		this.field = field;
		this.initializationID = initID;
		
		getExplanationFor(initializationID);

	}

	public String getAnswerText() {

		return "When " + object.getDisplayName(true, -1) + " was instantiated, " + field.getDisplayName(true, -1) + " was assigned a default value of " + field.getDefaultValue();
		
	}

	public String getKind() { return "constant value";  }

	protected int getPriority() { return 0; }

}
