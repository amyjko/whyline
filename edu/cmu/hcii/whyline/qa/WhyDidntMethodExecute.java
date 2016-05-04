package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.analysis.UnexecutedInstructionAnalyzer;
import edu.cmu.hcii.whyline.bytecode.Instruction;
import edu.cmu.hcii.whyline.bytecode.MethodInfo;

/**
 * Explains why a particular method was not executed, optionally on a specific instance. 
 * 
 * @author Andrew J. Ko
 *
 */
public final class WhyDidntMethodExecute extends WhyDidntQuestion<MethodInfo> {
	
	// If 0 (representing null), the instruction could have been executed on any instance. If non-zero, it must have been on the specific instance.
	private final long objectID;
	
	public WhyDidntMethodExecute(Asker asker, MethodInfo method, long objectID, String descriptionOfEvent) {

		super(asker, method, descriptionOfEvent);
		
		this.objectID = objectID;
		
		assert objectID > 0 : "Must provide legal objectID: " + objectID;
		assert method.isVirtual() : "This question requires that the given method is an instance method.";
		
	}
	
	public long getObjectIDOfInterest() { return objectID; }
	
	protected Answer answer() {

		if(scope.isEndOfProgram())
			return new MessageAnswer(this, "<i>Nothing</i> changed after the end of the program. Perhaps you forgot to select a specific time?");

		// If its a virtual method, we expect that, from the perspective of the first instruction of the method,
		// "this" is an instance of the class that contains the method. If the object of interest is non-null,
		// then we can be more specific about the type that we expect.
		return UnexecutedInstructionAnalyzer.explain(this, new Instruction[] { subject.getCode().getFirstInstruction() }, objectID == 0 ? null : new ExpectedObject(objectID, 0));
		
	}
	
	public String getQuestionExplanation() {
		
		return "explain why <b>" + getDescriptionOfSubject() + "</b> didn't execute";
		
	}

}