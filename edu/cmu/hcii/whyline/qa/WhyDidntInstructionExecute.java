package edu.cmu.hcii.whyline.qa;

import edu.cmu.hcii.whyline.analysis.UnexecutedInstructionAnalyzer;
import edu.cmu.hcii.whyline.bytecode.Instruction;

/**
 * Explains why a particular instruction was not reached and executed, optionally on a specific instance. 
 * 
 * @author Andrew J. Ko
 *
 */
public final class WhyDidntInstructionExecute extends WhyDidntQuestion<Instruction> {

	public WhyDidntInstructionExecute(Asker asker, Instruction instruction, String descriptionOfEvent) {

		super(asker, instruction, descriptionOfEvent);
		
		assert instruction != null : "Can't send a null instruction to " + this;

	}
	
	protected Answer answer() {

		// This question is used for asking about why a line didn't execute, or why some text output didn't get printed by a particular line.
		// All we know is that if its in an instance method, we know to expect a particular type for "this," whoever the caller is to the
		// method that contains this instruction.
		
		return UnexecutedInstructionAnalyzer.explain(this, new Instruction[] {subject}, null);
		
	}

	public String getQuestionExplanation() {
		
		return "explain why <b>" + getDescriptionOfSubject() + "</b> didn't execute";
		
	}
	
}