package edu.cmu.hcii.whyline.qa;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.hcii.whyline.analysis.AnalysisException;
import edu.cmu.hcii.whyline.analysis.UnexecutedInstructionAnalyzer;
import edu.cmu.hcii.whyline.bytecode.Definition;
import edu.cmu.hcii.whyline.bytecode.FieldInfo;
import edu.cmu.hcii.whyline.trace.Value;
import edu.cmu.hcii.whyline.trace.nodes.ObjectState;

/**
 * 
 * @author Andrew J. Ko
 *
 */ 
public class WhyDidntFieldChange extends WhyDidntQuestion<FieldInfo> {

	private final long objectID;
	
	public WhyDidntFieldChange(Asker asker, ObjectState object, FieldInfo field, Value value, String event) {
	
		super(asker, field, event);
		
		this.objectID = object.getObjectID();
		
	}
	
	protected Answer answer() throws AnalysisException {

		if(scope.isEndOfProgram()) {
			return new MessageAnswer(this, "<i>Nothing</i> changed after the end of the program. Perhaps you forgot to select a specific time?");
		}
		
		asker.processing(true);
		
		List<Definition> assignments = new ArrayList<Definition>();
		for(Definition assignment : subject.getDefinitions()) {
			// We skip <init>s, because presumably, the given object is already instantiated.
			if(!assignment.getMethod().isInstanceInitializer())
				assignments.add(assignment);
		}
		
		Answer answer = UnexecutedInstructionAnalyzer.explain(this, assignments.toArray(new Definition[assignments.size()]), new ExpectedObject(objectID, 0));				
			
		asker.processing(false);
		
		return answer;
		
	}

	public String getQuestionExplanation() {

		return "explain why " + subject.getDisplayName(true, -1) + " didn't change after " + scope.getDescription();
	
	}

}
