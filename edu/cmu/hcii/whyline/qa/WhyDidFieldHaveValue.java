package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.analysis.AnalysisException;
import edu.cmu.hcii.whyline.bytecode.*;
import edu.cmu.hcii.whyline.trace.EventKind;
import edu.cmu.hcii.whyline.trace.nodes.ObjectState;

public final class WhyDidFieldHaveValue extends WhyDidQuestion<FieldInfo> {

	private final ObjectState object;
	private final FieldInfo field;
	
	public WhyDidFieldHaveValue(Asker asker, ObjectState object, FieldInfo field, String event) {

		super(asker, field, event);

		this.object = object;
		this.field = field;
		
	}

	protected Answer answer() throws AnalysisException {

		// If we find an assignment, find a description of the value assigned.
		int assignmentID = trace.findFieldAssignmentBefore(field, object.getObjectID(), asker.getCurrentScope().getInputEventID());

		Answer answer;
		if(assignmentID < 0)
			answer = new MessageAnswer(this, "Couldn't find an assignment to this field. Either the Whyline didn't record the assignment, or the value was a default value assigned when the object was instantiated.");
		else {
			EventKind kind = trace.getKind(assignmentID);
			if(kind == EventKind.PUTFIELD)
				answer = trace.getDefinitionValueSet(assignmentID).getAnswer(this);
			else  if(kind == EventKind.NEW_OBJECT)
				answer = new DefaultValueAnswer(this, object, field, assignmentID);
			else
				answer = new MessageAnswer(this, "OOPS! Looked for an assignment to this field, but found a " + kind + "!"); 
		}

		return answer;
		
	}

	public String getQuestionExplanation() {
		
		return "explain why <b>" + getDescriptionOfSubject() + "</b> " + getDescriptionOfEvent();
		
	}

}
